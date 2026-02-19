package com.example.service

import com.example.domain.*
import com.example.dto.OrderItemResponse
import com.example.dto.OrderRequest
import com.example.dto.OrderResponse
import com.example.messaging.OrderEventProducer
import com.example.repository.AuditLogRepository
import com.example.repository.OrderRepository
import com.example.repository.ProductRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val auditLogRepository: AuditLogRepository,
    private val eventProducer: OrderEventProducer
) {
    suspend fun createOrder(userId: Int, request: OrderRequest): OrderResponse {
        // Проверяем наличие товаров и резервируем
        val items = mutableListOf<OrderItem>()
        var total = BigDecimal.ZERO

        for (itemReq in request.items) {
            val product = productRepository.findById(itemReq.productId)
                ?: throw IllegalArgumentException("Product not found: ${itemReq.productId}")
            if (product.stock < itemReq.quantity) {
                throw IllegalArgumentException("Insufficient stock for product: ${product.name}")
            }
            // Уменьшаем stock
            val newStock = product.stock - itemReq.quantity
            productRepository.updateStock(product.id!!, newStock)

            val item = OrderItem(
                productId = product.id,
                quantity = itemReq.quantity,
                price = product.price
            )
            items.add(item)
            total += product.price * BigDecimal(itemReq.quantity)
        }

        // Создаём заказ
        val order = Order(
            userId = userId,
            totalAmount = total,
            status = OrderStatus.PENDING,
            items = items
        )
        val createdOrder = orderRepository.create(order, items)

        // Пишем в audit log
        val audit = AuditLog(
            userId = userId,
            action = "CREATE_ORDER",
            entityType = "ORDER",
            entityId = createdOrder.id,
            details = "Order created with total $total"
        )
        auditLogRepository.create(audit)

        // Отправляем событие в Kafka
        eventProducer.sendOrderCreated(createdOrder.id!!, userId, total)

        return toResponse(createdOrder)
    }

    suspend fun getUserOrders(userId: Int, limit: Int, offset: Int): List<OrderResponse> {
        val orders = orderRepository.findByUser(userId, limit, offset)
        return orders.map { toResponse(it) }
    }

    suspend fun cancelOrder(orderId: Int, userId: Int, isAdmin: Boolean): Boolean {
        val order = orderRepository.findById(orderId)
            ?: throw IllegalArgumentException("Order not found")

        // Проверяем права: пользователь может отменить только свой заказ, админ любой
        if (!isAdmin && order.userId != userId) {
            throw SecurityException("Access denied")
        }

        if (order.status != OrderStatus.PENDING) {
            throw IllegalArgumentException("Only pending orders can be cancelled")
        }

        // Возвращаем товары на склад
        for (item in order.items) {
            val product = productRepository.findById(item.productId)
                ?: continue
            val newStock = product.stock + item.quantity
            productRepository.updateStock(product.id!!, newStock)
        }

        val updated = orderRepository.updateStatus(orderId, OrderStatus.CANCELLED)

        // Audit log
        val audit = AuditLog(
            userId = userId,
            action = "CANCEL_ORDER",
            entityType = "ORDER",
            entityId = orderId,
            details = "Order cancelled"
        )
        auditLogRepository.create(audit)

        return updated
    }

    private suspend fun toResponse(order: Order): OrderResponse {
        val items = order.items.map { item ->
            val product = productRepository.findById(item.productId)
            OrderItemResponse(
                productId = item.productId,
                productName = product?.name ?: "Unknown",
                quantity = item.quantity,
                price = item.price
            )
        }
        return OrderResponse(
            id = order.id!!,
            status = order.status.name,
            totalAmount = order.totalAmount,
            items = items
        )
    }
}