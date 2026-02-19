package com.example.service

import com.example.cache.RedisCache
import com.example.domain.Product
import com.example.dto.ProductRequest
import com.example.dto.ProductResponse
import com.example.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ProductServiceTest {
    private val productRepository = mockk<ProductRepository>()
    private val cache = mockk<RedisCache>(relaxed = true)
    private val productService = ProductService(productRepository, cache)

    @Test
    fun `createProduct should create and return product response`() = runBlocking {
        val request = ProductRequest("Test", "Desc", BigDecimal.TEN, 5)
        val product = Product(id = 1, name = "Test", description = "Desc", price = BigDecimal.TEN, stock = 5)
        coEvery { productRepository.create(any()) } returns product

        val result = productService.createProduct(request, 1)

        assertEquals(1, result.id)
        assertEquals("Test", result.name)
        coVerify { cache.delete("products:all") }
    }

    @Test
    fun `getProduct should return cached product if exists`() = runBlocking {
        val cached = ProductResponse(1, "Test", "Desc", BigDecimal.TEN, 5)
        coEvery { cache.get<ProductResponse>(any()) } answers { cached }
        val result = productService.getProduct(1)
        assertEquals(cached, result)
        coVerify(exactly = 0) { productRepository.findById(any()) }
    }


    @Test
    fun `getProduct should fetch from repo if not cached`() = runBlocking {
        val product = Product(id = 1, name = "Test", description = "Desc", price = BigDecimal.TEN, stock = 5)
        coEvery { cache.get<ProductResponse>(any()) } returns null
        coEvery { productRepository.findById(1) } returns product
        coEvery { cache.set(any(), any(), any()) } returns Unit
        val result = productService.getProduct(1)
        assertEquals(1, result?.id)
        coVerify { cache.set("product:1", any(), 3600) }
    }
}