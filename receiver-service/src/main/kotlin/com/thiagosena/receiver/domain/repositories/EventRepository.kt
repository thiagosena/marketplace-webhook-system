package com.thiagosena.receiver.domain.repositories

import com.thiagosena.receiver.domain.entities.Event

interface EventRepository {
    fun save(event: Event): Event
    fun existsByIdempotencyKey(idempotencyKey: String): Boolean
    fun findSnapshotPendingEventsForUpdate(maxRetries: Int, batchSize: Int): List<Event>
}
