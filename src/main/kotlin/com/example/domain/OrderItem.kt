package com.example.domain

import kotlinx.serialization.Contextual
import java.math.BigDecimal

data class OrderItem(
    val id: Int? = null,
    val orderId: Int? = null,
    val productId: Int,
    val quantity: Int,
    @Contextual val price: BigDecimal
)