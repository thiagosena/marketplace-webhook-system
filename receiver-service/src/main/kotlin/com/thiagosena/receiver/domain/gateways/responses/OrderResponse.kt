package com.thiagosena.receiver.domain.gateways.responses

import java.math.BigDecimal

data class OrderResponse(
    val id: String,
    val storeId: String,
    val status: OrderStatus,
    val items: List<OrderItemResponse>
)

data class OrderItemResponse(
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO
)

enum class OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    COMPLETED,
    DELIVERED,
    CANCELED
}
