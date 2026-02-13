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
import com.thiagosena.marketplace.domain.responses.OrderResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.transaction.Transactional
import java.util.*
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val outboxEventRepository: OutboxEventRepository,
    private val objectMapper: ObjectMapper
) {
    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Transactional
    fun createOrder(order: Order): OrderResponse {
        log.debug { "Creating order: $order" }

        return orderRepository.save(order).also { orderSaved ->
            log.info { "Order created: $orderSaved" }
            outboxEventRepository.save(
                OutboxEvent(
                    eventType = orderSaved.toEventTypeEnum().type,
                    payload = objectMapper.writeValueAsString(orderSaved),
                    aggregateId = orderSaved.storeId,
                    aggregateType = AggregateType.ORDER
                )
            ).also {
                log.info { "Outbox event created: $it" }
            }
        }.toResponse()
    }

    fun findById(orderId: UUID): OrderResponse =
        orderRepository.findById(orderId)?.toResponse() ?: throw OrderNotFoundException(
            ErrorType.ORDER_NOT_FOUND.name,
            "Order with id=$orderId not found"
        )

    @Transactional
    fun updateOrderStatusById(orderId: UUID, orderStatus: OrderStatus): OrderResponse {
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
                    aggregateId = orderSaved.storeId,
                    aggregateType = AggregateType.ORDER
                )
            )
            log.info { "Order status updated from: ${order.status} to ${orderSaved.status}" }
        }.toResponse()
    }
}
