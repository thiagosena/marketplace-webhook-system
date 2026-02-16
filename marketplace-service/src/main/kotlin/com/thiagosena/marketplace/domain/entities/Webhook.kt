package com.thiagosena.marketplace.domain.entities

import com.thiagosena.marketplace.domain.entities.responses.WebhookResponse
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "webhooks")
data class Webhook(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    /**
     * List of store IDs that this webhook should monitor.
     * Stored as an ARRAY in PostgreSQL.
     */
    @Column(name = "store_ids", nullable = false, columnDefinition = "varchar(100)[]")
    val storeIds: List<String>,

    /**
     * Callback URL that will receive events via POST.
     */
    @Column(name = "callback_url", nullable = false, length = 500)
    val callbackUrl: String,

    /**
     * Shared secret token for authenticating requests to the receiver.
     */
    @Column(name = "token", length = 255)
    val token: String,

    /**
     * Indicates whether the webhook is active or not.
     * Inactive webhooks do not receive events.
     */
    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null
) {
    init {
        require(storeIds.isNotEmpty()) { "Webhook must have at least one store ID" }
        require(callbackUrl.isNotBlank()) { "Callback URL is required" }
        require(isValidUrl(callbackUrl)) { "Invalid callback URL format" }
    }

    fun toResponse() = id?.let {
        WebhookResponse(
            id,
            storeIds,
            callbackUrl,
            active,
            createdAt
        )
    } ?: error("Webhook must have an ID")

    companion object {
        private fun isValidUrl(url: String): Boolean = try {
            val regex = "^https?://.*".toRegex()
            url.matches(regex)
        } catch (_: Exception) {
            false
        }
    }
}
