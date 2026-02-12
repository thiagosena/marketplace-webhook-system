package com.thiagosena.marketplace.application.web.controllers

import com.jayway.jsonpath.JsonPath
import com.thiagosena.marketplace.MarketplaceApplication
import com.thiagosena.marketplace.application.config.TestcontainersConfiguration
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.OrderStatus
import com.thiagosena.marketplace.domain.entities.OutboxStatus
import com.thiagosena.marketplace.resources.repositories.jpa.OrderJpaRepository
import com.thiagosena.marketplace.resources.repositories.jpa.OutboxEventJpaRepository
import java.util.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = [MarketplaceApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration::class)
class OrderControllerTest(
    @param:Autowired val orderJpaRepository: OrderJpaRepository,
    @param:Autowired val outboxEventJpaRepository: OutboxEventJpaRepository
) {
    @LocalServerPort
    private var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setupWebClient() {
        webTestClient =
            WebTestClient
                .bindToServer()
                .baseUrl("http://localhost:$port")
                .build()
    }

    @AfterEach
    fun cleanup() {
        outboxEventJpaRepository.deleteAll()
        orderJpaRepository.deleteAll()
    }

    @Test
    fun `create order returns ok and persists order and outbox event`() {
        val payload =
            """
            {
              "store_id": "store-1",
              "items": [
                {
                  "product_name": "Product A",
                  "quantity": 2,
                  "unit_price": 9.95,
                  "discount": 1.00,
                  "tax": 0.50
                },
                {
                  "product_name": "Product B",
                  "quantity": 1,
                  "unit_price": 10.00,
                  "discount": 0.00,
                  "tax": 0.00
                }
              ]
            }
            """.trimIndent()
        webTestClient
            .post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isCreated

        assertThat(orderJpaRepository.count()).isEqualTo(1)
        assertThat(outboxEventJpaRepository.count()).isEqualTo(1)

        val order = orderJpaRepository.findAll().toList().first()
        assertThat(order.storeId).isEqualTo("store-1")
        assertThat(order.totalAmount).isEqualByComparingTo("29.40")

        val outboxEvent = outboxEventJpaRepository.findAll().toList().first()
        assertThat(outboxEvent.eventType).isEqualTo(EventType.ORDER_CREATED.type)
        assertThat(outboxEvent.status).isEqualTo(OutboxStatus.PENDING)
    }

    @Test
    fun `create order with invalid payload returns bad request and does not persist`() {
        val payload =
            """
            {
              "store_id": "",
              "items": []
            }
            """.trimIndent()
        webTestClient
            .post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isBadRequest

        assertThat(orderJpaRepository.count()).isEqualTo(0)
        assertThat(outboxEventJpaRepository.count()).isEqualTo(0)
    }

    @Test
    fun `get order by id returns ok with order response`() {
        val createdOrderId = createOrderPayload()

        webTestClient
            .get()
            .uri("/api/v1/orders/$createdOrderId")
            .exchange()
            .expectStatus()
            .isOk
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .isEqualTo(createdOrderId)
            .jsonPath("$.store_id")
            .isEqualTo("store-1")
    }

    @Test
    fun `get order by id when order does not exist returns not found`() {
        val missingId = UUID.randomUUID()

        webTestClient
            .get()
            .uri("/api/v1/orders/$missingId")
            .exchange()
            .expectStatus()
            .isNotFound
    }

    @Test
    fun `update order status with valid transition persists order and outbox event`() {
        val createdOrderId = createOrderPayload()

        val payload =
            """
            {
              "status": "PAID"
            }
            """.trimIndent()

        webTestClient
            .patch()
            .uri("/api/v1/orders/$createdOrderId/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isOk

        val savedOrder = orderJpaRepository.findById(UUID.fromString(createdOrderId)).orElseThrow()
        assertThat(savedOrder.status).isEqualTo(OrderStatus.PAID)
        assertThat(outboxEventJpaRepository.count()).isEqualTo(2)

        val outboxEvent = outboxEventJpaRepository.findAll().toList().last()
        assertThat(outboxEvent.eventType).isEqualTo(EventType.ORDER_PAID.type)
        assertThat(outboxEvent.status).isEqualTo(OutboxStatus.PENDING)
    }

    @Test
    fun `update order status with invalid transition returns error and does not persist`() {
        val createdOrderId = createOrderPayload()
        val payload =
            """
            {
              "status": "SHIPPED"
            }
            """.trimIndent()

        webTestClient
            .patch()
            .uri("/api/v1/orders/$createdOrderId/status")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .is4xxClientError

        val savedOrder = orderJpaRepository.findById(UUID.fromString(createdOrderId)).orElseThrow()
        assertThat(savedOrder.status).isEqualTo(OrderStatus.CREATED)
        assertThat(outboxEventJpaRepository.count()).isEqualTo(1)
    }

    private fun createOrderPayload(): String {
        val createOrderPayload =
            """
            {
              "store_id": "store-1",
              "items": [
                {
                  "product_name": "Product A",
                  "quantity": 2,
                  "unit_price": 9.95,
                  "discount": 1.00,
                  "tax": 0.50
                }
              ]
            }
            """.trimIndent()
        return createOrderAndGetId(createOrderPayload)
    }

    private fun createOrderAndGetId(payload: String): String = webTestClient
        .post()
        .uri("/api/v1/orders")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(payload)
        .exchange()
        .expectStatus()
        .isCreated
        .expectBody<String>()
        .returnResult()
        .responseBody
        ?.let { JsonPath.read(it, "$.id") }
        ?: error("Order ID not found")
}
