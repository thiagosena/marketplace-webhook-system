package com.thiagosena.marketplace.resources.repositories.jpa

import com.thiagosena.marketplace.domain.entities.OutboxEvent
import org.springframework.data.repository.CrudRepository

interface OutboxEventJpaRepository : CrudRepository<OutboxEvent, String>
