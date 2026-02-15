package com.thiagosena.receiver.application.scheduler

import com.thiagosena.receiver.domain.services.SnapshotEventProcessorService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class EventSnapshotProcessor(private val snapshotEventProcessorService: SnapshotEventProcessorService) {

    @Scheduled(fixedDelayString = "\${app.snapshot-event.scheduler.fixed-delay:5000}")
    fun processOutboxEvents() {
        snapshotEventProcessorService.processSnapshotEvents()
    }
}
