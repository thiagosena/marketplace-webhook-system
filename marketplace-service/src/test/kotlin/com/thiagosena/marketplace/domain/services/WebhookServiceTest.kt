package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.entities.Webhook
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.StoreNotFoundException
import com.thiagosena.marketplace.domain.gateways.WebhookHttpGateway
import com.thiagosena.marketplace.domain.repositories.WebhookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class WebhookServiceTest {
    private val webhookRepository = mockk<WebhookRepository>()
    private val webhookHttpGateway = mockk<WebhookHttpGateway>()
    private val service = WebhookService(webhookRepository, webhookHttpGateway)

    @Test
    fun `given a valid webhook, it should save and return response`() {
        val webhook = webhook()
        val savedWebhook = webhook.copy(id = UUID.randomUUID())

        every { webhookRepository.save(webhook) } returns savedWebhook

        val response = service.createWebhook(webhook)

        verify(exactly = 1) { webhookRepository.save(webhook) }
        assertEquals(savedWebhook.id, response.id)
        assertEquals(savedWebhook.storeIds, response.storeIds)
        assertEquals(savedWebhook.callbackUrl, response.callbackUrl)
        assertEquals(savedWebhook.active, response.active)
        assertEquals(savedWebhook.createdAt, response.createdAt)
    }

    @Test
    fun `given a saved webhook without id, it should throw`() {
        val webhook = webhook()
        val savedWebhook = webhook.copy(id = null)

        every { webhookRepository.save(webhook) } returns savedWebhook

        assertThrows(IllegalStateException::class.java) {
            service.createWebhook(webhook)
        }

        verify(exactly = 1) { webhookRepository.save(webhook) }
    }

    @Test
    fun `given active webhooks, it should send to all callback urls`() {
        val event = outboxEvent()
        val webhookA = webhook(callbackUrl = "https://example.com/a")
        val webhookB = webhook(callbackUrl = "https://example.com/b")

        every { webhookRepository.findActiveByStoreId(event.aggregateId) } returns listOf(webhookA, webhookB)
        every { webhookHttpGateway.send(any(), any(), any()) } returns Unit

        service.findRelevantWebhooksAndSend(event)

        verify(exactly = 1) { webhookRepository.findActiveByStoreId(event.aggregateId) }
        verify(exactly = 1) { webhookHttpGateway.send(webhookA.callbackUrl, event.payload, webhookA.token) }
        verify(exactly = 1) { webhookHttpGateway.send(webhookB.callbackUrl, event.payload, webhookB.token) }
    }

    @Test
    fun `given no active webhooks, it should throw store not found`() {
        val event = outboxEvent()

        every { webhookRepository.findActiveByStoreId(event.aggregateId) } returns emptyList()

        val exception =
            assertThrows(StoreNotFoundException::class.java) {
                service.findRelevantWebhooksAndSend(event)
            }

        verify(exactly = 1) { webhookRepository.findActiveByStoreId(event.aggregateId) }
        verify(exactly = 0) { webhookHttpGateway.send(any(), any(), any()) }
        assertEquals(ErrorType.STORE_NOT_FOUND.name, exception.type)
    }

    @Test
    fun `given gateway failure, it should propagate exception`() {
        val event = outboxEvent()
        val webhookA = webhook(callbackUrl = "https://example.com/a")
        val webhookB = webhook(callbackUrl = "https://example.com/b")

        every { webhookRepository.findActiveByStoreId(event.aggregateId) } returns listOf(webhookA, webhookB)
        every {
            webhookHttpGateway.send(
                webhookA.callbackUrl,
                event.payload,
                webhookA.token
            )
        } throws RuntimeException("boom")

        assertThrows(RuntimeException::class.java) {
            service.findRelevantWebhooksAndSend(event)
        }

        verify(exactly = 1) { webhookHttpGateway.send(webhookA.callbackUrl, event.payload, webhookA.token) }
        verify(exactly = 0) { webhookHttpGateway.send(webhookB.callbackUrl, event.payload, webhookB.token) }
    }

    private fun webhook(callbackUrl: String = "https://example.com/webhook") = Webhook(
        storeIds = listOf("store-1"),
        callbackUrl = callbackUrl,
        token = "test-token-123",
        active = true
    )

    private fun outboxEvent(): OutboxEvent = OutboxEvent(
        id = UUID.randomUUID(),
        aggregateId = "store-1",
        aggregateType = AggregateType.ORDER,
        eventType = "order.created",
        payload = """{"id":"1"}"""
    )
}
