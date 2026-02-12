package com.thiagosena.marketplace.domain.services

import com.thiagosena.marketplace.domain.entities.AggregateType
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.entities.OutboxEvent
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.domain.repositories.OutboxEventRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.UUID

class OrderServiceTest {
    private val orderRepository = mockk<OrderRepository>()
    private val outboxEventRepository = mockk<OutboxEventRepository>()
    private val objectMapper = mockk<ObjectMapper>()
    private val service = OrderService(orderRepository, outboxEventRepository, objectMapper)

    @Test
    fun `createOrder saves order and creates outbox event`() {
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
    fun `createOrder throws when saved order has null id`() {
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
}
