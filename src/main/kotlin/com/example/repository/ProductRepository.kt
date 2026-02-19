package com.example.repository

import com.example.domain.Product

interface ProductRepository {
    suspend fun create(product: Product): Product
    suspend fun findById(id: Int): Product?
    suspend fun findAll(limit: Int, offset: Int): List<Product>
    suspend fun update(product: Product): Product
    suspend fun delete(id: Int): Boolean
    suspend fun updateStock(id: Int, newStock: Int): Boolean
}