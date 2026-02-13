package com.thiagosena.marketplace.application.scheduler

import com.thiagosena.marketplace.domain.services.OutboxProcessorService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OutboxProcessor(private val outboxProcessorService: OutboxProcessorService) {

    @Scheduled(fixedDelayString = "\${app.outbox-event.scheduler.fixed-delay:5000}")
    fun processOutboxEvents() {
        outboxProcessorService.processOutboxEvents()
    }
}
