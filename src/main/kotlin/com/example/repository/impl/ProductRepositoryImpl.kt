package com.example.repository.impl

import com.example.domain.Product
import com.example.repository.ProductRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.ZoneOffset

object ProductsTable : Table("products") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val price = decimal("price", 10, 2)
    val stock = integer("stock")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

class ProductRepositoryImpl : ProductRepository {
    override suspend fun create(product: Product): Product = newSuspendedTransaction {
        val insert = ProductsTable.insert {
            it[name] = product.name
            it[description] = product.description
            it[price] = product.price
            it[stock] = product.stock
        }
        product.copy(id = insert[ProductsTable.id])
    }

    override suspend fun findById(id: Int): Product? = newSuspendedTransaction {
        ProductsTable.select { ProductsTable.id eq id }
            .map { toProduct(it) }
            .singleOrNull()
    }

    override suspend fun findAll(limit: Int, offset: Int): List<Product> = newSuspendedTransaction {
        ProductsTable.selectAll()
            .limit(limit, offset.toLong())
            .orderBy(ProductsTable.id)
            .map { toProduct(it) }
    }

    override suspend fun update(product: Product): Product = newSuspendedTransaction {
        ProductsTable.update({ ProductsTable.id eq product.id!! }) {
            it[name] = product.name
            it[description] = product.description
            it[price] = product.price
            it[stock] = product.stock
            it[updatedAt] = LocalDateTime.now()
        }
        product
    }

    override suspend fun delete(id: Int): Boolean = newSuspendedTransaction {
        ProductsTable.deleteWhere { ProductsTable.id eq id } > 0
    }

    override suspend fun updateStock(id: Int, newStock: Int): Boolean = newSuspendedTransaction {
        ProductsTable.update({ ProductsTable.id eq id }) {
            it[stock] = newStock
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    private fun toProduct(row: ResultRow): Product = Product(
        id = row[ProductsTable.id],
        name = row[ProductsTable.name],
        description = row[ProductsTable.description],
        price = row[ProductsTable.price],
        stock = row[ProductsTable.stock],
        createdAt = row[ProductsTable.createdAt].toInstant(ZoneOffset.UTC),
        updatedAt = row[ProductsTable.updatedAt].toInstant(ZoneOffset.UTC)
    )
}