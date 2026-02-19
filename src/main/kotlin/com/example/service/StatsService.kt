package com.example.service

import com.example.dto.OrdersStatsResponse
import com.example.repository.OrderRepository
import java.math.BigDecimal
import java.math.RoundingMode

class StatsService(
    private val orderRepository: OrderRepository
) {
    suspend fun getOrdersStats(): OrdersStatsResponse {
        val countByStatus = orderRepository.countByStatus()
        val totalOrders = countByStatus.values.sum()
        val totalRevenue = orderRepository.totalRevenue()
        val averageOrderValue = if (totalOrders > 0) {
            totalRevenue.divide(BigDecimal(totalOrders), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        return OrdersStatsResponse(
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            averageOrderValue = averageOrderValue,
            ordersByStatus = countByStatus.mapKeys { it.key.name }
        )
    }
}