package com.example.repository.impl

import com.example.domain.Order
import com.example.domain.OrderItem
import com.example.domain.OrderStatus
import com.example.repository.OrderRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

object OrdersTable : Table("orders") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(UsersTable.id)
    val status = varchar("status", 50)
    val totalAmount = decimal("total_amount", 10, 2)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object OrderItemsTable : Table("order_items") {
    val id = integer("id").autoIncrement()
    val orderId = integer("order_id").references(OrdersTable.id)
    val productId = integer("product_id").references(ProductsTable.id)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2)

    override val primaryKey = PrimaryKey(id)
}

class OrderRepositoryImpl : OrderRepository {
    override suspend fun create(order: Order, items: List<OrderItem>): Order = newSuspendedTransaction {
        val insert = OrdersTable.insert {
            it[userId] = order.userId
            it[status] = order.status.name
            it[totalAmount] = order.totalAmount
        }
        val orderId = insert[OrdersTable.id]
        items.forEach { item ->
            OrderItemsTable.insert {
                it[OrderItemsTable.orderId] = orderId
                it[OrderItemsTable.productId] = item.productId
                it[OrderItemsTable.quantity] = item.quantity
                it[OrderItemsTable.price] = item.price
            }
        }
        order.copy(id = orderId, items = items.map { it.copy(orderId = orderId) })
    }

    override suspend fun findById(id: Int): Order? = newSuspendedTransaction {
        val orderRow = OrdersTable.select { OrdersTable.id eq id }.singleOrNull()
            ?: return@newSuspendedTransaction null
        val items = OrderItemsTable.select { OrderItemsTable.orderId eq id }
            .map { toOrderItem(it) }
        toOrder(orderRow, items)
    }

    override suspend fun findByUser(userId: Int, limit: Int, offset: Int): List<Order> = newSuspendedTransaction {
        val orders = OrdersTable.select { OrdersTable.userId eq userId }
            .limit(limit, offset.toLong())
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .map { row ->
                val items = OrderItemsTable.select { OrderItemsTable.orderId eq row[OrdersTable.id] }
                    .map { toOrderItem(it) }
                toOrder(row, items)!!
            }
        orders
    }

    override suspend fun updateStatus(id: Int, status: OrderStatus): Boolean = newSuspendedTransaction {
        OrdersTable.update({ OrdersTable.id eq id }) {
            it[OrdersTable.status] = status.name
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    override suspend fun delete(id: Int): Boolean = newSuspendedTransaction {
        OrdersTable.deleteWhere { OrdersTable.id eq id } > 0
    }

    override suspend fun countByStatus(): Map<OrderStatus, Long> = newSuspendedTransaction {
        OrdersTable.slice(OrdersTable.status, OrdersTable.id.count())
            .selectAll()
            .groupBy(OrdersTable.status)
            .associate { row ->
                OrderStatus.valueOf(row[OrdersTable.status]) to row[OrdersTable.id.count()]
            }
    }

    override suspend fun totalRevenue(): BigDecimal = newSuspendedTransaction {
        OrdersTable.slice(OrdersTable.totalAmount.sum())
            .select { OrdersTable.status eq OrderStatus.DELIVERED.name }
            .map { it[OrdersTable.totalAmount.sum()] ?: BigDecimal.ZERO }
            .firstOrNull() ?: BigDecimal.ZERO
    }

    private fun toOrderItem(row: ResultRow): OrderItem = OrderItem(
        id = row[OrderItemsTable.id],
        orderId = row[OrderItemsTable.orderId],
        productId = row[OrderItemsTable.productId],
        quantity = row[OrderItemsTable.quantity],
        price = row[OrderItemsTable.price]
    )

    private fun toOrder(row: ResultRow, items: List<OrderItem>): Order? = Order(
        id = row[OrdersTable.id],
        userId = row[OrdersTable.userId],
        status = OrderStatus.valueOf(row[OrdersTable.status]),
        totalAmount = row[OrdersTable.totalAmount],
        createdAt = row[OrdersTable.createdAt].toInstant(ZoneOffset.UTC),
        updatedAt = row[OrdersTable.updatedAt].toInstant(ZoneOffset.UTC),
        items = items
    )
}