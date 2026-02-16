package com.thiagosena.marketplace.application.security

import com.thiagosena.marketplace.MarketplaceApplication
import com.thiagosena.marketplace.application.config.TestcontainersConfiguration
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
class SecurityTest {
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

    @Test
    fun `request without authorization header returns unauthorized`() {
        webTestClient
            .post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"store_id":"store-1","items":[]}""")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.error").isEqualTo("UNAUTHORIZED")
            .jsonPath("$.message").exists()
            .jsonPath("$.timestamp").exists()
    }

    @Test
    fun `request with invalid authorization header returns unauthorized`() {
        webTestClient
            .post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "invalid-token")
            .bodyValue("""{"store_id":"store-1","items":[]}""")
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `request with valid authorization header returns ok or appropriate status`() {
        webTestClient
            .post()
            .uri("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "test-secret")
            .bodyValue("""{"store_id":"store-1","items":[]}""")
            .exchange()
            .expectStatus()
            .is4xxClientError
    }

    @Test
    fun `actuator endpoints are accessible without authentication`() {
        webTestClient
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus()
            .isOk
    }
}
