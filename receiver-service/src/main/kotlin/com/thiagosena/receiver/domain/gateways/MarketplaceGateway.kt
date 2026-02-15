package com.thiagosena.receiver.domain.gateways

import com.thiagosena.receiver.domain.gateways.responses.OrderResponse

interface MarketplaceGateway {
    fun findOrderById(orderId: String): OrderResponse
}
