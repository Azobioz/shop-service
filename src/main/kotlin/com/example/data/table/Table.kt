package com.example.data.table

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object Users : LongIdTable("users") {
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50).default("USER")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object Products : LongIdTable("products") {
    val name = varchar("name", 255)
    val description = text("description")
    val price = decimal("price", 10, 2)
    val stock = integer("stock").default(0)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

object Orders : LongIdTable("orders") {
    val userId = reference("user_id", Users)
    val status = varchar("status", 50).default("PENDING")
    val totalPrice = decimal("total_price", 10, 2)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}

object OrderItems : LongIdTable("order_items") {
    val orderId = reference("order_id", Orders)
    val productId = reference("product_id", Products)
    val quantity = integer("quantity")
    val price = decimal("price", 10, 2)
}

object AuditLogs : LongIdTable("audit_logs") {
    val userId = long("user_id").nullable()
    val action = varchar("action", 255)
    val entityType = varchar("entity_type", 100)
    val entityId = long("entity_id").nullable()
    val details = text("details").nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
}