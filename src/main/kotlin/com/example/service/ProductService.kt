package com.example.service

import kotlinx.serialization.json.Json
import com.example.data.repository.ProductRepository
import com.example.module.Product
import java.math.BigDecimal

class ProductService(
    private val productRepository: ProductRepository,
    private val cacheService: CacheService
) {

    fun getProduct(id: Long): Product? {
        // Проверяем кеш
        val cacheKey = "product:$id"
        val cached = cacheService.get(cacheKey) { json ->
            Json.decodeFromString<Product>(json)
        }

        if (cached != null) {
            return cached
        }

        // Не нашли - идем в базу
        val product = productRepository.findById(id) ?: return null

        cacheService.setJson(cacheKey, product, 300)

        return product
    }

    fun getAllProducts(): List<Product> {
        return productRepository.findAll()
    }

    fun createProduct(name: String, description: String, price: String, stock: Int): Product {
        val priceDecimal = BigDecimal(price)
        return productRepository.create(name, description, priceDecimal, stock)
    }

    fun updateProduct(id: Long, name: String?, description: String?, price: String?, stock: Int?): Product? {
        val priceDecimal = price?.let { BigDecimal(it) }
        val updated = productRepository.update(id, name, description, priceDecimal, stock)

        // Инвалидируем кеш
        if (updated != null) {
            cacheService.delete("product:$id")
        }

        return updated
    }

    fun deleteProduct(id: Long): Result<Unit> {
        // Проверяем существует ли товар
        if (productRepository.findById(id) == null) {
            return Result.failure(Exception("Product not found"))
        }

        val deleted = productRepository.delete(id)

        if (deleted) {
            cacheService.delete("product:$id")
            return Result.success(Unit)
        } else {
            return Result.failure(Exception("Cannot delete product. It is referenced in existing orders"))
        }
    }
}