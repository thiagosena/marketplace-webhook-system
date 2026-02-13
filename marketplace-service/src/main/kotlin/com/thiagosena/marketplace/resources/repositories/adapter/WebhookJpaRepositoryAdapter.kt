package com.thiagosena.marketplace.resources.repositories.adapter

import com.thiagosena.marketplace.domain.entities.Webhook
import com.thiagosena.marketplace.domain.repositories.WebhookRepository
import com.thiagosena.marketplace.resources.repositories.jpa.WebhookJpaRepository
import org.springframework.stereotype.Repository

@Repository
class WebhookJpaRepositoryAdapter(private val webhookJpaRepository: WebhookJpaRepository) : WebhookRepository {
    override fun save(webhook: Webhook): Webhook = webhookJpaRepository.save(webhook)

    override fun findActiveByStoreId(storeId: String): List<Webhook> = webhookJpaRepository.findActiveByStoreId(storeId)
}
