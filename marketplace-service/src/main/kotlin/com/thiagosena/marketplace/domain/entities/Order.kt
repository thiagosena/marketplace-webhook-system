package com.thiagosena.marketplace.domain.entities

import com.thiagosena.marketplace.application.web.controllers.responses.CreateOrderResponse
import com.thiagosena.marketplace.application.web.controllers.responses.OrderItemResponse
import jakarta.persistence.*
import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
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
    @Fetch(FetchMode.SELECT)
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

    override fun toString() = """
        Order(
            id=$id, 
            storeId=$storeId, 
            status=$status, 
            totalAmount=$totalAmount
            items=$items
            createdAt=$createdAt
            updatedAt=$updatedAt
        )
    """.trimIndent()

    fun toResponse(): CreateOrderResponse = id?.let {
        CreateOrderResponse(
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
            })
    } ?: error("Order ID cannot be null when converting to response")
}

enum class OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    COMPLETED,
    CANCELED
}