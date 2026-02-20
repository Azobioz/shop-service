package com.example.module

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val totalPrice: String,
    val items: List<OrderItem>,
    val createdAt: String
)

@Serializable
enum class OrderStatus {
    PENDING, COMPLETED, CANCELLED
}

@Serializable
data class OrderItem(
    val id: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val price: String
)

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>
)

@Serializable
data class OrderItemRequest(
    val productId: Long,
    val quantity: Int
)
