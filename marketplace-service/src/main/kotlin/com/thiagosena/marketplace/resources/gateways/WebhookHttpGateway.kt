package com.thiagosena.marketplace.resources.gateways

import com.thiagosena.marketplace.domain.config.WebhookProperties
import com.thiagosena.marketplace.domain.gateways.WebhookHttpGateway
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import java.time.Duration
import java.util.concurrent.TimeUnit
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class WebhookHttpGateway(
    private val webClientBuilder: WebClient.Builder,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val retryRegistry: RetryRegistry,
    private val webhookProperties: WebhookProperties
) : WebhookHttpGateway {

    override fun send(url: String, payload: String) {
        val timeoutValueInSeconds = Duration.ofSeconds(webhookProperties.timeoutInSeconds)

        retryRegistry.retry(WEBHOOK_NAME).executeSupplier {
            circuitBreakerRegistry.circuitBreaker(WEBHOOK_NAME).executeSupplier {
                webClientBuilder.build().post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono<String>()
                    .toFuture()
                    .get(timeoutValueInSeconds.toMillis(), TimeUnit.MILLISECONDS)
            }
        }
    }

    companion object {
        const val WEBHOOK_NAME = "webhook"
    }
}
