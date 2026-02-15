package com.thiagosena.receiver.domain.services

import com.thiagosena.receiver.domain.entities.Event
import com.thiagosena.receiver.domain.entities.dto.EventWebhookDto
import com.thiagosena.receiver.domain.repositories.EventRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class EventService(private val eventRepository: EventRepository) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Transactional
    fun processEvent(event: EventWebhookDto) {
        log.info {
            "Event received: ${event.eventType}, orderId: ${event.orderId}, idempotencyKey: ${event.idempotencyKey}"
        }

        eventRepository.existsByIdempotencyKey(event.idempotencyKey).takeIf { it }?.let {
            log.warn { "Event already processed with idempotencyKey: ${event.idempotencyKey}" }
            return
        }

        eventRepository.save(
            Event(
                idempotencyKey = event.idempotencyKey,
                eventType = event.eventType,
                orderId = event.orderId,
                storeId = event.storeId
            )
        )
    }
}
