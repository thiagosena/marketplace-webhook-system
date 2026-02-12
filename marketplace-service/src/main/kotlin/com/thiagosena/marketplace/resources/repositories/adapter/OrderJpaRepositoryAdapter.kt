package com.thiagosena.marketplace.resources.repositories.adapter

import com.thiagosena.marketplace.domain.entities.Order
import com.thiagosena.marketplace.domain.repositories.OrderRepository
import com.thiagosena.marketplace.resources.repositories.jpa.OrderJpaRepository
import java.util.*
import org.springframework.stereotype.Repository

@Repository
class OrderJpaRepositoryAdapter(private val orderJpaRepository: OrderJpaRepository) : OrderRepository {
    override fun save(order: Order): Order = orderJpaRepository.save(order)

    override fun findById(id: UUID): Order? = orderJpaRepository.findById(id).orElse(null)
}
