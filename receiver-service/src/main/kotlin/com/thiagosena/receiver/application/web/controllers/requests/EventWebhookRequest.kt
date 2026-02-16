package com.thiagosena.receiver.application.web.controllers.requests

import com.thiagosena.receiver.domain.entities.dto.EventWebhookDto
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

data class EventWebhookRequest(
    @field:NotBlank
    val idempotencyKey: String,
    @field:NotBlank
    val eventType: String,
    @field:NotBlank
    val orderId: String,
    @field:NotBlank
    val storeId: String,
    @field:NotBlank
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
