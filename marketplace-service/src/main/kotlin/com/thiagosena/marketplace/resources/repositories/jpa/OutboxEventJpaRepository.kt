package com.thiagosena.marketplace.resources.repositories.jpa

import com.thiagosena.marketplace.domain.entities.OutboxEvent
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface OutboxEventJpaRepository : JpaRepository<OutboxEvent, UUID> {
    @Query(
        """
        SELECT * FROM outbox_events event 
        WHERE event.status = 'PENDING' 
        AND event.retry_count <= :maxRetries 
        AND COALESCE(event.next_retry_at, TIMESTAMP '-infinity') <= NOW()
        ORDER BY COALESCE(event.next_retry_at, TIMESTAMP '-infinity')
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
    """,
        nativeQuery = true
    )
    fun findPendingEventsForUpdate(maxRetries: Int, batchSize: Int): List<OutboxEvent>
}
