package com.thiagosena.marketplace.domain.entities

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue
    val id: UUID? = null,
    @Column(name = "store_id")
    val storeId: String,
    @Column(name = "status")
    val status: OrderStatus,
    @Column(name = "total_amount")
    val totalAmount: BigDecimal,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", columnDefinition = "jsonb")
    val items: List<OrderItem>,

    @Column(name = "created_at", updatable = false)
    val createAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null
)

enum class OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED
}