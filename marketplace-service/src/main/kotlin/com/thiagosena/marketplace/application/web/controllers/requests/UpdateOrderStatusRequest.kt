package com.thiagosena.marketplace.application.web.controllers.requests

import com.thiagosena.marketplace.domain.entities.OrderStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class UpdateOrderStatusRequest(
    @field:NotBlank(message = "Order status is required")
    @field:Schema(
        description = "Order status",
        allowableValues = ["CREATED", "PAID", "SHIPPED", "COMPLETED", "CANCELED"]
    )
    val status: OrderStatus
) {
    fun toDomain() = status
}
