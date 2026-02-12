package com.thiagosena.marketplace.domain.repositories

import com.thiagosena.marketplace.domain.entities.Order
import java.util.*

interface OrderRepository {
    fun save(order: Order): Order

    fun findById(id: UUID): Order?
}
