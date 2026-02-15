package com.thiagosena.marketplace.application.web.controllers

import com.thiagosena.marketplace.application.web.controllers.requests.CreateOrderRequest
import com.thiagosena.marketplace.application.web.controllers.requests.UpdateOrderStatusRequest
import com.thiagosena.marketplace.domain.entities.responses.OrderResponse
import com.thiagosena.marketplace.domain.services.OrderService
import jakarta.validation.Valid
import java.util.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/orders")
class OrderController(private val orderService: OrderService) {
    @PostMapping
    fun createOrder(@Valid @RequestBody createOrderRequest: CreateOrderRequest): ResponseEntity<OrderResponse> =
        orderService.createOrder(createOrderRequest.toDomain()).let { orderResponse ->
            ResponseEntity.status(HttpStatus.CREATED).body(orderResponse)
        }

    @GetMapping("/{orderId}")
    fun getOrderById(@PathVariable orderId: UUID): ResponseEntity<OrderResponse> =
        ResponseEntity.ok(orderService.findById(orderId))

    @PatchMapping("/{orderId}/status")
    fun updateOrderStatus(
        @PathVariable orderId: UUID,
        @RequestBody updateOrderStatusRequest: UpdateOrderStatusRequest
    ): ResponseEntity<OrderResponse> =
        orderService.updateOrderStatusById(orderId, updateOrderStatusRequest.toDomain()).let {
            ResponseEntity.ok(it)
        }
}
