package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.config.OutboxEventProperties
import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.entities.OutboxStatus
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.StoreNotFoundException
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OutboxProcessorServiceTest {
    private val outboxEventRepository = mockk<OutboxEventRepository>()
    private val webhookService = mockk<WebhookService>()

    @Test
    fun `given no pending events, it should not call webhook or save`() {
        val properties = properties()
        val service = OutboxProcessorService(outboxEventRepository, webhookService, properties)
        every { outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize) } returns
            emptyList()

        service.processOutboxEvents()

        verify(exactly = 1) {
            outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize)
        }
        verify(exactly = 0) { outboxEventRepository.save(any()) }
        verify(exactly = 0) { webhookService.findRelevantWebhooksAndSend(any()) }
    }

    @Test
    fun `given a pending event, it should mark processing then sent`() {
        val properties = properties()
        val service = OutboxProcessorService(outboxEventRepository, webhookService, properties)
        val event = outboxEvent()

        val savedEvents = mutableListOf<OutboxEvent>()
        every { outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize) } returns
            listOf(event)
        every { outboxEventRepository.save(capture(savedEvents)) } answers { firstArg() }
        every { webhookService.findRelevantWebhooksAndSend(event) } returns Unit

        service.processOutboxEvents()

        verify(exactly = 2) { outboxEventRepository.save(any()) }
        verify(exactly = 1) { webhookService.findRelevantWebhooksAndSend(event) }
        assertEquals(OutboxStatus.PROCESSING, savedEvents[0].status)
        assertEquals(OutboxStatus.SENT, savedEvents[1].status)
        assertNotNull(savedEvents[1].processedAt)
    }

    @Test
    fun `given store not found, it should mark webhook not registered`() {
        val properties = properties()
        val service = OutboxProcessorService(outboxEventRepository, webhookService, properties)
        val event = outboxEvent()

        val savedEvents = mutableListOf<OutboxEvent>()
        every { outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize) } returns
            listOf(event)
        every { outboxEventRepository.save(capture(savedEvents)) } answers { firstArg() }
        every { webhookService.findRelevantWebhooksAndSend(event) } throws StoreNotFoundException(
            ErrorType.STORE_NOT_FOUND.name,
            "store missing"
        )

        service.processOutboxEvents()

        verify(exactly = 2) { outboxEventRepository.save(any()) }
        assertEquals(OutboxStatus.PROCESSING, savedEvents[0].status)
        assertEquals(OutboxStatus.WEBHOOK_NOT_REGISTERED, savedEvents[1].status)
        assertNotNull(savedEvents[1].processedAt)
    }

    @Test
    fun `given webhook failure, it should schedule retry when below max retries`() {
        val properties = properties(
            maxRetries = 3,
            baseDelaySeconds = 0,
            maxDelaySeconds = 0,
            maxJitterSeconds = 0
        )
        val service = OutboxProcessorService(outboxEventRepository, webhookService, properties)
        val event = outboxEvent(retryCount = 1)

        val savedEvents = mutableListOf<OutboxEvent>()
        every { outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize) } returns
            listOf(event)
        every { outboxEventRepository.save(capture(savedEvents)) } answers { firstArg() }
        every { webhookService.findRelevantWebhooksAndSend(event) } throws RuntimeException("boom")

        val before = LocalDateTime.now()
        service.processOutboxEvents()
        val after = LocalDateTime.now()

        verify(exactly = 2) { outboxEventRepository.save(any()) }
        val updated = savedEvents.last()
        assertEquals(OutboxStatus.PENDING, updated.status)
        assertEquals(2, updated.retryCount)
        assertNotNull(updated.lastError)
        assertNotNull(updated.nextRetryAt)
        assertTrue(!(updated.nextRetryAt?.isBefore(before) ?: false))
        assertTrue(!(updated.nextRetryAt?.isAfter(after) ?: false))
    }

    @Test
    fun `given webhook failure, it should mark failed when max retries reached`() {
        val properties = properties(maxRetries = 2)
        val service = OutboxProcessorService(outboxEventRepository, webhookService, properties)
        val event = outboxEvent(retryCount = 2)

        val savedEvents = mutableListOf<OutboxEvent>()
        every { outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize) } returns
            listOf(event)
        every { outboxEventRepository.save(capture(savedEvents)) } answers { firstArg() }
        every { webhookService.findRelevantWebhooksAndSend(event) } throws RuntimeException("boom")

        service.processOutboxEvents()

        verify(exactly = 2) { outboxEventRepository.save(any()) }
        val updated = savedEvents.last()
        assertEquals(OutboxStatus.FAILED, updated.status)
        assertEquals(3, updated.retryCount)
        assertNotNull(updated.lastError)
        assertNotNull(updated.processedAt)
    }

    @Test
    fun `given repository failure while marking processing, it should still update failure`() {
        val properties = properties(
            baseDelaySeconds = 0,
            maxDelaySeconds = 0,
            maxJitterSeconds = 0
        )
        val service = OutboxProcessorService(outboxEventRepository, webhookService, properties)
        val event = outboxEvent(retryCount = 0)

        var calls = 0
        val savedEvents = mutableListOf<OutboxEvent>()
        every { outboxEventRepository.findPendingEventsForUpdate(properties.maxRetries, properties.batchSize) } returns
            listOf(event)
        every { outboxEventRepository.save(any()) } answers {
            calls += 1
            if (calls == 1) error("db down")
            savedEvents.add(firstArg())
            firstArg()
        }

        val before = LocalDateTime.now()
        service.processOutboxEvents()
        val after = LocalDateTime.now()

        verify(exactly = 2) { outboxEventRepository.save(any()) }
        verify(exactly = 0) { webhookService.findRelevantWebhooksAndSend(any()) }

        val updated = savedEvents.single()
        assertEquals(OutboxStatus.PENDING, updated.status)
        assertEquals(1, updated.retryCount)
        assertNotNull(updated.lastError)
        assertNotNull(updated.nextRetryAt)
        assertTrue(!(updated.nextRetryAt?.isBefore(before) ?: false))
        assertTrue(!(updated.nextRetryAt?.isAfter(after) ?: false))
    }

    private fun outboxEvent(retryCount: Int = 0): OutboxEvent = OutboxEvent(
        id = UUID.randomUUID(),
        aggregateId = "store-1",
        aggregateType = AggregateType.ORDER,
        eventType = "order.created",
        payload = """{"id":"1"}""",
        retryCount = retryCount
    )

    private fun properties(
        maxRetries: Int = 3,
        batchSize: Int = 10,
        baseDelaySeconds: Int = 1,
        maxDelaySeconds: Int = 60,
        maxJitterSeconds: Int = 1
    ): OutboxEventProperties = OutboxEventProperties(
        maxRetries = maxRetries,
        batchSize = batchSize,
        baseDelaySeconds = baseDelaySeconds,
        maxDelaySeconds = maxDelaySeconds,
        maxJitterSeconds = maxJitterSeconds
    )
}
