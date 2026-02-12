package com.thiagosena.marketplace.domain.repositories

import com.thiagosena.marketplace.domain.entities.Order

interface OrderRepository {
    fun save(order: Order): Order
}
