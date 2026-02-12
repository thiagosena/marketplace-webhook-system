package com.thiagosena.marketplace.application.web.controllers.responses

data class OrderResponse(
    val id: String,
    val storeId: String,
    val items: List<OrderItemResponse>,
)
