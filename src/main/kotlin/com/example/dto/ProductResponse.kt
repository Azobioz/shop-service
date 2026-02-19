package com.example.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ProductResponse(
    val id: Int,
    val name: String,
    val description: String?,
    @Contextual val price: BigDecimal,
    val stock: Int
)