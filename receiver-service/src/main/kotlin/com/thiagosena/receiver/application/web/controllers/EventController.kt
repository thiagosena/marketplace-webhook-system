package com.thiagosena.receiver.application.web.controllers

import com.thiagosena.receiver.application.web.controllers.requests.EventWebhookRequest
import com.thiagosena.receiver.domain.services.EventService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class EventController(private val eventService: EventService) {

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SERVICE')")
    fun receiveEvent(@RequestBody eventWebhookRequest: EventWebhookRequest): ResponseEntity<Unit> =
        eventService.processEvent(eventWebhookRequest.toDomain()).let {
            ResponseEntity.noContent().build()
        }
}
