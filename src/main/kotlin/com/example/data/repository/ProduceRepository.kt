package com.example.data.repository

import com.example.data.table.Products
import com.example.module.Product
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant

class ProductRepository {

    fun create(name: String, description: String, price: BigDecimal, stock: Int): Product = transaction {
        val productId = Products.insert {
            it[Products.name] = name
            it[Products.description] = description
            it[Products.price] = price
            it[Products.stock] = stock
        } get Products.id

        findById(productId.value)!!
    }

    fun findById(id: Long): Product? = transaction {
        Products.selectAll().where { Products.id eq id }
            .map { rowToProduct(it) }
            .singleOrNull()
    }

    fun findAll(): List<Product> = transaction {
        Products.selectAll()
            .map { rowToProduct(it) }
    }

    fun update(id: Long, name: String?, description: String?, price: BigDecimal?, stock: Int?): Product? = transaction {
        Products.update({ Products.id eq id }) {
            name?.let { n -> it[Products.name] = n }
            description?.let { d -> it[Products.description] = d }
            price?.let { p -> it[Products.price] = p }
            stock?.let { s -> it[Products.stock] = s }
            it[Products.updatedAt] = Instant.now()
        }
        findById(id)
    }

    fun delete(id: Long): Boolean = transaction {
        try {
            Products.deleteWhere { Products.id eq id } > 0
        } catch (e: Exception) {
            // Если товар используется в заказах, вернем false
            if (e.message?.contains("foreign key constraint") == true ||
                e.message?.contains("fk_order_items_product") == true) {
                false
            } else {
                throw e
            }
        }
    }

    fun decreaseStock(id: Long, quantity: Int): Boolean = transaction {
        val product = findById(id) ?: return@transaction false
        val currentStock = product.stock

        if (currentStock < quantity) {
            return@transaction false
        }

        Products.update({ Products.id eq id }) {
            it[stock] = currentStock - quantity
            it[updatedAt] = Instant.now()
        }
        true
    }

    private fun rowToProduct(row: ResultRow): Product {
        return Product(
            id = row[Products.id].value,
            name = row[Products.name],
            description = row[Products.description],
            price = row[Products.price].toString(),
            stock = row[Products.stock],
            createdAt = row[Products.createdAt].toString(),
            updatedAt = row[Products.updatedAt].toString()
        )
    }
}