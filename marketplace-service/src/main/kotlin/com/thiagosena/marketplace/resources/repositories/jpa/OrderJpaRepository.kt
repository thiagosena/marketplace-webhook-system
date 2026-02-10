package com.thiagosena.marketplace.resources.repositories.jpa

import com.thiagosena.marketplace.domain.entities.Order
import org.springframework.data.repository.CrudRepository

interface OrderJpaRepository : CrudRepository<Order, String> {
}