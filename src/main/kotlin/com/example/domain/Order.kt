package com.example.domain

import java.math.BigDecimal
import java.time.Instant

enum class OrderStatus {
    PENDING, PAID, SHIPPED, DELIVERED, CANCELLED
}

data class Order(
    val id: Int? = null,
    val userId: Int,
    val status: OrderStatus = OrderStatus.PENDING,
    val totalAmount: BigDecimal,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val items: List<OrderItem> = emptyList()
)