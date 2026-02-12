package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OrderStatus
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.InvalidOrderStatusTransitionException
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import jakarta.transaction.Transactional
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(OrderService::class.java)

    @Transactional
    fun createOrder(order: Order): Order {
        log.debug("Creating order: {}", order)

        return orderRepository.save(order).also { orderSaved ->
            log.info("Order created: {}", orderSaved)
            outboxEventRepository.save(
                OutboxEvent(
                    eventType = orderSaved.toEventTypeEnum().type,
                    payload = objectMapper.writeValueAsString(orderSaved),
                    aggregateId = orderSaved.id ?: error("Order ID cannot be null"),
                    aggregateType = AggregateType.ORDER
                )
            ).also {
                log.info("Outbox event created: {}", it)
            }
        }
    }

    fun findById(orderId: UUID) = orderRepository.findById(orderId) ?: throw OrderNotFoundException(
        ErrorType.ORDER_NOT_FOUND.name,
        "Order with id=$orderId not found"
    )

    @Transactional
    fun updateOrderStatusById(orderId: UUID, orderStatus: OrderStatus): Order {
        val order = orderRepository.findById(orderId)
            ?: throw OrderNotFoundException(
                ErrorType.ORDER_NOT_FOUND.name,
                "Order with id=$orderId not found"
            )

        require(order.canTransitionTo(orderStatus)) {
            throw InvalidOrderStatusTransitionException(
                ErrorType.INVALID_STATUS_TRANSITION.name,
                "Cannot transition from ${order.status} to $orderStatus"
            )
        }

        return orderRepository.save(order.copy(status = orderStatus)).also { orderSaved ->
            outboxEventRepository.save(
                OutboxEvent(
                    eventType = orderSaved.toEventTypeEnum().type,
                    payload = objectMapper.writeValueAsString(orderSaved),
                    aggregateId = orderSaved.id ?: error("Order ID cannot be null"),
                    aggregateType = AggregateType.ORDER
                )
            )
            log.info("Order status updated from: {} to {}", order.status, orderSaved.status)
        }
    }
}
