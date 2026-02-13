package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.config.OutboxEventProperties
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.entities.OutboxStatus
import com.thiagosena.marketplace.domain.exceptions.StoreNotFoundException
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
            try {
                webhookService.findRelevantWebhooksAndSend(event)
            } catch (ex: StoreNotFoundException) {
                log.error(ex) { "Failed to send outbox event $event" }
                outboxEventRepository.save(
                    event.copy(
                        status = OutboxStatus.WEBHOOK_NOT_REGISTERED,
                        processedAt = LocalDateTime.now()
                    )
                )
                return
            }
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
                lastError = ex.stackTraceToString()
            )
        } else {
            event.copy(
                retryCount = event.retryCount + 1,
                status = OutboxStatus.PENDING,
                nextRetryAt = calculateNextRetry(event.retryCount),
                lastError = ex.stackTraceToString()
            )
        }

        outboxEventRepository.save(eventUpdated)
    }

    private fun calculateNextRetry(retryCount: Int): LocalDateTime {
        val exponential = outboxEventProperties.baseDelaySeconds * 2.0.pow(retryCount).toLong()
        val jitter = (0..outboxEventProperties.maxJitterSeconds).random()
        val delaySeconds = (exponential + jitter)
            .coerceAtMost(outboxEventProperties.maxDelaySeconds.toLong())
        return LocalDateTime.now().plusSeconds(delaySeconds)
    }
}
