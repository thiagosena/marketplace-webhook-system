package com.thiagosena.marketplace.domain.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
    OutboxEventProperties::class,
    WebhookProperties::class,
    AppConfigProperties::class
)
class PropertiesConfiguration

@ConfigurationProperties(prefix = "app.outbox-event")
data class OutboxEventProperties(
    val maxRetries: Int,
    val batchSize: Int,
    val baseDelaySeconds: Int,
    val maxDelaySeconds: Int,
    val maxJitterSeconds: Int
)

@ConfigurationProperties(prefix = "app.webhook")
data class WebhookProperties(val timeoutInSeconds: Long)

@ConfigurationProperties(prefix = "app.config.service")
data class AppConfigProperties(val sharedSecret: String)
