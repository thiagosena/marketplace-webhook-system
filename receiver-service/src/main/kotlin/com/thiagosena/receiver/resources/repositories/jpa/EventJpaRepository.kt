package com.thiagosena.receiver.resources.repositories.jpa

import com.thiagosena.receiver.domain.entities.Event
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventJpaRepository : JpaRepository<Event, UUID> {
    fun existsByIdempotencyKey(eventId: String): Boolean

    @Query(
        """
        SELECT * FROM events event
        WHERE event.status = 'SNAPSHOT_PENDING'
        AND event.retry_count <= :maxRetries
        AND COALESCE(event.next_retry_at, TIMESTAMP '-infinity') <= NOW()
        ORDER BY COALESCE(event.next_retry_at, TIMESTAMP '-infinity')
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
    """,
        nativeQuery = true
    )
    fun findSnapshotPendingEventsForUpdate(maxRetries: Int, batchSize: Int): List<Event>
}
