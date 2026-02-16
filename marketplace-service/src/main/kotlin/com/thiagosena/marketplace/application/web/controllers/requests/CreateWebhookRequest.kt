package com.thiagosena.marketplace.application.web.controllers.requests

import com.thiagosena.marketplace.domain.entities.Webhook
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class CreateWebhookRequest(
    @field:NotEmpty(message = "Webhook must have at least one store ID")
    @field:Schema(
        description = "List of store IDs to associate with the webhook",
        example = "[\"store-123\", \"store-456\"]"
    )
    val storeIds: List<String>,

    @field:NotBlank(message = "Callback URL is required")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "Invalid callback URL format"
    )
    @field:Schema(
        description = "URL that will receive webhook notifications",
        example = "https://api.example.com/api/v1/events"
    )
    val callbackUrl: String,

    @field:NotBlank(message = "Token is required")
    @field:Schema(
        description = "Authentication token for webhook requests",
        example = "sk_live_1234567890abcdef"
    )
    val token: String
) {
    fun toDomain() = Webhook(storeIds = storeIds, callbackUrl = callbackUrl, token = token)
}
