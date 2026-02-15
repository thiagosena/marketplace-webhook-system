package com.thiagosena.receiver.resources.gateways

import com.thiagosena.receiver.domain.exceptions.ErrorType
import com.thiagosena.receiver.domain.exceptions.MarketplaceOrderNotFoundException
import com.thiagosena.receiver.domain.exceptions.MarketplaceServiceUnavailableException
import com.thiagosena.receiver.domain.gateways.MarketplaceGateway
import com.thiagosena.receiver.domain.gateways.responses.OrderResponse
import com.thiagosena.receiver.resources.gateways.clients.MarketplaceClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component

@Component
class MarketplaceGatewayImpl(private val marketplaceClient: MarketplaceClient) : MarketplaceGateway {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Suppress("TooGenericExceptionCaught")
    @CircuitBreaker(name = "marketplaceService", fallbackMethod = "fallbackFindOrderById")
    @Retry(name = "marketplaceService")
    override fun findOrderById(orderId: String): OrderResponse {
        try {
            return marketplaceClient.findOrderById(orderId)
        } catch (ex: Exception) {
            log.error(ex) { "Error processing request" }
            throw ex
        }
    }

    private fun fallbackFindOrderById(orderId: String, ex: Throwable): OrderResponse {
        log.error { "Fallback method called for orderId: $orderId" }
        if (ex is MarketplaceOrderNotFoundException) {
            throw ex
        }
        throw MarketplaceServiceUnavailableException(
            ErrorType.MARKETPLACE_SERVICE_UNAVAILABLE.name,
            "Marketplace service is currently unavailable. Fallback method called."
        )
    }
}
