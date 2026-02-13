package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.entities.Webhook
import com.thiagosena.marketplace.domain.gateways.WebhookHttpGateway
import com.thiagosena.marketplace.domain.repositories.WebhookRepository
import com.thiagosena.marketplace.domain.responses.WebhookResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class WebhookService(
    private val webhookRepository: WebhookRepository,
    private val webhookHttpGateway: WebhookHttpGateway
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Transactional
    fun createWebhook(webhook: Webhook): WebhookResponse {
        log.info { "Registering webhook for stores: ${webhook.storeIds}" }

        return webhookRepository.save(webhook).also {
            log.info { "Webhook registered successfully: ${it.id}" }
        }.toResponse()
    }

    fun findRelevantWebhooksAndSend(event: OutboxEvent) {
        webhookRepository.findActiveByStoreId(event.aggregateId).forEach { webhook ->
            webhookHttpGateway.send(webhook.callbackUrl, event.payload).also {
                log.info { "Webhook sent successfully: ${webhook.callbackUrl}" }
            }
        }
    }
}
