package com.thiagosena.marketplace.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration(proxyBeanMethods = false)
class WebClientConfig {
    @Bean
    fun webClientBuilder(): WebClient.Builder = WebClient.builder()
}
