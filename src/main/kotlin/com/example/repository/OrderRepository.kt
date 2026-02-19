package com.example.repository

import com.example.domain.Order
import com.example.domain.OrderItem
import com.example.domain.OrderStatus

interface OrderRepository {
    suspend fun create(order: Order, items: List<OrderItem>): Order
    suspend fun findById(id: Int): Order?
    suspend fun findByUser(userId: Int, limit: Int, offset: Int): List<Order>
    suspend fun updateStatus(id: Int, status: OrderStatus): Boolean
    suspend fun delete(id: Int): Boolean
    suspend fun countByStatus(): Map<OrderStatus, Long>
    suspend fun totalRevenue(): java.math.BigDecimal
}