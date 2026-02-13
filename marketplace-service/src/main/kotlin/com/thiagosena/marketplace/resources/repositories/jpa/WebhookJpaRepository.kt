package com.thiagosena.marketplace.resources.repositories.jpa

import com.thiagosena.marketplace.domain.entities.Webhook
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface WebhookJpaRepository : JpaRepository<Webhook, UUID> {
    @Query(
        """
        SELECT webhook FROM Webhook webhook 
        WHERE :storeId = ANY(webhook.storeIds)
        AND webhook.active = true
    """
    )
    fun findActiveByStoreId(@Param("storeId") storeId: String): List<Webhook>
}
