package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.application.web.controllers.requests.CreateWebhookRequest
import com.thiagosena.marketplace.domain.entities.responses.WebhookResponse
import com.thiagosena.marketplace.domain.services.WebhookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/webhooks")
class WebhookController(private val webhookService: WebhookService) {
    @PostMapping
    fun createWebhook(@Valid @RequestBody createWebhookRequest: CreateWebhookRequest): ResponseEntity<WebhookResponse> =
        webhookService.createWebhook(createWebhookRequest.toDomain()).let { webhookResponse ->
            ResponseEntity.status(HttpStatus.CREATED).body(webhookResponse)
        }
}
