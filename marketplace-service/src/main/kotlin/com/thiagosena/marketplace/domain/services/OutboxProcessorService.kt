package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.config.OutboxEventProperties
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.entities.OutboxStatus
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import kotlin.math.pow
import org.springframework.stereotype.Service

@Service
class OutboxProcessorService(
    private val outboxEventRepository: OutboxEventRepository,
    private val webhookService: WebhookService,
    private val outboxEventProperties: OutboxEventProperties
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Transactional
    fun processOutboxEvents() {
        outboxEventRepository.findPendingEventsForUpdate(
            maxRetries = outboxEventProperties.maxRetries,
            batchSize = outboxEventProperties.batchSize
        ).let { events ->
            log.info { "Processing ${events.size} outbox events" }
            events.forEach { event ->
                processEvent(event)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processEvent(event: OutboxEvent) {
        try {
            outboxEventRepository.save(event.copy(status = OutboxStatus.PROCESSING))
            webhookService.findRelevantWebhooksAndSend(event)
            outboxEventRepository.save(
                event.copy(
                    status = OutboxStatus.SENT,
                    processedAt = LocalDateTime.now()
                )
            )
        } catch (ex: Exception) {
            handleFailure(event, ex)
        }
    }

    private fun handleFailure(event: OutboxEvent, ex: Exception) {
        log.error(ex) { "Failed to process outbox event ${event.id}" }

        val eventUpdated = if (event.retryCount >= outboxEventProperties.maxRetries) {
            log.error { "Outbox event ${event.id} failed permanently, max retries reached = ${event.retryCount}" }
            event.copy(
                retryCount = event.retryCount + 1,
                status = OutboxStatus.FAILED,
                processedAt = LocalDateTime.now(),
                lastError = ex.message
            )
        } else {
            event.copy(
                retryCount = event.retryCount + 1,
                status = OutboxStatus.FAILED,
                nextRetryAt = calculateNextRetry(event.retryCount),
                lastError = ex.message
            )
        }

        outboxEventRepository.save(eventUpdated)
    }

    private fun calculateNextRetry(retryCount: Int): LocalDateTime {
        val delaySeconds = 2.0.pow(retryCount).toLong()
        return LocalDateTime.now().plusSeconds(delaySeconds)
    }
}
