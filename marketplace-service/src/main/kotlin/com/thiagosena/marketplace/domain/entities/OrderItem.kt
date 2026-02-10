package com.thiagosena.marketplace.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import java.math.BigDecimal

@Embeddable
data class OrderItem(

    @Column(name = "product_name")
    val productName: String? = null,

    @Column(nullable = false)
    val quantity: Int,

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    val unitPrice: BigDecimal,

    @Column(name = "discount", precision = 10, scale = 2)
    val discount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tax", precision = 10, scale = 2)
    val tax: BigDecimal = BigDecimal.ZERO
)