package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OrderItem
import com.thiagosena.marketplace.domain.entities.OrderStatus
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.InvalidOrderStatusTransitionException
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.math.BigDecimal
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper

class OrderServiceTest {
    private val orderRepository = mockk<OrderRepository>()
    private val outboxEventRepository = mockk<OutboxEventRepository>()
    private val objectMapper = mockk<ObjectMapper>()
    private val service = OrderService(orderRepository, outboxEventRepository, objectMapper)

    @Test
    fun `given a valid order, it should create and save the order request and create an outbox event`() {
        val order =
            Order(
                storeId = "store-123",
                totalAmount = BigDecimal("25.50")
            )
        val savedOrder = order.copy(id = UUID.randomUUID())

        every { orderRepository.save(order) } returns savedOrder
        every { objectMapper.writeValueAsString(savedOrder) } returns """{"id": null}"""

        val outboxSlot = slot<OutboxEvent>()
        every { outboxEventRepository.save(capture(outboxSlot)) } answers { firstArg() }

        service.createOrder(order)

        verify(exactly = 1) { orderRepository.save(order) }
        verify(exactly = 1) { objectMapper.writeValueAsString(savedOrder) }
        verify(exactly = 1) { outboxEventRepository.save(any()) }

        val outboxEvent = outboxSlot.captured
        assertEquals(EventType.ORDER_CREATED.type, outboxEvent.eventType)
        assertEquals("""{"id": null}""", outboxEvent.payload)
        assertEquals(savedOrder.id, outboxEvent.aggregateId)
        assertEquals(AggregateType.ORDER, outboxEvent.aggregateType)
    }

    @Test
    fun `given an invalid order, it should throws IllegalStateException when saved order has null id`() {
        val order =
            Order(
                storeId = "store-999",
                totalAmount = BigDecimal("10.00")
            )

        every { orderRepository.save(order) } returns order
        every { objectMapper.writeValueAsString(order) } returns """{"id": null}"""

        assertThrows(IllegalStateException::class.java) {
            service.createOrder(order)
        }

        verify(exactly = 1) { orderRepository.save(order) }
        verify(exactly = 1) { objectMapper.writeValueAsString(order) }
        verify(exactly = 0) { outboxEventRepository.save(any()) }
    }

    @Test
    fun `given a valid order id, it should returns order response when order exists`() {
        val orderId = UUID.randomUUID()
        val order =
            Order(
                id = orderId,
                storeId = "store-777",
                totalAmount = BigDecimal("42.00")
            )
        val item =
            OrderItem(
                productName = "Product A",
                quantity = 2,
                unitPrice = BigDecimal("10.00"),
                discount = BigDecimal("1.00"),
                tax = BigDecimal("0.50"),
                order = order
            )
        order.addItem(item)

        every { orderRepository.findById(orderId) } returns order

        val response = service.findById(orderId)

        verify(exactly = 1) { orderRepository.findById(orderId) }
        assertEquals(orderId, response.id)
        assertEquals(order.storeId, response.storeId)
        assertEquals(1, response.items.size)
        assertEquals(item.productName, response.items[0].productName)
        assertEquals(item.quantity, response.items[0].quantity)
        assertEquals(item.unitPrice, response.items[0].unitPrice)
        assertEquals(item.discount, response.items[0].discount)
        assertEquals(item.tax, response.items[0].tax)
    }

    @Test
    fun `should throws when order does not exist`() {
        val orderId = UUID.randomUUID()

        every { orderRepository.findById(orderId) } returns null

        val exception =
            assertThrows(OrderNotFoundException::class.java) {
                service.findById(orderId)
            }

        verify(exactly = 1) { orderRepository.findById(orderId) }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals(ErrorType.ORDER_NOT_FOUND.name, exception.type)
        assertEquals("Order with id=$orderId not found", exception.reason)
    }

    @Test
    fun `given a valid order status transition, it should update order status and create outbox event`() {
        val orderId = UUID.randomUUID()
        val order =
            Order(
                id = orderId,
                storeId = "store-111",
                status = OrderStatus.CREATED,
                totalAmount = BigDecimal("100.00")
            )
        val updatedOrder = order.copy(status = OrderStatus.PAID)

        every { orderRepository.findById(orderId) } returns order
        every { orderRepository.save(updatedOrder) } returns updatedOrder
        every { objectMapper.writeValueAsString(updatedOrder) } returns """{"id": "$orderId"}"""

        val outboxSlot = slot<OutboxEvent>()
        every { outboxEventRepository.save(capture(outboxSlot)) } answers { firstArg() }

        val result = service.updateOrderStatusById(orderId, OrderStatus.PAID)

        verify(exactly = 1) { orderRepository.findById(orderId) }
        verify(exactly = 1) { orderRepository.save(updatedOrder) }
        verify(exactly = 1) { objectMapper.writeValueAsString(updatedOrder) }
        verify(exactly = 1) { outboxEventRepository.save(any()) }
        assertEquals(OrderStatus.PAID, result.status)

        val outboxEvent = outboxSlot.captured
        assertEquals(EventType.ORDER_PAID.type, outboxEvent.eventType)
        assertEquals("""{"id": "$orderId"}""", outboxEvent.payload)
        assertEquals(orderId, outboxEvent.aggregateId)
        assertEquals(AggregateType.ORDER, outboxEvent.aggregateType)
    }

    @Test
    fun `given an invalid order status transition, it should throw and not persist`() {
        val orderId = UUID.randomUUID()
        val order =
            Order(
                id = orderId,
                storeId = "store-222",
                status = OrderStatus.CREATED,
                totalAmount = BigDecimal("60.00")
            )

        every { orderRepository.findById(orderId) } returns order

        assertThrows(InvalidOrderStatusTransitionException::class.java) {
            service.updateOrderStatusById(orderId, OrderStatus.SHIPPED)
        }

        verify(exactly = 1) { orderRepository.findById(orderId) }
        verify(exactly = 0) { orderRepository.save(any()) }
        verify(exactly = 0) { outboxEventRepository.save(any()) }
    }

    @Test
    fun `given a missing order, it should throw not found when updating status`() {
        val orderId = UUID.randomUUID()

        every { orderRepository.findById(orderId) } returns null

        val exception =
            assertThrows(OrderNotFoundException::class.java) {
                service.updateOrderStatusById(orderId, OrderStatus.PAID)
            }

        verify(exactly = 1) { orderRepository.findById(orderId) }
        verify(exactly = 0) { orderRepository.save(any()) }
        verify(exactly = 0) { outboxEventRepository.save(any()) }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertEquals(ErrorType.ORDER_NOT_FOUND.name, exception.type)
        assertEquals("Order with id=$orderId not found", exception.reason)
    }

    @Test
    fun `given a saved order without id, it should throw when updating status`() {
        val orderId = UUID.randomUUID()
        val order =
            Order(
                id = orderId,
                storeId = "store-333",
                status = OrderStatus.PAID,
                totalAmount = BigDecimal("75.00")
            )
        val updatedOrder = order.copy(id = null, status = OrderStatus.SHIPPED)

        every { orderRepository.findById(orderId) } returns order
        every { orderRepository.save(any()) } returns updatedOrder
        every { objectMapper.writeValueAsString(updatedOrder) } returns """{"id": null}"""

        assertThrows(IllegalStateException::class.java) {
            service.updateOrderStatusById(orderId, OrderStatus.SHIPPED)
        }

        verify(exactly = 1) { orderRepository.findById(orderId) }
        verify(exactly = 1) { orderRepository.save(any()) }
        verify(exactly = 1) { objectMapper.writeValueAsString(updatedOrder) }
        verify(exactly = 0) { outboxEventRepository.save(any()) }
    }
}
