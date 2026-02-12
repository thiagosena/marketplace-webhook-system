package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.application.web.controllers.responses.CreateOrderResponse
import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(OrderService::class.java)

    @Transactional
    fun createOrder(order: Order): CreateOrderResponse {
        logger.debug("Creating order: {}", order)

        return orderRepository
            .save(order)
            .also { orderSaved ->
                logger.info("Order created: {}", orderSaved)
                outboxEventRepository
                    .save(
                        OutboxEvent(
                            eventType = EventType.ORDER_CREATED.type,
                            payload = objectMapper.writeValueAsString(order),
                            aggregateId = orderSaved.id ?: error("Order ID cannot be null"),
                            aggregateType = AggregateType.ORDER,
                        ),
                    ).also {
                        logger.info("Outbox event created: {}", it)
                    }
            }.toResponse()
    }
}
