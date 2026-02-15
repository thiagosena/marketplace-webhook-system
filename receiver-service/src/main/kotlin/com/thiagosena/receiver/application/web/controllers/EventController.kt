package com.thiagosena.receiver.application.web.controllers

import com.thiagosena.receiver.application.web.controllers.requests.EventWebhookRequest
import com.thiagosena.receiver.domain.services.EventService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/events")
class EventController(private val eventService: EventService) {

    @PostMapping
    fun receiveEvent(@RequestBody eventWebhookRequest: EventWebhookRequest) {
        eventService.processEvent(eventWebhookRequest.toDomain())
    }
}
