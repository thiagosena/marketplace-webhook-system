package com.thiagosena.receiver.resources.gateways

import com.thiagosena.receiver.domain.exceptions.ErrorType
import com.thiagosena.receiver.domain.exceptions.MarketplaceOrderNotFoundException
import com.thiagosena.receiver.domain.exceptions.MarketplaceServiceUnavailableException
import com.thiagosena.receiver.domain.gateways.responses.OrderStatus
import com.thiagosena.receiver.factory.OrderFactory
import com.thiagosena.receiver.resources.gateways.clients.MarketplaceClient
import feign.FeignException
import feign.Request
import feign.RequestTemplate
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.lang.reflect.InvocationTargetException
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MarketplaceGatewayImplTest {
    private val marketplaceClient = mockk<MarketplaceClient>()
    private val gateway = MarketplaceGatewayImpl(marketplaceClient)

    private fun createFeignRequest(): Request = Request.create(
        Request.HttpMethod.GET,
        "/api/orders/123",
        emptyMap(),
        null,
        StandardCharsets.UTF_8,
        RequestTemplate()
    )

    @Test
    fun `given a valid orderId, it should return OrderResponse successfully`() {
        val orderId = UUID.randomUUID().toString()
        val orderItemResponse = OrderFactory.sampleOrderItemResponse(productName = "Product A", quantity = 1)
        val expectedResponse = OrderFactory.sampleOrderResponse(
            orderId,
            status = OrderStatus.CREATED,
            items = listOf(orderItemResponse)
        )

        every { marketplaceClient.findOrderById(orderId) } returns expectedResponse

        val result = gateway.findOrderById(orderId)

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals(expectedResponse.id, result.id)
        assertEquals(expectedResponse.storeId, result.storeId)
        assertEquals(expectedResponse.status, result.status)
        assertEquals("Product A", result.items[0].productName)
        assertEquals(1, result.items[0].quantity)
        assertTrue(result.items[0].unitPrice > BigDecimal.ZERO)
    }

    @Test
    fun `given an orderId that does not exist, it should throw MarketplaceOrderNotFoundException`() {
        val orderId = UUID.randomUUID().toString()

        every { marketplaceClient.findOrderById(orderId) } throws
            MarketplaceOrderNotFoundException(
                ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name,
                "Order with id=$orderId not found"
            )

        val exception =
            assertThrows(MarketplaceOrderNotFoundException::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals(ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name, exception.type)
        assertEquals("Order with id=$orderId not found", exception.reason)
    }

    @Test
    fun `given a service failure, it should throw and log the error`() {
        val orderId = UUID.randomUUID().toString()
        val feignException =
            FeignException.InternalServerError(
                "Internal error",
                createFeignRequest(),
                null,
                null
            )

        every { marketplaceClient.findOrderById(orderId) } throws feignException

        val exception =
            assertThrows(FeignException.InternalServerError::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Internal error", exception.message)
    }

    @Test
    fun `given a timeout exception, it should throw the exception`() {
        val orderId = UUID.randomUUID().toString()
        val timeoutException = RuntimeException("Request timeout")

        every { marketplaceClient.findOrderById(orderId) } throws timeoutException

        val exception =
            assertThrows(RuntimeException::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Request timeout", exception.message)
    }

    @Test
    fun `given multiple successful calls, it should return responses for all calls`() {
        val orderId1 = UUID.randomUUID().toString()
        val orderId2 = UUID.randomUUID().toString()
        val orderItemResponse1 = OrderFactory.sampleOrderItemResponse(productName = "Product A", quantity = 1)
        val response1 = OrderFactory.sampleOrderResponse(
            orderId1,
            status = OrderStatus.CREATED,
            items = listOf(orderItemResponse1)
        )

        val orderItemResponse2 = OrderFactory.sampleOrderItemResponse(
            productName = "Product B",
            quantity = 1,
            unitPrice = BigDecimal("75.00")
        )
        val response2 = OrderFactory.sampleOrderResponse(
            orderId2,
            status = OrderStatus.PAID,
            items = listOf(orderItemResponse2)
        )

        every { marketplaceClient.findOrderById(orderId1) } returns response1
        every { marketplaceClient.findOrderById(orderId2) } returns response2

        val result1 = gateway.findOrderById(orderId1)
        val result2 = gateway.findOrderById(orderId2)

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId1) }
        verify(exactly = 1) { marketplaceClient.findOrderById(orderId2) }
        assertEquals(orderId1, result1.id)
        assertEquals(orderId2, result2.id)
        assertEquals(OrderStatus.CREATED, result1.status)
        assertEquals(OrderStatus.PAID, result2.status)
    }

    @Test
    fun `given a BadRequest exception, it should propagate the exception`() {
        val orderId = "invalid-uuid"
        val badRequestException =
            FeignException.BadRequest(
                "Invalid order ID format",
                createFeignRequest(),
                null,
                null
            )

        every { marketplaceClient.findOrderById(orderId) } throws badRequestException

        val exception =
            assertThrows(FeignException.BadRequest::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Invalid order ID format", exception.message)
    }

    @Test
    fun `given a ServiceUnavailable exception, it should propagate the exception`() {
        val orderId = UUID.randomUUID().toString()
        val serviceUnavailableException =
            FeignException.ServiceUnavailable(
                "Service temporarily unavailable",
                createFeignRequest(),
                null,
                null
            )

        every { marketplaceClient.findOrderById(orderId) } throws serviceUnavailableException

        val exception =
            assertThrows(FeignException.ServiceUnavailable::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Service temporarily unavailable", exception.message)
    }

    @Test
    fun `given an order with items, it should return complete OrderResponse with items`() {
        val orderId = UUID.randomUUID().toString()
        val orderItemResponse1 = OrderFactory.sampleOrderItemResponse(
            productName = "Product A",
            quantity = 2,
            unitPrice = BigDecimal("25.00"),
            discount = BigDecimal("5.00"),
            tax = BigDecimal("2.50")
        )
        val orderItemResponse2 = OrderFactory.sampleOrderItemResponse(
            productName = "Product B",
            quantity = 1,
            unitPrice = BigDecimal("50.00"),
            discount = BigDecimal("0.00"),
            tax = BigDecimal("5.00")
        )
        val expectedResponse = OrderFactory.sampleOrderResponse(
            orderId,
            status = OrderStatus.COMPLETED,
            items = listOf(orderItemResponse1, orderItemResponse2)
        )

        every { marketplaceClient.findOrderById(orderId) } returns expectedResponse

        val result = gateway.findOrderById(orderId)

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals(orderId, result.id)
        assertEquals(2, result.items.size)
        assertEquals("Product A", result.items[0].productName)
        assertEquals(2, result.items[0].quantity)
        assertEquals(BigDecimal("25.00"), result.items[0].unitPrice)
        assertEquals("Product B", result.items[1].productName)
        assertEquals(1, result.items[1].quantity)
    }

    @Test
    fun `given a Forbidden exception, it should propagate the exception`() {
        val orderId = UUID.randomUUID().toString()
        val forbiddenException =
            FeignException.Forbidden(
                "Access denied",
                createFeignRequest(),
                null,
                null
            )

        every { marketplaceClient.findOrderById(orderId) } throws forbiddenException

        val exception =
            assertThrows(FeignException.Forbidden::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Access denied", exception.message)
    }

    @Test
    fun `given a null pointer exception from client, it should propagate the exception`() {
        val orderId = UUID.randomUUID().toString()
        val nullPointerException = NullPointerException("Unexpected null value")

        every { marketplaceClient.findOrderById(orderId) } throws nullPointerException

        val exception =
            assertThrows(NullPointerException::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Unexpected null value", exception.message)
    }

    @Test
    fun `given an IllegalArgumentException from client, it should propagate the exception`() {
        val orderId = ""
        val illegalArgumentException = IllegalArgumentException("Order ID cannot be empty")

        every { marketplaceClient.findOrderById(orderId) } throws illegalArgumentException

        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Order ID cannot be empty", exception.message)
    }

    @Test
    fun `given an order with zero total amount, it should return OrderResponse successfully`() {
        val orderId = UUID.randomUUID().toString()
        val expectedResponse = OrderFactory.sampleOrderResponse(orderId, status = OrderStatus.COMPLETED)

        every { marketplaceClient.findOrderById(orderId) } returns expectedResponse

        val result = gateway.findOrderById(orderId)

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals(expectedResponse.id, result.id)
        assertEquals(expectedResponse.status, result.status)
    }

    @Test
    fun `given a TooManyRequests exception, it should propagate the exception`() {
        val orderId = UUID.randomUUID().toString()
        val tooManyRequestsException =
            FeignException.TooManyRequests(
                "Rate limit exceeded",
                createFeignRequest(),
                null,
                null
            )

        every { marketplaceClient.findOrderById(orderId) } throws tooManyRequestsException

        val exception =
            assertThrows(FeignException.TooManyRequests::class.java) {
                gateway.findOrderById(orderId)
            }

        verify(exactly = 1) { marketplaceClient.findOrderById(orderId) }
        assertEquals("Rate limit exceeded", exception.message)
    }

    @Test
    fun `given TimeoutException in fallback, it should throw MarketplaceServiceUnavailableException`() {
        val orderId = UUID.randomUUID().toString()
        val timeoutException = java.util.concurrent.TimeoutException("Request timeout")

        val exception =
            assertThrows(MarketplaceServiceUnavailableException::class.java) {
                invokeFallback(orderId, timeoutException)
            }

        assertEquals(ErrorType.MARKETPLACE_SERVICE_UNAVAILABLE.name, exception.type)
        assertEquals(
            "Marketplace service is currently unavailable. Fallback method called.",
            exception.reason
        )
    }

    @Test
    fun `given MarketplaceOrderNotFoundException in fallback, it should rethrow the same exception`() {
        val orderId = UUID.randomUUID().toString()
        val notFoundException =
            MarketplaceOrderNotFoundException(
                ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name,
                "Order with id=$orderId not found"
            )

        val exception =
            assertThrows(MarketplaceOrderNotFoundException::class.java) {
                invokeFallback(orderId, notFoundException)
            }

        assertEquals(ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name, exception.type)
        assertEquals("Order with id=$orderId not found", exception.reason)
    }

    private fun invokeFallback(orderId: String, throwable: Throwable) {
        val fallbackMethod = gateway.javaClass.getDeclaredMethod(
            "fallbackFindOrderById",
            String::class.java,
            Throwable::class.java
        )
        fallbackMethod.isAccessible = true

        try {
            fallbackMethod.invoke(gateway, orderId, throwable)
        } catch (e: InvocationTargetException) {
            // A exceção real está na causa do InvocationTargetException
            throw e.cause ?: e
        }
    }
}
