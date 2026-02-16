package com.thiagosena.marketplace.resources.gateways

import com.thiagosena.marketplace.domain.config.WebhookProperties
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

class WebhookHttpGatewayImplTest {
    @Test
    fun `given a successful request, it should send payload with configured timeout`() {
        val url = "https://example.com/webhook"
        val payload = """{"id": "123"}"""
        val webhookProperties = WebhookProperties(timeoutInSeconds = 2)

        val future = mockk<CompletableFuture<String?>>()
        every { future.get(any<Long>(), any()) } returns "ok"

        val webClientBuilder = mockWebClientChain(url, payload, future)
        val circuitBreakerRegistry = mockCircuitBreakerRegistry()
        val retryRegistry = mockRetryRegistry()

        val gateway =
            WebhookHttpGatewayImpl(
                webClientBuilder = webClientBuilder,
                circuitBreakerRegistry = circuitBreakerRegistry,
                retryRegistry = retryRegistry,
                webhookProperties = webhookProperties
            )

        gateway.send(url, payload, "test-token-123")

        verify(exactly = 1) { future.get(2000, TimeUnit.MILLISECONDS) }
    }

    private fun mockCircuitBreakerRegistry(): CircuitBreakerRegistry {
        val registry = mockk<CircuitBreakerRegistry>()
        val circuitBreaker = mockk<CircuitBreaker>()

        every { registry.circuitBreaker(any()) } returns circuitBreaker
        every { circuitBreaker.executeSupplier<String>(any()) } answers {
            firstArg<java.util.function.Supplier<String>>().get()
        }

        return registry
    }

    private fun mockRetryRegistry(): RetryRegistry {
        val registry = mockk<RetryRegistry>()
        val retry = mockk<Retry>()

        every { registry.retry(any()) } returns retry
        every { retry.executeSupplier<String>(any()) } answers {
            firstArg<java.util.function.Supplier<String>>().get()
        }

        return registry
    }

    private fun mockWebClientChain(
        url: String,
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
        every { requestBodySpec.header(HttpHeaders.AUTHORIZATION, "test-token-123") } returns
            requestBodySpec
        every { requestBodySpec.bodyValue(payload) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono<String>() } returns mono
        every { mono.toFuture() } returns future

        return webClientBuilder
    }
}
