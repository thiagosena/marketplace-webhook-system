package com.thiagosena.receiver.resources.gateways.clients

import com.thiagosena.receiver.domain.gateways.responses.OrderResponse
import com.thiagosena.receiver.resources.decode.MarketplaceErrorDecoder
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@FeignClient(
    name = "marketplace-service",
    url = $$"${spring.cloud.openfeign.client.config.marketplace-service.url}",
    configuration = [MarketplaceErrorDecoder::class]
)
interface MarketplaceClient {
    @GetMapping("/api/v1/orders/{orderId}")
    fun findOrderById(@PathVariable orderId: String): OrderResponse
}
