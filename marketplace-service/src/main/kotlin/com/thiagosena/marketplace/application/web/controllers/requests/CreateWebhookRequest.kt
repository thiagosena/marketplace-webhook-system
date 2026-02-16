package com.thiagosena.marketplace.application.web.controllers.requests

import com.thiagosena.marketplace.domain.entities.Webhook
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class CreateWebhookRequest(
    @field:NotEmpty(message = "Webhook must have at least one store ID")
    val storeIds: List<String>,

    @field:NotBlank(message = "Callback URL is required")
    @field:Pattern(
        regexp = "^https?://.+",
        message = "Invalid callback URL format"
    )
    val callbackUrl: String,

    @field:NotBlank(message = "Token is required")
    val token: String
) {
    fun toDomain() = Webhook(storeIds = storeIds, callbackUrl = callbackUrl, token = token)
}
