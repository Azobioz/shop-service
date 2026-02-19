package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemRequest(
    val productId: Int,
    val quantity: Int
)

@Serializable
data class OrderRequest(
    val items: List<OrderItemRequest>
)
