package com.thiagosena.marketplace.domain.repositories

import com.thiagosena.marketplace.domain.entities.OutboxEvent

interface OutboxEventRepository {
    fun save(outboxEvent: OutboxEvent): OutboxEvent
}