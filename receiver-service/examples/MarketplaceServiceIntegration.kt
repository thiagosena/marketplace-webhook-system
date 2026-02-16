package com.thiagosena.marketplace.infrastructure.config

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.time.Instant

/**
 * Exemplo de integração do Marketplace Service com o Receiver Service
 * 
 * Para usar este exemplo:
 * 1. Copie este arquivo para: marketplace-service/src/main/kotlin/com/thiagosena/marketplace/infrastructure/
 * 2. Adicione a configuração no application.yaml do marketplace-service
 * 3. Configure a variável de ambiente RECEIVER_SERVICE_TOKEN
 */

// ============================================================================
// CONFIGURAÇÃO DO FEIGN CLIENT
// ============================================================================

@Configuration
class ReceiverServiceFeignConfig {

    @Bean
    fun receiverServiceRequestInterceptor(
        @Value("\${config.service.receiver.token}") token: String
    ): RequestInterceptor {
        return RequestInterceptor { template: RequestTemplate ->
            template.header("Authorization", token)
        }
    }
}

// ============================================================================
// FEIGN CLIENT INTERFACE
// ============================================================================

@FeignClient(
    name = "receiver-service",
    url = "\${spring.cloud.openfeign.client.config.receiver-service.url}",
    configuration = [ReceiverServiceFeignConfig::class]
)
interface ReceiverServiceClient {

    @PostMapping("/api/v1/events")
    fun sendEvent(@RequestBody event: EventWebhookRequest)
}

// ============================================================================
// REQUEST/RESPONSE MODELS
// ============================================================================

data class EventWebhookRequest(
    val idempotencyKey: String,
    val eventType: String,
    val orderId: String,
    val storeId: String,
    val createdAt: String
)

// ============================================================================
// SERVICE PARA PUBLICAR EVENTOS
// ============================================================================

@org.springframework.stereotype.Service
class EventPublisherService(
    private val receiverServiceClient: ReceiverServiceClient
) {

    fun publishOrderCreated(orderId: String, storeId: String) {
        val event = EventWebhookRequest(
            idempotencyKey = "order-created-$orderId-${System.currentTimeMillis()}",
            eventType = "order.created",
            orderId = orderId,
            storeId = storeId,
            createdAt = Instant.now().toString()
        )
        
        receiverServiceClient.sendEvent(event)
    }

    fun publishOrderUpdated(orderId: String, storeId: String) {
        val event = EventWebhookRequest(
            idempotencyKey = "order-updated-$orderId-${System.currentTimeMillis()}",
            eventType = "order.updated",
            orderId = orderId,
            storeId = storeId,
            createdAt = Instant.now().toString()
        )
        
        receiverServiceClient.sendEvent(event)
    }

    fun publishOrderCancelled(orderId: String, storeId: String) {
        val event = EventWebhookRequest(
            idempotencyKey = "order-cancelled-$orderId-${System.currentTimeMillis()}",
            eventType = "order.cancelled",
            orderId = orderId,
            storeId = storeId,
            createdAt = Instant.now().toString()
        )
        
        receiverServiceClient.sendEvent(event)
    }
}

// ============================================================================
// CONFIGURAÇÃO NO application.yaml DO MARKETPLACE-SERVICE
// ============================================================================

/*
config:
  service:
    receiver:
      token: ${RECEIVER_SERVICE_TOKEN:change-me-in-production}

spring:
  cloud:
    openfeign:
      client:
        config:
          receiver-service:
            url: ${RECEIVER_SERVICE_URL:http://localhost:8001}
            connectTimeout: 5000
            readTimeout: 5000
*/

// ============================================================================
// EXEMPLO DE USO NO CONTROLLER OU SERVICE
// ============================================================================

/*
@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
    private val eventPublisherService: EventPublisherService
) {

    @PostMapping
    fun createOrder(@RequestBody request: CreateOrderRequest): OrderResponse {
        val order = orderService.createOrder(request)
        
        // Publica evento para o Receiver Service
        eventPublisherService.publishOrderCreated(
            orderId = order.id.toString(),
            storeId = order.storeId.toString()
        )
        
        return order.toResponse()
    }

    @PutMapping("/{orderId}/cancel")
    fun cancelOrder(@PathVariable orderId: String): OrderResponse {
        val order = orderService.cancelOrder(orderId)
        
        // Publica evento de cancelamento
        eventPublisherService.publishOrderCancelled(
            orderId = order.id.toString(),
            storeId = order.storeId.toString()
        )
        
        return order.toResponse()
    }
}
*/
