package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OrderStatus
import com.thiagosena.marketplace.domain.entities.responses.OrderResponse
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.InvalidOrderStatusTransitionException
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
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
    fun createOrder(order: Order): OrderResponse = orderRepository.save(order).also { orderSaved ->
        outboxEventRepository.save(
            outboxEvent = orderSaved.toOutboxEvent(
                idempotencyKey = generatedIdempotencyKey(orderSaved),
                objectMapper
            )
        )
        log.info { "Order created: $orderSaved" }
    }.toResponse()

    fun findById(orderId: UUID): OrderResponse = findOrderById(orderId).toResponse()

    @Transactional
    fun updateOrderStatusById(orderId: UUID, orderStatus: OrderStatus): OrderResponse {
        val order = findOrderById(orderId)

        require(order.canTransitionTo(orderStatus)) {
            throw InvalidOrderStatusTransitionException(
                ErrorType.INVALID_STATUS_TRANSITION.name,
                "Cannot transition from ${order.status} to $orderStatus"
            )
        }

        return orderRepository.save(order.copy(status = orderStatus)).also { orderSaved ->
            outboxEventRepository.save(
                outboxEvent = orderSaved.toOutboxEvent(
                    idempotencyKey = generatedIdempotencyKey(orderSaved),
                    objectMapper
                )
            )
            log.info { "Order status updated from: ${order.status} to ${orderSaved.status}" }
        }.toResponse()
    }

    private fun findOrderById(orderId: UUID) = orderRepository.findById(orderId)
        ?: throw OrderNotFoundException(
            ErrorType.ORDER_NOT_FOUND.name,
            "Order with id=$orderId not found"
        )

    private fun generatedIdempotencyKey(order: Order) = "${order.toEventTypeEnum().type}-${order.id}-${order.createdAt}"
}
