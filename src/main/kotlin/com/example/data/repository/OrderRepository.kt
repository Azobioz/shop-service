package com.example.data.repository

import com.example.data.table.OrderItems
import com.example.data.table.Orders
import com.example.data.table.Products
import com.example.module.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class OrderRepository {

    fun findAll(): List<Order> = transaction {
        Orders.selectAll()
            .map { orderRow ->
                val orderId = orderRow[Orders.id].value
                val items = (OrderItems innerJoin Products)
                    .selectAll().where { OrderItems.orderId eq orderId }
                    .map { row ->
                        OrderItem(
                            id = row[OrderItems.id].value,
                            productId = row[OrderItems.productId].value,
                            productName = row[Products.name],
                            quantity = row[OrderItems.quantity],
                            price = row[OrderItems.price].toString()
                        )
                    }

                Order(
                    id = orderId,
                    userId = orderRow[Orders.userId].value,
                    status = OrderStatus.valueOf(orderRow[Orders.status]),
                    totalPrice = orderRow[Orders.totalPrice].toString(),
                    items = items,
                    createdAt = orderRow[Orders.createdAt].toString()
                )
            }
    }

    fun create(userId: Long, items: List<OrderItemRequest>): Order = transaction {
        var totalPrice = BigDecimal.ZERO

        val orderId = Orders.insert {
            it[Orders.userId] = userId
            it[Orders.totalPrice] = totalPrice
        } get Orders.id

        val orderItems = items.map { item ->
            val product = Products.selectAll().where { Products.id eq item.productId }.single()
            val price = product[Products.price]
            val itemTotal = price * BigDecimal(item.quantity)
            totalPrice = totalPrice.add(itemTotal)

            val itemId = OrderItems.insert {
                it[OrderItems.orderId] = orderId.value
                it[OrderItems.productId] = item.productId
                it[OrderItems.quantity] = item.quantity
                it[OrderItems.price] = price
            } get OrderItems.id

            OrderItem(
                id = itemId.value,
                productId = item.productId,
                productName = product[Products.name],
                quantity = item.quantity,
                price = price.toString()
            )
        }

        Orders.update({ Orders.id eq orderId }) {
            it[Orders.totalPrice] = totalPrice
        }

        Order(
            id = orderId.value,
            userId = userId,
            status = OrderStatus.PENDING,
            totalPrice = totalPrice.toString(),
            items = orderItems,
            createdAt = Orders.selectAll().where { Orders.id eq orderId }.single()[Orders.createdAt].toString()
        )
    }

    fun findById(id: Long): Order? = transaction {
        val orderRow = Orders.selectAll().where { Orders.id eq id }.singleOrNull() ?: return@transaction null

        val items = (OrderItems innerJoin Products)
            .selectAll().where { OrderItems.orderId eq id }
            .map { row ->
                OrderItem(
                    id = row[OrderItems.id].value,
                    productId = row[OrderItems.productId].value,
                    productName = row[Products.name],
                    quantity = row[OrderItems.quantity],
                    price = row[OrderItems.price].toString()
                )
            }

        Order(
            id = orderRow[Orders.id].value,
            userId = orderRow[Orders.userId].value,
            status = OrderStatus.valueOf(orderRow[Orders.status]),
            totalPrice = orderRow[Orders.totalPrice].toString(),
            items = items,
            createdAt = orderRow[Orders.createdAt].toString()
        )
    }

    fun findByUserId(userId: Long): List<Order> = transaction {
        Orders.selectAll().where { Orders.userId eq userId }
            .map { orderRow ->
                val orderId = orderRow[Orders.id].value
                val items = (OrderItems innerJoin Products)
                    .selectAll().where { OrderItems.orderId eq orderId }
                    .map { row ->
                        OrderItem(
                            id = row[OrderItems.id].value,
                            productId = row[OrderItems.productId].value,
                            productName = row[Products.name],
                            quantity = row[OrderItems.quantity],
                            price = row[OrderItems.price].toString()
                        )
                    }

                Order(
                    id = orderId,
                    userId = orderRow[Orders.userId].value,
                    status = OrderStatus.valueOf(orderRow[Orders.status]),
                    totalPrice = orderRow[Orders.totalPrice].toString(),
                    items = items,
                    createdAt = orderRow[Orders.createdAt].toString()
                )
            }
    }

    fun cancel(id: Long, userId: Long): Boolean = transaction {
        val order = findById(id) ?: return@transaction false

        if (order.userId != userId) {
            return@transaction false
        }

        if (order.status != OrderStatus.PENDING) {
            return@transaction false
        }

        Orders.update({ Orders.id eq id }) {
            it[status] = OrderStatus.CANCELLED.name
        }
        true
    }

    fun countAll(): Long = transaction {
        Orders.selectAll().count()
    }

    fun getTotalRevenue(): BigDecimal = transaction {
        Orders.selectAll().where {
            (Orders.status eq OrderStatus.COMPLETED.name) or (Orders.status eq OrderStatus.PENDING.name)
        }.sumOf { it[Orders.totalPrice] }
    }
}