package com.example.module

import kotlinx.serialization.Serializable

@Serializable
data class Product(
    val id: Long,
    val name: String,
    val description: String,
    val price: String, // Используем String для BigDecimal в JSON
    val stock: Int,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateProductRequest(
    val name: String,
    val description: String,
    val price: String,
    val stock: Int
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val description: String? = null,
    val price: String? = null,
    val stock: Int? = null
)