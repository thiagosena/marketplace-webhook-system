package com.thiagosena.marketplace.domain.entities

import com.thiagosena.marketplace.domain.entities.EventType.ORDER_CANCELED
import com.thiagosena.marketplace.domain.entities.EventType.ORDER_COMPLETED
import com.thiagosena.marketplace.domain.entities.EventType.ORDER_CREATED
import com.thiagosena.marketplace.domain.entities.EventType.ORDER_PAID
import com.thiagosena.marketplace.domain.entities.EventType.ORDER_SHIPPED
import com.thiagosena.marketplace.domain.entities.OrderStatus.CANCELED
import com.thiagosena.marketplace.domain.entities.OrderStatus.COMPLETED
import com.thiagosena.marketplace.domain.entities.OrderStatus.CREATED
import com.thiagosena.marketplace.domain.entities.OrderStatus.PAID
import com.thiagosena.marketplace.domain.entities.OrderStatus.SHIPPED
import com.thiagosena.marketplace.domain.responses.OrderItemResponse
import com.thiagosena.marketplace.domain.responses.OrderResponse
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "orders")
data class Order(
    @Id
    @GeneratedValue
    val id: UUID? = null,
    @Column(name = "store_id", nullable = false)
    val storeId: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    val status: OrderStatus = OrderStatus.CREATED,
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    val totalAmount: BigDecimal,
    @OneToMany(
        mappedBy = "order",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE],
        orphanRemoval = true
    )
    val items: MutableList<OrderItem> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null
) {
    fun addItem(item: OrderItem) {
        items.add(item)
    }

    fun addItems(newItems: Iterable<OrderItem>) {
        newItems.forEach { addItem(it) }
    }

    override fun toString() =
        """
        Order(
            id=$id, 
            storeId=$storeId, 
            status=$status, 
            totalAmount=$totalAmount
            createdAt=$createdAt
            updatedAt=$updatedAt
            items=$items
        )
        """.trimIndent()

    fun toResponse(): OrderResponse = id?.let {
        OrderResponse(
            it.toString(),
            storeId,
            items.map { item ->
                OrderItemResponse(
                    productName = item.productName,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    discount = item.discount,
                    tax = item.tax
                )
            }
        )
    } ?: error("Order ID cannot be null when converting to response")

    fun canTransitionTo(newStatus: OrderStatus): Boolean = when (status) {
        CREATED -> newStatus in listOf(PAID, CANCELED)
        PAID -> newStatus in listOf(SHIPPED, CANCELED)
        SHIPPED -> newStatus == COMPLETED
        else -> false
    }

    fun toEventTypeEnum(): EventType = when (status) {
        CREATED -> ORDER_CREATED
        PAID -> ORDER_PAID
        SHIPPED -> ORDER_SHIPPED
        COMPLETED -> ORDER_COMPLETED
        CANCELED -> ORDER_CANCELED
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Order

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}

enum class OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED
}
