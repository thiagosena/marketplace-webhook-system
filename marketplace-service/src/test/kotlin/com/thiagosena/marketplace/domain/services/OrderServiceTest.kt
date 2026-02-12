package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OrderItem
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.exceptions.ErrorType
import com.thiagosena.marketplace.domain.exceptions.OrderNotFoundException
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.*

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
                totalAmount = BigDecimal("25.50"),
            )
        val savedOrder = order.copy(id = UUID.randomUUID())

        every { orderRepository.save(order) } returns savedOrder
        every { objectMapper.writeValueAsString(order) } returns """{"id": null}"""

        val outboxSlot = slot<OutboxEvent>()
        every { outboxEventRepository.save(capture(outboxSlot)) } answers { firstArg() }

        service.createOrder(order)

        verify(exactly = 1) { orderRepository.save(order) }
        verify(exactly = 1) { objectMapper.writeValueAsString(order) }
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
                totalAmount = BigDecimal("10.00"),
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
                totalAmount = BigDecimal("42.00"),
            )
        val item =
            OrderItem(
                productName = "Product A",
                quantity = 2,
                unitPrice = BigDecimal("10.00"),
                discount = BigDecimal("1.00"),
                tax = BigDecimal("0.50"),
                order = order,
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
}
