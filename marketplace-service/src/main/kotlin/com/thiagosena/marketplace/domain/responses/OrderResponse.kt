package com.thiagosena.marketplace.domain.responses

import com.thiagosena.marketplace.domain.entities.OrderStatus

data class OrderResponse(
    val id: String,
    val storeId: String,
    val status: OrderStatus,
    val items: List<OrderItemResponse>
)
