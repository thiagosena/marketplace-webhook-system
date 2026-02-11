package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.MarketplaceApplication
import com.thiagosena.marketplace.application.config.TestcontainersConfiguration
import com.thiagosena.marketplace.domain.entities.EventType
import com.thiagosena.marketplace.domain.entities.OutboxStatus
import com.thiagosena.marketplace.resources.repositories.jpa.OrderJpaRepository
import com.thiagosena.marketplace.resources.repositories.jpa.OutboxEventJpaRepository
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient


@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = [MarketplaceApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration::class)
class OrderControllerTest(
    @Autowired val orderJpaRepository: OrderJpaRepository,
    @Autowired val outboxEventJpaRepository: OutboxEventJpaRepository,
    @Autowired val jdbcTemplate: JdbcTemplate
) {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setupWebClient() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @AfterEach
    fun cleanup() {
        jdbcTemplate.execute("DELETE FROM order_items")
        outboxEventJpaRepository.deleteAll()
        orderJpaRepository.deleteAll()
    }

    @Test
    fun `create order returns ok and persists order and outbox event`() {
        val payload = """
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
        webTestClient.post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus().isCreated

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
        val payload = """
            {
              "store_id": "",
              "items": []
            }
        """.trimIndent()
        webTestClient.post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus().isBadRequest

        assertThat(orderJpaRepository.count()).isEqualTo(0)
        assertThat(outboxEventJpaRepository.count()).isEqualTo(0)
    }
}