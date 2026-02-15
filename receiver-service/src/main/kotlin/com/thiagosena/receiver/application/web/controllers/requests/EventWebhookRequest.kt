package com.thiagosena.receiver.application.web.controllers.requests

import com.thiagosena.receiver.domain.entities.dto.EventWebhookDto
import java.time.LocalDateTime

data class EventWebhookRequest(
    val idempotencyKey: String,
    val eventType: String,
    val orderId: String,
    val storeId: String,
    val createdAt: LocalDateTime
) {
    fun toDomain(): EventWebhookDto = EventWebhookDto(
        idempotencyKey = idempotencyKey,
        eventType = eventType,
        orderId = orderId,
        storeId = storeId,
        receivedAt = createdAt
    )
}
