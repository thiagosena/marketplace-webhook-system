package com.thiagosena.receiver.resources.decode

import com.thiagosena.receiver.domain.exceptions.ErrorType
import com.thiagosena.receiver.domain.exceptions.MarketplaceOrderNotFoundException
import feign.FeignException
import feign.Request
import feign.Response
import java.nio.charset.StandardCharsets
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import tools.jackson.databind.ObjectMapper

class MarketplaceErrorDecoderTest {
    private val objectMapper = ObjectMapper()
    private val decoder = MarketplaceErrorDecoder(objectMapper)

    private fun createRequest(): Request = Request.create(
        Request.HttpMethod.GET,
        "/api/orders/123",
        emptyMap(),
        null,
        StandardCharsets.UTF_8,
        null
    )

    private fun createResponse(status: Int, body: String): Response = Response.builder()
        .status(status)
        .reason("Error")
        .request(createRequest())
        .headers(emptyMap())
        .body(body, StandardCharsets.UTF_8)
        .build()

    @Test
    fun `given 404 status with ORDER_NOT_FOUND type, it should return MarketplaceOrderNotFoundException`() {
        val errorBody = """
            {
                "type": "ORDER_NOT_FOUND",
                "message": "Order with id=123 not found"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(MarketplaceOrderNotFoundException::class.java, exception)
        val marketplaceException = exception as MarketplaceOrderNotFoundException
        assertEquals(ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name, marketplaceException.type)
        assertEquals("Order with id=123 not found", marketplaceException.reason)
    }

    @Test
    fun `given 404 status with ORDER_NOT_FOUND type and no message, it should use default message`() {
        val errorBody = """
            {
                "type": "ORDER_NOT_FOUND"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(MarketplaceOrderNotFoundException::class.java, exception)
        val marketplaceException = exception as MarketplaceOrderNotFoundException
        assertEquals(ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name, marketplaceException.type)
        assertEquals("Order not found", marketplaceException.reason)
    }

    @Test
    fun `given 404 status with different error type, it should return FeignException`() {
        val errorBody = """
            {
                "type": "STORE_NOT_FOUND",
                "message": "Store not found"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findStoreById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.NotFound)
    }

    @Test
    fun `given 400 status, it should return FeignException BadRequest`() {
        val errorBody = """
            {
                "type": "INVALID_REQUEST",
                "message": "Invalid request parameters"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.BAD_REQUEST.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#createOrder(Order)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.BadRequest)
    }

    @Test
    fun `given 500 status, it should return FeignException InternalServerError`() {
        val errorBody = """
            {
                "type": "INTERNAL_ERROR",
                "message": "Internal server error"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.InternalServerError)
    }

    @Test
    fun `given 404 status with null response body, it should return FeignException`() {
        val response = Response.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .reason("Not Found")
            .request(createRequest())
            .headers(emptyMap())
            .build()

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.NotFound)
    }

    @Test
    fun `given 404 status with empty response body, it should return FeignException`() {
        val response = createResponse(HttpStatus.NOT_FOUND.value(), "")

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.NotFound)
    }

    @Test
    fun `given 404 status with invalid JSON, it should return FeignException`() {
        val errorBody = "{ invalid json }"

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.NotFound)
    }

    @Test
    fun `given 404 status with ORDER_NOT_FOUND and additional fields, it should return MarketplaceOrderNotFoundException`() {
        val errorBody = """
            {
                "type": "ORDER_NOT_FOUND",
                "message": "Order with id=456 not found",
                "timestamp": "2025-02-15T10:00:00Z",
                "path": "/api/orders/456"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(MarketplaceOrderNotFoundException::class.java, exception)
        val marketplaceException = exception as MarketplaceOrderNotFoundException
        assertEquals(ErrorType.MARKETPLACE_ORDER_NOT_FOUND.name, marketplaceException.type)
        assertEquals("Order with id=456 not found", marketplaceException.reason)
    }

    @Test
    fun `given 403 status, it should return FeignException Forbidden`() {
        val errorBody = """
            {
                "type": "FORBIDDEN",
                "message": "Access denied"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.FORBIDDEN.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.Forbidden)
    }

    @Test
    fun `given 503 status, it should return FeignException ServiceUnavailable`() {
        val errorBody = """
            {
                "type": "SERVICE_UNAVAILABLE",
                "message": "Service temporarily unavailable"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.ServiceUnavailable)
    }

    @Test
    fun `given 404 status with case-sensitive type mismatch, it should return FeignException`() {
        val errorBody = """
            {
                "type": "order_not_found",
                "message": "Order not found"
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(FeignException::class.java, exception)
        assertTrue(exception is FeignException.NotFound)
    }

    @Test
    fun `given 404 status with ORDER_NOT_FOUND and null message explicitly, it should use default message`() {
        val errorBody = """
            {
                "type": "ORDER_NOT_FOUND",
                "message": null
            }
        """.trimIndent()

        val response = createResponse(HttpStatus.NOT_FOUND.value(), errorBody)

        val exception = decoder.decode("MarketplaceGateway#findOrderById(UUID)", response)

        assertInstanceOf(MarketplaceOrderNotFoundException::class.java, exception)
        val marketplaceException = exception as MarketplaceOrderNotFoundException
        assertEquals("Order not found", marketplaceException.reason)
    }
}
