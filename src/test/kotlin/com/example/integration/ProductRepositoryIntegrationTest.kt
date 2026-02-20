package com.example.integration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.example.data.repository.ProductRepository
import com.example.data.table.Products
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Testcontainers
class ProductRepositoryIntegrationTest : CoreIntegrationTest() {

    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test")
            withPassword("test")
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            val config = HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl
                username = postgres.username
                password = postgres.password
                driverClassName = postgres.driverClassName
            }

            Database.connect(HikariDataSource(config))

            transaction {
                SchemaUtils.create(Products)
            }
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            transaction {
                SchemaUtils.drop(Products)
            }
        }
    }

    private val repository = ProductRepository()

    @Test
    fun `should create and find product`() {
        val product = repository.create(
            "Test Product",
            "Test Description",
            BigDecimal("99.99"),
            10
        )

        assertNotNull(product)
        assertEquals("Test Product", product.name)
        assertEquals("99.99", product.price)

        val found = repository.findById(product.id)
        assertNotNull(found)
        assertEquals(product.id, found.id)
    }

    @Test
    fun `should decrease stock correctly`() {
        val product = repository.create(
            "Stock Test Product",
            "Description",
            BigDecimal("50.00"),
            20
        )

        val decreased = repository.decreaseStock(product.id, 5)
        assertEquals(true, decreased)

        val updated = repository.findById(product.id)
        assertNotNull(updated)
        assertEquals(15, updated.stock)
    }
}