package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun createOrder(order: Order) {
        // Save order
        orderRepository.save(order).also { orderSaved ->
            outboxEventRepository.save(
                OutboxEvent(
                    eventType = "order.created",
                    payload = objectMapper.writeValueAsString(order),
                    aggregateId = orderSaved.id ?: error("Order ID cannot be null"),
                    aggregateType = "Order",
                )
            )
        }
    }
}