package com.thiagosena.receiver.domain.services

import com.thiagosena.receiver.domain.config.SnapshotEventProperties
import com.thiagosena.receiver.domain.entities.Event
import com.thiagosena.receiver.domain.entities.EventStatus
import com.thiagosena.receiver.domain.gateways.MarketplaceGateway
import com.thiagosena.receiver.domain.gateways.responses.OrderStatus
import com.thiagosena.receiver.domain.repositories.EventRepository
import com.thiagosena.receiver.factory.EventFactory
import com.thiagosena.receiver.factory.OrderFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class SnapshotEventProcessorServiceTest {
    private val marketplaceGateway = mockk<MarketplaceGateway>()
    private val eventRepository = mockk<EventRepository>()
    private val snapshotEventProperties = mockk<SnapshotEventProperties>()
    private val objectMapper = mockk<ObjectMapper>()
    private val service =
        SnapshotEventProcessorService(
            marketplaceGateway,
            eventRepository,
            snapshotEventProperties,
            objectMapper
        )

    @Test
    fun `given pending events, it should process all events successfully and update status to SNAPSHOT_PROCESSED`() {
        val orderId1 = UUID.randomUUID().toString()
        val orderId2 = UUID.randomUUID().toString()
        val event1 = EventFactory.sampleEvent(orderId = orderId1)
        val event2 = EventFactory.sampleEvent(orderId = orderId2, eventType = "order.paid")

        val order1 = OrderFactory.sampleOrderResponse(orderId1)
        val order2 = OrderFactory.sampleOrderResponse(orderId2, status = OrderStatus.PAID)

        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns listOf(event1, event2)
        every { marketplaceGateway.findOrderById(orderId1) } returns order1
        every { marketplaceGateway.findOrderById(orderId2) } returns order2
        every { objectMapper.writeValueAsString(order1) } returns """{"id":"$orderId1"}"""
        every { objectMapper.writeValueAsString(order2) } returns """{"id":"$orderId2"}"""

        val eventSlot = mutableListOf<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        verify(exactly = 1) {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        }
        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId1) }
        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId2) }
        verify(exactly = 2) { eventRepository.save(any()) }

        assertEquals(2, eventSlot.size)
        assertEquals(EventStatus.SNAPSHOT_PROCESSED, eventSlot[0].status)
        assertEquals(EventStatus.SNAPSHOT_PROCESSED, eventSlot[1].status)
        assertNotNull(eventSlot[0].snapshot)
        assertNotNull(eventSlot[1].snapshot)
        assertNotNull(eventSlot[0].processedAt)
        assertNotNull(eventSlot[1].processedAt)
    }

    @Test
    fun `given no pending events, it should not process anything`() {
        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns emptyList()

        service.processSnapshotEvents()

        verify(exactly = 1) {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        }
        verify(exactly = 0) { marketplaceGateway.findOrderById(any()) }
        verify(exactly = 0) { eventRepository.save(any()) }
    }

    @Test
    fun `given a gateway failure on first retry, it should increment retryCount and set status to SNAPSHOT_PENDING`() {
        val orderId = UUID.randomUUID().toString()
        val event = EventFactory.sampleEvent(orderId = orderId)

        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every { snapshotEventProperties.baseDelaySeconds } returns 5
        every { snapshotEventProperties.maxJitterSeconds } returns 2
        every { snapshotEventProperties.maxDelaySeconds } returns 300
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns listOf(event)
        every { marketplaceGateway.findOrderById(orderId) } throws RuntimeException("Gateway error")

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        verify(exactly = 1) {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        }
        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId) }
        verify(exactly = 1) { eventRepository.save(any()) }

        val savedEvent = eventSlot.captured
        assertEquals(1, savedEvent.retryCount)
        assertEquals(EventStatus.SNAPSHOT_PENDING, savedEvent.status)
        assertNotNull(savedEvent.nextRetryAt)
        assertNotNull(savedEvent.lastError)
        assertTrue(savedEvent.lastError!!.contains("Gateway error"))
    }

    @Test
    fun `given a gateway failure exceeding max retries, it should set status to SNAPSHOT_FAILED`() {
        val orderId = UUID.randomUUID().toString()
        val event = EventFactory.sampleEvent(eventType = "order.paid", orderId = orderId, retryCount = 3)

        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns listOf(event)
        every { marketplaceGateway.findOrderById(orderId) } throws RuntimeException("Permanent failure")

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        verify(exactly = 1) {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        }
        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId) }
        verify(exactly = 1) { eventRepository.save(any()) }

        val savedEvent = eventSlot.captured
        assertEquals(4, savedEvent.retryCount)
        assertEquals(EventStatus.SNAPSHOT_FAILED, savedEvent.status)
        assertNotNull(savedEvent.processedAt)
        assertNotNull(savedEvent.lastError)
        assertTrue(savedEvent.lastError!!.contains("Permanent failure"))
    }

    @Test
    fun `given mixed success and failure events, it should process successes and handle failures independently`() {
        val orderId1 = UUID.randomUUID().toString()
        val orderId2 = UUID.randomUUID().toString()
        val eventSuccess = EventFactory.sampleEvent(orderId = orderId1)
        val eventFail = EventFactory.sampleEvent(orderId = orderId2, eventType = "order.paid")

        val orderResponse = OrderFactory.sampleOrderResponse(orderId = orderId1, status = OrderStatus.DELIVERED)

        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every { snapshotEventProperties.baseDelaySeconds } returns 5
        every { snapshotEventProperties.maxJitterSeconds } returns 2
        every { snapshotEventProperties.maxDelaySeconds } returns 300
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns listOf(eventSuccess, eventFail)
        every { marketplaceGateway.findOrderById(orderId1) } returns orderResponse
        every { marketplaceGateway.findOrderById(orderId2) } throws RuntimeException("Gateway error")
        every { objectMapper.writeValueAsString(orderResponse) } returns """{"id":"$orderId1"}"""

        val eventSlot = mutableListOf<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        verify(exactly = 1) {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        }
        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId1) }
        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId2) }
        verify(exactly = 2) { eventRepository.save(any()) }

        assertEquals(2, eventSlot.size)
        assertEquals(EventStatus.SNAPSHOT_PROCESSED, eventSlot[0].status)
        assertEquals(EventStatus.SNAPSHOT_PENDING, eventSlot[1].status)
        assertEquals(1, eventSlot[1].retryCount)
    }

    @Test
    fun `given an event with snapshot already populated, it should update with new snapshot data`() {
        val orderId = UUID.randomUUID().toString()
        val event = EventFactory.sampleEvent(
            orderId = orderId,
            eventType = "order.shipped",
            status = EventStatus.SNAPSHOT_PENDING,
            snapshot = """{"old":"data"}"""
        )

        val orderResponse = OrderFactory.sampleOrderResponse(orderId, status = OrderStatus.DELIVERED)

        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns listOf(event)
        every { marketplaceGateway.findOrderById(orderId) } returns orderResponse
        every { objectMapper.writeValueAsString(orderResponse) } returns """{"id":"$orderId","status":"SHIPPED"}"""

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId) }
        verify(exactly = 1) { eventRepository.save(any()) }

        val savedEvent = eventSlot.captured
        assertEquals(EventStatus.SNAPSHOT_PROCESSED, savedEvent.status)
        assertEquals("""{"id":"$orderId","status":"SHIPPED"}""", savedEvent.snapshot)
        assertNotNull(savedEvent.processedAt)
    }

    @Test
    fun `given retry with exponential backoff, it should calculate nextRetryAt with jitter`() {
        val orderId = UUID.randomUUID().toString()
        val event = EventFactory.sampleEvent(orderId = orderId, retryCount = 2)

        every { snapshotEventProperties.maxRetries } returns 5
        every { snapshotEventProperties.batchSize } returns 10
        every { snapshotEventProperties.baseDelaySeconds } returns 10
        every { snapshotEventProperties.maxJitterSeconds } returns 5
        every { snapshotEventProperties.maxDelaySeconds } returns 1000
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 5,
                batchSize = 10
            )
        } returns listOf(event)
        every { marketplaceGateway.findOrderById(orderId) } throws RuntimeException("Retry error")

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        val savedEvent = eventSlot.captured
        assertEquals(3, savedEvent.retryCount)
        assertEquals(EventStatus.SNAPSHOT_PENDING, savedEvent.status)
        assertNotNull(savedEvent.nextRetryAt)
        assertTrue(savedEvent.nextRetryAt!!.isAfter(LocalDateTime.now()))
    }

    @Test
    fun `given serialization error, it should handle failure and save error details`() {
        val orderId = UUID.randomUUID().toString()
        val event = EventFactory.sampleEvent(
            orderId = orderId,
            eventType = "order.delivered",
            status = EventStatus.SNAPSHOT_PENDING
        )

        val orderResponse = OrderFactory.sampleOrderResponse(orderId, status = OrderStatus.DELIVERED)

        every { snapshotEventProperties.maxRetries } returns 3
        every { snapshotEventProperties.batchSize } returns 10
        every { snapshotEventProperties.baseDelaySeconds } returns 5
        every { snapshotEventProperties.maxJitterSeconds } returns 2
        every { snapshotEventProperties.maxDelaySeconds } returns 300
        every {
            eventRepository.findSnapshotPendingEventsForUpdate(
                maxRetries = 3,
                batchSize = 10
            )
        } returns listOf(event)
        every { marketplaceGateway.findOrderById(orderId) } returns orderResponse
        every { objectMapper.writeValueAsString(any()) } throws RuntimeException("Serialization failed")

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processSnapshotEvents()

        verify(exactly = 1) { marketplaceGateway.findOrderById(orderId) }
        verify(exactly = 1) { objectMapper.writeValueAsString(any()) }
        verify(exactly = 1) { eventRepository.save(any()) }

        val savedEvent = eventSlot.captured
        assertEquals(1, savedEvent.retryCount)
        assertEquals(EventStatus.SNAPSHOT_PENDING, savedEvent.status)
        assertNotNull(savedEvent.lastError)
        assertTrue(savedEvent.lastError!!.contains("Serialization failed"))
    }
}
