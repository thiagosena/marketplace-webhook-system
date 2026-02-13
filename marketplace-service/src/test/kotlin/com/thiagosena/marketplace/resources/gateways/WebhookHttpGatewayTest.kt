package com.thiagosena.marketplace.resources.gateways

import com.thiagosena.marketplace.domain.config.WebhookProperties
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

class WebhookHttpGatewayTest {
    @Test
    fun `given a successful request, it should send payload with configured timeout`() {
        val url = "https://example.com/webhook"
        val payload = """{"id": "123"}"""
        val webhookProperties = WebhookProperties(timeoutInSeconds = 2)

        val future = mockk<CompletableFuture<String?>>()
        every { future.get(any<Long>(), any()) } returns "ok"

        val webClientBuilder = mockWebClientChain(url, payload, future)
        val gateway =
            WebhookHttpGateway(
                webClientBuilder = webClientBuilder,
                circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults(),
                retryRegistry = RetryRegistry.ofDefaults(),
                webhookProperties = webhookProperties
            )

        gateway.send(url, payload)

        verify(exactly = 1) { future.get(2000, TimeUnit.MILLISECONDS) }
    }

    @Test
    fun `should retry on failure and eventually succeed`() {
        val url = "https://example.com/webhook"
        val payload = """{"id": "retry"}"""
        val webhookProperties = WebhookProperties(timeoutInSeconds = 1)

        val future = mockk<CompletableFuture<String?>>()
        var attempts = 0
        every { future.get(any<Long>(), any()) } answers {
            attempts += 1
            if (attempts < 3) error("boom")
            "ok"
        }

        val retryConfig =
            RetryConfig.custom<Any>()
                .maxAttempts(3)
                .waitDuration(Duration.ZERO)
                .build()
        val retryRegistry = RetryRegistry.of(retryConfig)

        val webClientBuilder = mockWebClientChain(url, payload, future)
        val gateway =
            WebhookHttpGateway(
                webClientBuilder = webClientBuilder,
                circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults(),
                retryRegistry = retryRegistry,
                webhookProperties = webhookProperties
            )

        gateway.send(url, payload)

        verify(exactly = 3) { future.get(any<Long>(), any()) }
    }

    @Test
    fun `should throw when retry is exhausted`() {
        val url = "https://example.com/webhook"
        val payload = """{"id": "fail"}"""
        val webhookProperties = WebhookProperties(timeoutInSeconds = 1)

        val future = mockk<CompletableFuture<String?>>()
        every { future.get(any<Long>(), any()) } throws RuntimeException("boom")

        val retryConfig =
            RetryConfig.custom<Any>()
                .maxAttempts(2)
                .waitDuration(Duration.ZERO)
                .build()
        val retryRegistry = RetryRegistry.of(retryConfig)

        val webClientBuilder = mockWebClientChain(url, payload, future)
        val gateway =
            WebhookHttpGateway(
                webClientBuilder = webClientBuilder,
                circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults(),
                retryRegistry = retryRegistry,
                webhookProperties = webhookProperties
            )

        assertThrows(RuntimeException::class.java) {
            gateway.send(url, payload)
        }

        verify(exactly = 2) { future.get(any<Long>(), any()) }
    }

    @Test
    fun `should throw when circuit breaker is open`() {
        val url = "https://example.com/webhook"
        val payload = """{"id": "cb-open"}"""
        val webhookProperties = WebhookProperties(timeoutInSeconds = 1)

        val future = mockk<CompletableFuture<String?>>()
        every { future.get(any<Long>(), any()) } returns "ok"

        val webClientBuilder = mockWebClientChain(url, payload, future)
        val circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults()
        circuitBreakerRegistry.circuitBreaker(WebhookHttpGateway.WEBHOOK_NAME).transitionToOpenState()
        val retryRegistry =
            RetryRegistry.of(
                RetryConfig.custom<Any>()
                    .maxAttempts(1)
                    .waitDuration(Duration.ZERO)
                    .build()
            )

        val gateway =
            WebhookHttpGateway(
                webClientBuilder = webClientBuilder,
                circuitBreakerRegistry = circuitBreakerRegistry,
                retryRegistry = retryRegistry,
                webhookProperties = webhookProperties
            )

        assertThrows(CallNotPermittedException::class.java) {
            gateway.send(url, payload)
        }

        verify(exactly = 0) { webClientBuilder.build() }
    }

    @Test
    fun `should open circuit breaker after consecutive failures`() {
        val url = "https://example.com/webhook"
        val payload = """{"id": "cb-fail"}"""
        val webhookProperties = WebhookProperties(timeoutInSeconds = 1)

        val future = mockk<CompletableFuture<String?>>()
        every { future.get(any<Long>(), any()) } throws TimeoutException("timeout")

        val circuitBreakerConfig =
            CircuitBreakerConfig.custom()
                .slidingWindowSize(2)
                .minimumNumberOfCalls(2)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build()
        val circuitBreakerRegistry = CircuitBreakerRegistry.of(circuitBreakerConfig)

        val retryConfig =
            RetryConfig.custom<Any>()
                .maxAttempts(1)
                .waitDuration(Duration.ZERO)
                .build()
        val retryRegistry = RetryRegistry.of(retryConfig)

        val webClientBuilder = mockWebClientChain(url, payload, future)
        val gateway =
            WebhookHttpGateway(
                webClientBuilder = webClientBuilder,
                circuitBreakerRegistry = circuitBreakerRegistry,
                retryRegistry = retryRegistry,
                webhookProperties = webhookProperties
            )

        repeat(2) {
            assertThrows(TimeoutException::class.java) {
                gateway.send(url, payload)
            }
        }

        val circuitBreaker = circuitBreakerRegistry.circuitBreaker(WebhookHttpGateway.WEBHOOK_NAME)
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.state)
    }

    private fun mockWebClientChain(
        url: String = "https://example.com/webhook",
        payload: String,
        future: CompletableFuture<String?>
    ): WebClient.Builder {
        val webClientBuilder = mockk<WebClient.Builder>()
        val webClient = mockk<WebClient>()
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val requestHeadersSpec = mockk<WebClient.RequestHeadersSpec<*>>()
        val responseSpec = mockk<WebClient.ResponseSpec>()
        val mono = mockk<Mono<String>>()

        every { webClientBuilder.build() } returns webClient
        every { webClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(url) } returns requestBodySpec
        every { requestBodySpec.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE) } returns
            requestBodySpec
        every { requestBodySpec.bodyValue(payload) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono<String>() } returns mono
        every { mono.toFuture() } returns future

        return webClientBuilder
    }
}
