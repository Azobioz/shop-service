package com.example.service

import com.example.domain.*
import com.example.dto.OrderItemRequest
import com.example.dto.OrderRequest
import com.example.messaging.OrderEventProducer
import com.example.repository.AuditLogRepository
import com.example.repository.OrderRepository
import com.example.repository.ProductRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderServiceTest {
    private val orderRepository = mockk<OrderRepository>()
    private val productRepository = mockk<ProductRepository>()
    private val auditLogRepository = mockk<AuditLogRepository>()
    private val eventProducer = mockk<OrderEventProducer>(relaxed = true)
    private val orderService = OrderService(orderRepository, productRepository, auditLogRepository, eventProducer)

    @Test
    fun `createOrder should throw if product not found`() = runBlocking<Unit> {
        val request = OrderRequest(listOf(OrderItemRequest(1, 2)))
        coEvery { productRepository.findById(1) } returns null

        assertThrows<IllegalArgumentException> {
            orderService.createOrder(1, request)
        }
    }

    @Test
    fun `createOrder should throw if insufficient stock`() = runBlocking<Unit> {
        val product = Product(id = 1, name = "Test", description = null, price = BigDecimal.TEN, stock = 1)
        val request = OrderRequest(listOf(OrderItemRequest(1, 2)))
        coEvery { productRepository.findById(1) } returns product

        assertThrows<IllegalArgumentException> {
            orderService.createOrder(1, request)
        }
    }

    @Test
    fun `createOrder should create order and update stock`() = runBlocking {
        val product = Product(id = 1, name = "Test", description = null, price = BigDecimal.TEN, stock = 5)
        val request = OrderRequest(listOf(OrderItemRequest(1, 2)))
        val order = Order(id = 10, userId = 1, totalAmount = BigDecimal.valueOf(20), status = OrderStatus.PENDING, items = listOf(
            OrderItem(productId = 1, quantity = 2, price = BigDecimal.TEN)
        ))

        coEvery { productRepository.findById(1) } returns product
        coEvery { productRepository.updateStock(1, 3) } returns true
        coEvery { orderRepository.create(any(), any()) } returns order
        coEvery { auditLogRepository.create(any()) } returns mockk()

        val result = orderService.createOrder(1, request)

        assertEquals(10, result.id)
        assertEquals(BigDecimal.valueOf(20), result.totalAmount)
        coVerify { productRepository.updateStock(1, 3) }
        coVerify { eventProducer.sendOrderCreated(10, 1, BigDecimal.valueOf(20)) }
    }
}