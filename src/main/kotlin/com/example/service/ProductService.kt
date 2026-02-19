package com.example.service

import com.example.cache.RedisCache
import com.example.domain.Product
import com.example.dto.ProductRequest
import com.example.dto.ProductResponse
import com.example.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductService(
    private val productRepository: ProductRepository,
    private val cache: RedisCache
) {
    suspend fun createProduct(request: ProductRequest, adminId: Int): ProductResponse {
        val product = Product(
            name = request.name,
            description = request.description,
            price = request.price,
            stock = request.stock
        )
        val created = productRepository.create(product)
        cache.delete("products:all")
        return toResponse(created)
    }

    suspend fun getProduct(id: Int): ProductResponse? {
        val cached = cache.get<ProductResponse>("product:$id")
        if (cached != null) return cached

        val product = productRepository.findById(id) ?: return null
        val response = toResponse(product)
        cache.set("product:$id", response, 3600)
        return response
    }

    suspend fun getAllProducts(limit: Int, offset: Int): List<ProductResponse> {
        val cacheKey = "products:all:$limit:$offset"
        val cached = cache.get<List<ProductResponse>>(cacheKey)
        if (cached != null) return cached

        val products = productRepository.findAll(limit, offset)
        val response = products.map { toResponse(it) }
        cache.set(cacheKey, response, 600)
        return response
    }

    suspend fun updateProduct(id: Int, request: ProductRequest, adminId: Int): ProductResponse? {
        val existing = productRepository.findById(id) ?: return null
        val updated = existing.copy(
            name = request.name,
            description = request.description,
            price = request.price,
            stock = request.stock
        )
        val result = productRepository.update(updated)
        withContext(Dispatchers.IO) {
            cache.delete("product:$id")
            cache.deleteByPattern("products:all:*")
        }
        return toResponse(result)
    }

    suspend fun deleteProduct(id: Int, adminId: Int): Boolean {
        val deleted = productRepository.delete(id)
        if (deleted) {
            withContext(Dispatchers.IO) {
                cache.delete("product:$id")
                cache.deleteByPattern("products:all:*")
            }
        }
        return deleted
    }

    private fun toResponse(product: Product): ProductResponse = ProductResponse(
        id = product.id!!,
        name = product.name,
        description = product.description,
        price = product.price,
        stock = product.stock
    )
}