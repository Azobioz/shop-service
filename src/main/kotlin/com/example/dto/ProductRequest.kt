package com.example.dto

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ProductRequest(
    val name: String,
    val description: String?,
    @Contextual val price: BigDecimal,
    val stock: Int
)