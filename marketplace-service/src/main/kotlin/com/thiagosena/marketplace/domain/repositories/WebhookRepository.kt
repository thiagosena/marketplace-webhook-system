package com.thiagosena.marketplace.domain.repositories

import com.thiagosena.marketplace.domain.entities.Webhook

interface WebhookRepository {
    fun save(webhook: Webhook): Webhook
    fun findActiveByStoreId(storeId: String): List<Webhook>
}
