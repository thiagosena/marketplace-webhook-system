package com.thiagosena.receiver.factory

import com.thiagosena.receiver.domain.gateways.responses.OrderItemResponse
import com.thiagosena.receiver.domain.gateways.responses.OrderResponse
import com.thiagosena.receiver.domain.gateways.responses.OrderStatus
import java.math.BigDecimal
import java.util.*

object OrderFactory {
    fun sampleOrderResponse(
        orderId: String = UUID.randomUUID().toString(),
        storeId: String = UUID.randomUUID().toString(),
        status: OrderStatus = OrderStatus.CREATED,
        items: List<OrderItemResponse> = listOf(sampleOrderItemResponse())
    ) = OrderResponse(
        id = orderId,
        storeId = storeId,
        status = status,
        items = items
    )

    fun sampleOrderItemResponse(
        productName: String = "Sample Product",
        quantity: Int = 1,
        unitPrice: BigDecimal = BigDecimal("10.00"),
        discount: BigDecimal = BigDecimal.ZERO,
        tax: BigDecimal = BigDecimal.ZERO
    ) = OrderItemResponse(
        productName = productName,
        quantity = quantity,
        unitPrice = unitPrice,
        discount = discount,
        tax = tax
    )
}
