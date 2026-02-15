package com.thiagosena.marketplace.domain.entities.responses

import java.time.LocalDateTime
import java.util.*

data class WebhookResponse(
    val id: UUID,
    val storeIds: List<String>,
    val callbackUrl: String,
    val active: Boolean,
    val createdAt: LocalDateTime
)
