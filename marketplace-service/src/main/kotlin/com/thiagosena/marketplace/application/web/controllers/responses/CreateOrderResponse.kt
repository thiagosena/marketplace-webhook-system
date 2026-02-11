package com.thiagosena.marketplace.application.web.controllers.responses

data class CreateOrderResponse(
    val id: String,
    val storeId: String,
    val items: List<OrderItemResponse>
)