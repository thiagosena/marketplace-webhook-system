package com.thiagosena.marketplace.domain.entities.responses

import com.thiagosena.marketplace.domain.entities.OrderStatus
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
