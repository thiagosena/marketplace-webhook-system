package com.thiagosena.marketplace.application.web.controllers.responses

import java.math.BigDecimal

data class OrderItemResponse(
    val productName: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val discount: BigDecimal = BigDecimal.ZERO,
    val tax: BigDecimal = BigDecimal.ZERO
)