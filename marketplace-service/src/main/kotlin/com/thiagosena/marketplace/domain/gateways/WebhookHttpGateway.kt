package com.thiagosena.marketplace.domain.gateways

interface WebhookHttpGateway {
    fun send(url: String, payload: String, token: String)
}
