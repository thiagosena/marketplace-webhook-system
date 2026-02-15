package com.thiagosena.receiver.domain.services

import com.thiagosena.receiver.domain.entities.Event
import com.thiagosena.receiver.domain.entities.dto.EventWebhookDto
import com.thiagosena.receiver.domain.repositories.EventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventServiceTest {
    private val eventRepository = mockk<EventRepository>()
    private val service = EventService(eventRepository)

    @Test
    fun `given a valid event, it should save the event when idempotencyKey does not exist`() {
        val eventDto =
            EventWebhookDto(
                idempotencyKey = "key-123",
                eventType = "order.created",
                orderId = UUID.randomUUID().toString(),
                storeId = "store-456"
            )

        every { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) } returns false

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processEvent(eventDto)

        verify(exactly = 1) { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) }
        verify(exactly = 1) { eventRepository.save(any()) }

        val savedEvent = eventSlot.captured
        assertEquals(eventDto.idempotencyKey, savedEvent.idempotencyKey)
        assertEquals(eventDto.eventType, savedEvent.eventType)
        assertEquals(eventDto.orderId, savedEvent.orderId)
        assertEquals(eventDto.storeId, savedEvent.storeId)
    }

    @Test
    fun `given a duplicate event, it should not save when idempotencyKey already exists`() {
        val eventDto =
            EventWebhookDto(
                idempotencyKey = "key-duplicate",
                eventType = "order.paid",
                orderId = UUID.randomUUID().toString(),
                storeId = "store-789"
            )

        every { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) } returns true

        service.processEvent(eventDto)

        verify(exactly = 1) { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) }
        verify(exactly = 0) { eventRepository.save(any()) }
    }

    @Test
    fun `given multiple different events, it should save all events with unique idempotencyKeys`() {
        val eventDto1 =
            EventWebhookDto(
                idempotencyKey = "key-001",
                eventType = "order.created",
                orderId = UUID.randomUUID().toString(),
                storeId = "store-111"
            )
        val eventDto2 =
            EventWebhookDto(
                idempotencyKey = "key-002",
                eventType = "order.shipped",
                orderId = UUID.randomUUID().toString(),
                storeId = "store-222"
            )

        every { eventRepository.existsByIdempotencyKey("key-001") } returns false
        every { eventRepository.existsByIdempotencyKey("key-002") } returns false
        every { eventRepository.save(any()) } answers { firstArg() }

        service.processEvent(eventDto1)
        service.processEvent(eventDto2)

        verify(exactly = 1) { eventRepository.existsByIdempotencyKey("key-001") }
        verify(exactly = 1) { eventRepository.existsByIdempotencyKey("key-002") }
        verify(exactly = 2) { eventRepository.save(any()) }
    }

    @Test
    fun `given an event with same idempotencyKey sent twice, it should save only once`() {
        val eventDto =
            EventWebhookDto(
                idempotencyKey = "key-repeated",
                eventType = "order.cancelled",
                orderId = UUID.randomUUID().toString(),
                storeId = "store-333"
            )

        every { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) } returns false andThen true
        every { eventRepository.save(any()) } answers { firstArg() }

        service.processEvent(eventDto)
        service.processEvent(eventDto)

        verify(exactly = 2) { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) }
        verify(exactly = 1) { eventRepository.save(any()) }
    }

    @Test
    fun `given an event with all fields populated, it should save event with correct data`() {
        val orderId = UUID.randomUUID().toString()
        val eventDto =
            EventWebhookDto(
                idempotencyKey = "key-complete",
                eventType = "order.delivered",
                orderId = orderId,
                storeId = "store-complete"
            )

        every { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) } returns false

        val eventSlot = slot<Event>()
        every { eventRepository.save(capture(eventSlot)) } answers { firstArg() }

        service.processEvent(eventDto)

        val savedEvent = eventSlot.captured
        assertEquals("key-complete", savedEvent.idempotencyKey)
        assertEquals("order.delivered", savedEvent.eventType)
        assertEquals(orderId, savedEvent.orderId)
        assertEquals("store-complete", savedEvent.storeId)

        verify(exactly = 1) { eventRepository.existsByIdempotencyKey(eventDto.idempotencyKey) }
        verify(exactly = 1) { eventRepository.save(any()) }
    }
}
