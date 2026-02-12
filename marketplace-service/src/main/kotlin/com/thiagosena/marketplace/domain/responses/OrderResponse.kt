package com.thiagosena.marketplace.domain.responses

data class OrderResponse(
    val id: String,
    val storeId: String,
    val items: List<OrderItemResponse>,
)
