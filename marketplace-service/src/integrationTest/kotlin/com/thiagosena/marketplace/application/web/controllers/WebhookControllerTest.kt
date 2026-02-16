package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.MarketplaceApplication
import com.thiagosena.marketplace.application.config.TestcontainersConfiguration
import com.thiagosena.marketplace.resources.repositories.jpa.WebhookJpaRepository
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
@SpringBootTest(webEnvironment = RANDOM_PORT, classes = [MarketplaceApplication::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Import(TestcontainersConfiguration::class)
class WebhookControllerTest(@param:Autowired val webhookJpaRepository: WebhookJpaRepository) {
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
        webhookJpaRepository.deleteAll()
    }

    @Test
    fun `create webhook returns created and persists`() {
        val payload =
            """
            {
              "store_ids": ["store-1", "store-2"],
              "callback_url": "https://example.com/webhooks",
              "token": "secret-token-123"
            }
            """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/webhooks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isCreated
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON).expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.store_ids[0]").isEqualTo("store-1")
            .jsonPath("$.store_ids[1]").isEqualTo("store-2")
            .jsonPath("$.callback_url").isEqualTo("https://example.com/webhooks")
            .jsonPath("$.active").isEqualTo(true)
            .jsonPath("$.created_at").exists()

        assertThat(webhookJpaRepository.count()).isEqualTo(1)
        val webhook = webhookJpaRepository.findAll().first()
        assertThat(webhook.storeIds).containsExactly("store-1", "store-2")
        assertThat(webhook.callbackUrl).isEqualTo("https://example.com/webhooks")
        assertThat(webhook.token).isEqualTo("secret-token-123")
        assertThat(webhook.active).isTrue()
    }

    @Test
    fun `create webhook with empty store ids returns bad request`() {
        val payload =
            """
            {
              "store_ids": [],
              "callback_url": "https://example.com/webhooks",
              "token": "secret-token-123"
            }
            """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/webhooks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isBadRequest

        assertThat(webhookJpaRepository.count()).isEqualTo(0)
    }

    @Test
    fun `create webhook with invalid callback url returns bad request`() {
        val payload =
            """
            {
              "store_ids": ["store-1"],
              "callback_url": "ftp://example.com/webhooks",
              "token": "secret-token-123"
            }
            """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/webhooks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isBadRequest

        assertThat(webhookJpaRepository.count()).isEqualTo(0)
    }

    @Test
    fun `create webhook with blank callback url returns bad request`() {
        val payload =
            """
            {
              "store_ids": ["store-1"],
              "callback_url": "",
              "token": "secret-token-123"
            }
            """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/webhooks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isBadRequest

        assertThat(webhookJpaRepository.count()).isEqualTo(0)
    }

    @Test
    fun `create webhook with blank token returns bad request`() {
        val payload =
            """
            {
              "store_ids": ["store-1"],
              "callback_url": "https://example.com/webhooks",
              "token": ""
            }
            """.trimIndent()

        webTestClient
            .post()
            .uri("/api/v1/webhooks")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(payload)
            .exchange()
            .expectStatus()
            .isBadRequest

        assertThat(webhookJpaRepository.count()).isEqualTo(0)
    }
}
