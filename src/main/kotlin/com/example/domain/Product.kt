package com.example.domain

import kotlinx.serialization.Contextual
import java.math.BigDecimal
import java.time.Instant

data class Product(
    val id: Int? = null,
    val name: String,
    val description: String?,
    @Contextual val price: BigDecimal,
    val stock: Int,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)