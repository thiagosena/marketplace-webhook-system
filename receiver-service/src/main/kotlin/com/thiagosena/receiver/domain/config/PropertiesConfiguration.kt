package com.thiagosena.receiver.domain.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
    SnapshotEventProperties::class
)
class PropertiesConfiguration

@ConfigurationProperties(prefix = "app.snapshot-event")
data class SnapshotEventProperties(
    val maxRetries: Int,
    val batchSize: Int,
    val baseDelaySeconds: Int,
    val maxDelaySeconds: Int,
    val maxJitterSeconds: Int
)
