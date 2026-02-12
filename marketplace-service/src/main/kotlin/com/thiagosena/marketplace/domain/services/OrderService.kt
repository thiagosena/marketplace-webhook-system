package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.util.*

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    @Transactional
    fun createOrder(order: Order): Order {
        log.debug("Creating order: {}", order)

        return orderRepository
            .save(order)
            .also { orderSaved ->
                log.info("Order created: {}", orderSaved)
                outboxEventRepository
                    .save(
                        OutboxEvent(
                            eventType = EventType.ORDER_CREATED.type,
                            payload = objectMapper.writeValueAsString(order),
                            aggregateId = orderSaved.id ?: error("Order ID cannot be null"),
                            aggregateType = AggregateType.ORDER,
                        ),
                    ).also {
                        log.info("Outbox event created: {}", it)
                    }
            }
    }

    fun findById(orderId: UUID) =
        orderRepository.findById(orderId) ?: throw OrderNotFoundException(
            HttpStatus.NOT_FOUND,
            ErrorType.ORDER_NOT_FOUND.name,
            "Order with id=$orderId not found",
        )
}
