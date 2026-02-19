package com.example.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class OrdersStatsResponse(
    val totalOrders: Long,
    @Contextual val totalRevenue: BigDecimal,
    @Contextual val averageOrderValue: BigDecimal,
    val ordersByStatus: Map<String, Long>
)