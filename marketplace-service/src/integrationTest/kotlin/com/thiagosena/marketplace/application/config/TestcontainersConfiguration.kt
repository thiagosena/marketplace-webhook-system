package com.thiagosena.marketplace.application.config

import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    private val logger = LoggerFactory.getLogger(TestcontainersConfiguration::class.java)

    @Bean
    @ServiceConnection
    fun postgreContainer(): PostgreSQLContainer<*> =
        PostgreSQLContainer(DockerImageName.parse("postgres:16"))
            .also {
                logger.info("Starting PostgreSQL container: {}", it)
            }
}
