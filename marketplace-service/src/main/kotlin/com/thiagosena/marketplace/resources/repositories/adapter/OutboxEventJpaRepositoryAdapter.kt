package com.thiagosena.marketplace.resources.repositories.adapter

import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import com.thiagosena.marketplace.resources.repositories.jpa.OutboxEventJpaRepository
import org.springframework.stereotype.Repository

@Repository
class OutboxEventJpaRepositoryAdapter(private val outboxEventJpaRepository: OutboxEventJpaRepository) :
    OutboxEventRepository {
    override fun save(outboxEvent: OutboxEvent): OutboxEvent = outboxEventJpaRepository.save(outboxEvent)

    override fun findPendingEventsForUpdate(maxRetries: Int, batchSize: Int): List<OutboxEvent> =
        outboxEventJpaRepository.findPendingEventsForUpdate(maxRetries, batchSize)
}
