package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.application.web.controllers.requests.CreateOrderRequest
import com.thiagosena.marketplace.domain.services.OrderService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/orders")
class OrderController(
    private val orderService: OrderService
) {
    @PostMapping
    fun createOrder(
        @Valid @RequestBody createOrderRequest: CreateOrderRequest
    ) {
        orderService.createOrder(createOrderRequest.toDomain())
    }
}