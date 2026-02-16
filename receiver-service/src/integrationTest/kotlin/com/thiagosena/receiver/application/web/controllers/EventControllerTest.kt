package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.application.config.TestcontainersConfiguration
import com.thiagosena.receiver.ReceiverApplication
import com.thiagosena.receiver.domain.entities.EventStatus
import com.thiagosena.receiver.resources.repositories.jpa.EventJpaRepository
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = [ReceiverApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration::class)
class EventControllerTest(@param:Autowired val eventJpaRepository: EventJpaRepository) {
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
        eventJpaRepository.deleteAll()
    }

    @Test
    fun `should receive and process event successfully`() {
        val request = """
            {
                "idempotency_key": "test-key-001",
                "event_type": "order.created",
                "order_id": "order-123",
                "store_id": "store-456",
                "created_at": "2024-01-15T10:30:00"
            }
        """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk

        val savedEvent = eventJpaRepository.findAll().first()
        assertThat(savedEvent.idempotencyKey).isEqualTo("test-key-001")
        assertThat(savedEvent.eventType).isEqualTo("order.created")
        assertThat(savedEvent.orderId).isEqualTo("order-123")
        assertThat(savedEvent.storeId).isEqualTo("store-456")
        assertThat(savedEvent.status).isEqualTo(EventStatus.SNAPSHOT_PENDING)
    }

    @Test
    fun `should not duplicate event with same idempotency key`() {
        val request = """
            {
                "idempotency_key": "duplicate-key",
                "event_type": "order.updated",
                "order_id": "order-789",
                "store_id": "store-101",
                "created_at": "2024-01-15T11:00:00"
            }
        """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk

        webTestClient
            .post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk

        val events = eventJpaRepository.findAll()
        assertThat(events).hasSize(1)
        assertThat(events.first().idempotencyKey).isEqualTo("duplicate-key")
    }

    @Test
    fun `should handle multiple different events`() {
        val request1 = """
            {
                "idempotency_key": "key-001",
                "event_type": "order.created",
                "order_id": "order-001",
                "store_id": "store-001",
                "created_at": "2024-01-15T10:00:00"
            }
        """.trimIndent()

        val request2 = """
            {
                "idempotency_key": "key-002",
                "event_type": "order.updated",
                "order_id": "order-002",
                "store_id": "store-002",
                "created_at": "2024-01-15T11:00:00"
            }
        """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request1)
            .exchange()
            .expectStatus().isOk

        webTestClient
            .post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(request2)
            .exchange()
            .expectStatus().isOk

        val events = eventJpaRepository.findAll()
        assertThat(events).hasSize(2)
        assertThat(events.map { it.idempotencyKey }).containsExactlyInAnyOrder("key-001", "key-002")
    }

    @Test
    fun `should return bad request when required field is missing`() {
        val invalidRequest = """
            {
                "event_type": "order.created",
                "order_id": "order-123",
                "store_id": "store-456"
            }
        """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/events")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest
    }

    @Test
    fun `should handle different event types`() {
        val eventTypes = listOf("order.created", "order.updated", "order.cancelled", "order.completed")

        eventTypes.forEachIndexed { index, eventType ->
            val request = """
                {
                    "idempotency_key": "key-$index",
                    "event_type": "$eventType",
                    "order_id": "order-$index",
                    "store_id": "store-$index",
                    "created_at": "2024-01-15T10:00:00"
                }
            """.trimIndent()

            webTestClient
                .post()
                .uri("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk
        }

        val events = eventJpaRepository.findAll()
        assertThat(events).hasSize(eventTypes.size)
        assertThat(events.map { it.eventType }).containsExactlyInAnyOrderElementsOf(eventTypes)
    }
}
