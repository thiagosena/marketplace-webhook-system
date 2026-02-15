package com.thiagosena.receiver.domain.entities.dto

import java.time.LocalDateTime

data class EventWebhookDto(
    val idempotencyKey: String,
    val eventType: String,
    val orderId: String,
    val storeId: String,
    val receivedAt: LocalDateTime = LocalDateTime.now()
)
