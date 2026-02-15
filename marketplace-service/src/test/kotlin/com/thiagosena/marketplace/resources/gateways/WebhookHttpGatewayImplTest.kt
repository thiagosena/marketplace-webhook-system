package com.thiagosena.marketplace.resources.gateways

import com.thiagosena.marketplace.domain.config.WebhookProperties
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
        val gateway =
            WebhookHttpGatewayImpl(
                webClientBuilder = webClientBuilder,
                webhookProperties = webhookProperties
            )

        gateway.send(url, payload)

        verify(exactly = 1) { future.get(2000, TimeUnit.MILLISECONDS) }
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
        every { requestBodySpec.bodyValue(payload) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono<String>() } returns mono
        every { mono.toFuture() } returns future

        return webClientBuilder
    }
}
