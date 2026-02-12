package com.thiagosena.marketplace.resources.repositories.jpa

import com.thiagosena.marketplace.domain.entities.Order
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface OrderJpaRepository : CrudRepository<Order, UUID>
