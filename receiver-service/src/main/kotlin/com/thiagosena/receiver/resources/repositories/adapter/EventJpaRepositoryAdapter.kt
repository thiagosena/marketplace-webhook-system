package com.thiagosena.receiver.resources.repositories.adapter

import com.thiagosena.receiver.domain.entities.Event
import com.thiagosena.receiver.domain.repositories.EventRepository
import com.thiagosena.receiver.resources.repositories.jpa.EventJpaRepository
import org.springframework.stereotype.Repository

@Repository
class EventJpaRepositoryAdapter(private val eventJpaRepository: EventJpaRepository) : EventRepository {
    override fun save(event: Event): Event = eventJpaRepository.save(event)

    override fun existsByIdempotencyKey(idempotencyKey: String) =
        eventJpaRepository.existsByIdempotencyKey(idempotencyKey)

    override fun findSnapshotPendingEventsForUpdate(maxRetries: Int, batchSize: Int) =
        eventJpaRepository.findSnapshotPendingEventsForUpdate(maxRetries, batchSize)
}
