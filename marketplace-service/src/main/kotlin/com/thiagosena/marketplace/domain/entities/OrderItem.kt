package com.thiagosena.marketplace.domain.entities

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.*

@Entity
@Table(name = "order_items")
data class OrderItem(
    @Id
    @GeneratedValue
    val id: UUID? = null,
    @Column(name = "product_name", nullable = false)
    val productName: String,
    @Column(name = "quantity", nullable = false)
    val quantity: Int,
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    val unitPrice: BigDecimal,
    @Column(name = "discount", nullable = false, precision = 10, scale = 2)
    val discount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "tax", nullable = false, precision = 10, scale = 2)
    val tax: BigDecimal = BigDecimal.ZERO,
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = ForeignKey(name = "FK_ORDER_ID"))
    val order: Order
) {
    override fun toString() =
        """
        OrderItem(
            id=$id, 
            productName=$productName, 
            quantity=$quantity, 
            unitPrice=$unitPrice, 
            discount=$discount, 
            tax=$tax
        )
        """.trimIndent()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OrderItem

        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
