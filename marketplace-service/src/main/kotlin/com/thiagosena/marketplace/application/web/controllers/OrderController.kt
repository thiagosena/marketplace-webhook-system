package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.application.web.controllers.requests.CreateOrderRequest
import com.thiagosena.marketplace.application.web.controllers.responses.OrderResponse
import com.thiagosena.marketplace.domain.services.OrderService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(
    private val orderService: OrderService,
) {
    @PostMapping
    fun createOrder(
        @Valid @RequestBody createOrderRequest: CreateOrderRequest,
    ): ResponseEntity<OrderResponse> =
        orderService.createOrder(createOrderRequest.toDomain()).let { order ->
            ResponseEntity.status(HttpStatus.CREATED).body(order)
        }

    @GetMapping("/{orderId}")
    fun getOrderById(
        @PathVariable orderId: UUID,
    ): ResponseEntity<OrderResponse> = ResponseEntity.ok(orderService.findById(orderId))
}
