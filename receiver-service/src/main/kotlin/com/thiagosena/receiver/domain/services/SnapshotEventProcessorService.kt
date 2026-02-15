package com.thiagosena.receiver.domain.services

import com.thiagosena.receiver.domain.config.SnapshotEventProperties
import com.thiagosena.receiver.domain.entities.Event
import com.thiagosena.receiver.domain.entities.EventStatus
import com.thiagosena.receiver.domain.gateways.MarketplaceGateway
import com.thiagosena.receiver.domain.repositories.EventRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import java.time.LocalDateTime
import kotlin.math.pow
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class SnapshotEventProcessorService(
    private val marketplaceGateway: MarketplaceGateway,
    private val eventRepository: EventRepository,
    private val snapshotEventProperties: SnapshotEventProperties,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Transactional
    fun processSnapshotEvents() {
        eventRepository.findSnapshotPendingEventsForUpdate(
            maxRetries = snapshotEventProperties.maxRetries,
            batchSize = snapshotEventProperties.batchSize
        ).let { events ->
            log.info { "Processing ${events.size} snapshot events" }
            events.forEach { event ->
                processEvent(event)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processEvent(event: Event) {
        try {
            marketplaceGateway.findOrderById(event.orderId).let { order ->
                eventRepository.save(
                    event.copy(
                        snapshot = objectMapper.writeValueAsString(order),
                        status = EventStatus.SNAPSHOT_PROCESSED,
                        processedAt = LocalDateTime.now()
                    )
                ).also {
                    log.info {
                        "Snapshot event ${event.id} and idempotencyKey: ${event.idempotencyKey} processed successfully"
                    }
                }
            }
        } catch (ex: Exception) {
            handleFailure(event, ex)
        }
    }

    private fun handleFailure(event: Event, ex: Exception) {
        log.error(ex) { "Failed to process snapshot event ${event.id}" }

        val eventUpdated = if (event.retryCount >= snapshotEventProperties.maxRetries) {
            log.error { "Snapshot event ${event.id} failed permanently, max retries reached = ${event.retryCount}" }
            event.copy(
                retryCount = event.retryCount + 1,
                status = EventStatus.SNAPSHOT_FAILED,
                processedAt = LocalDateTime.now(),
                lastError = ex.stackTraceToString()
            )
        } else {
            event.copy(
                retryCount = event.retryCount + 1,
                status = EventStatus.SNAPSHOT_PENDING,
                nextRetryAt = calculateNextRetry(event.retryCount),
                lastError = ex.stackTraceToString()
            )
        }

        eventRepository.save(eventUpdated)
    }

    private fun calculateNextRetry(retryCount: Int): LocalDateTime {
        val exponential = snapshotEventProperties.baseDelaySeconds * 2.0.pow(retryCount).toLong()
        val jitter = (0..snapshotEventProperties.maxJitterSeconds).random()
        val delaySeconds = (exponential + jitter)
            .coerceAtMost(snapshotEventProperties.maxDelaySeconds.toLong())
        return LocalDateTime.now().plusSeconds(delaySeconds)
    }
}
