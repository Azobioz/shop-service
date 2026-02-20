package com.example.service

import com.example.data.repository.AuditLogRepository
import com.example.data.repository.OrderRepository
import com.example.data.repository.ProductRepository
import com.example.kafka.KafkaProducer
import com.example.kafka.OrderEvent
import com.example.module.CreateOrderRequest
import com.example.module.Order
import org.jetbrains.exposed.sql.transactions.transaction

class OrderService(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val auditLogRepository: AuditLogRepository,
    private val kafkaProducer: KafkaProducer,
    private val cacheService: CacheService
) {

    fun createOrder(userId: Long, request: CreateOrderRequest): Result<Order> = transaction {
        try {
            // Проверяем что все товары есть в наличии
            for (item in request.items) {
                val product = productRepository.findById(item.productId)
                    ?: return@transaction Result.failure(Exception("Product ${item.productId} not found"))

                if (product.stock < item.quantity) {
                    return@transaction Result.failure(Exception("Not enough stock for product ${product.name}"))
                }
            }

            // Списываем товары со склада
            for (item in request.items) {
                val success = productRepository.decreaseStock(item.productId, item.quantity)
                if (!success) {
                    return@transaction Result.failure(Exception("Failed to decrease stock"))
                }
                // Нужно сбросить кеш этого товара
                cacheService.delete("product:${item.productId}")
            }

            // Сохраняем заказ
            val order = orderRepository.create(userId, request.items)

            // Пишем в audit log
            auditLogRepository.log(
                userId = userId,
                action = "CREATE_ORDER",
                entityType = "ORDER",
                entityId = order.id,
                details = "Created order with ${request.items.size} items, total: ${order.totalPrice}"
            )

            // Кешируем заказ на 10 минут
            cacheService.setJson("order:${order.id}", order, 600)

            // Шлем событие в кафку
            kafkaProducer.sendOrderEvent(
                OrderEvent(
                    orderId = order.id,
                    userId = userId,
                    eventType = "ORDER_CREATED",
                    details = "Order created with total price ${order.totalPrice}"
                )
            )

            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOrder(id: Long, userId: Long): Order? {
        // Сначала проверяем кеш
        val cacheKey = "order:$id"
        val cached = cacheService.get(cacheKey) { json ->
            kotlinx.serialization.json.Json.decodeFromString<Order>(json)
        }

        if (cached != null) {
            // Проверяем, что заказ принадлежит пользователю
            if (cached.userId != userId) {
                return null
            }
            return cached
        }

        // Не нашли в кеше - идем в базу
        val order = orderRepository.findById(id) ?: return null

        // Проверяем, что заказ принадлежит пользователю
        if (order.userId != userId) {
            return null
        }

        // Кешируем заказ на 10 минут
        cacheService.setJson(cacheKey, order, 600)

        return order
    }

    fun getUserOrders(userId: Long): List<Order> {
        return orderRepository.findByUserId(userId)
    }

    fun cancelOrder(id: Long, userId: Long): Boolean = transaction {
        val cancelled = orderRepository.cancel(id, userId)

        if (cancelled) {
            // Сохраняем в audit
            auditLogRepository.log(
                userId = userId,
                action = "CANCEL_ORDER",
                entityType = "ORDER",
                entityId = id,
                details = "Order cancelled by user"
            )

            // Удаляем из кеша
            cacheService.delete("order:$id")

            // Уведомляем через кафку
            kafkaProducer.sendOrderEvent(
                OrderEvent(
                    orderId = id,
                    userId = userId,
                    eventType = "ORDER_CANCELLED",
                    details = "Order cancelled"
                )
            )
        }

        cancelled
    }
}