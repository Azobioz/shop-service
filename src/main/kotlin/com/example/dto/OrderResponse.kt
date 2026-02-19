package com.example.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OrderResponse(
    val id: Int,
    val status: String,
    @Contextual val totalAmount: BigDecimal,
    val items: List<OrderItemResponse>
)

@Serializable
data class OrderItemResponse(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    @Contextual val price: BigDecimal
)