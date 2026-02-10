package com.thiagosena.marketplace

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MarketplaceServiceApplication

fun main(args: Array<String>) {
    runApplication<MarketplaceServiceApplication>(*args)
}