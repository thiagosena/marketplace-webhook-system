package com.thiagosena.marketplace.application.web.controllers.requests

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class OrderItemRequest(
    @field:NotBlank(message = "Product name is required")
    val productName: String,
    @field:Positive(message = "Quantity must be greater than 0")
    val quantity: Int,
    @field:DecimalMin(
        value = "0.01",
        inclusive = true,
        message = "Price must be greater than 0"
    )
    val unitPrice: BigDecimal,
    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Discount cannot be negative"
    )
    val discount: BigDecimal = BigDecimal.ZERO,
    @field:DecimalMin(
        value = "0.00",
        inclusive = true,
        message = "Tax cannot be negative"
    )
    val tax: BigDecimal = BigDecimal.ZERO
) {
    fun getSubtotal(): BigDecimal = unitPrice.multiply(BigDecimal(quantity))
    fun getTotal(): BigDecimal = getSubtotal().subtract(discount).add(tax)
}