package com.thiagosena.marketplace.application.web.controllers.requests

import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OrderItem
import com.thiagosena.marketplace.domain.entities.OrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class CreateOrderRequest(
    @field:NotBlank
    val storeId: String,
    @field:NotEmpty
    val items: List<OrderItemRequest>
) {
    fun toDomain() = Order(
        storeId = storeId,
        status = OrderStatus.CREATED,
        totalAmount = items.sumOf { it.getTotal() },
    ).also { order ->
        order.addItems(items.let { items ->
            items.map { item ->
                OrderItem(
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    tax = item.tax,
                    order = order
                )
            }
        }.toMutableList())
    }
}