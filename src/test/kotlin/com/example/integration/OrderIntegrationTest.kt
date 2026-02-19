package com.example.integration

import com.example.config.DatabaseConfig
import com.example.dto.OrderItemRequest
import com.example.dto.OrderRequest
import com.example.migrations.FlywayMigration
import com.example.repository.impl.*
import com.example.service.OrderService
import com.example.messaging.OrderEventProducer
import com.example.cache.RedisCache
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.*
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

@Testcontainers
class OrderIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb_order")
            withUsername("test")
            withPassword("test")
        }

        @Container
        val redis = GenericContainer(DockerImageName.parse("redis:7-alpine")).apply {
            withExposedPorts(6379)
        }

        @Container
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

        @JvmStatic
        @BeforeAll
        fun setup() {
            val dbConfig = DatabaseConfig(
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password
            )
            FlywayMigration.migrate(dbConfig)
            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )
        }
    }

    private lateinit var orderService: OrderService
    private lateinit var productRepository: ProductRepositoryImpl
    private lateinit var orderRepository: OrderRepositoryImpl
    private lateinit var auditLogRepository: AuditLogRepositoryImpl

    @BeforeEach
    fun init() {
        productRepository = ProductRepositoryImpl()
        orderRepository = OrderRepositoryImpl()
        auditLogRepository = AuditLogRepositoryImpl()
        val eventProducer = mockk<OrderEventProducer>(relaxed = true)
        val cache = mockk<RedisCache>(relaxed = true)
        orderService = OrderService(orderRepository, productRepository, auditLogRepository, eventProducer)

        transaction {
            OrderItemsTable.deleteAll()
            OrdersTable.deleteAll()
            ProductsTable.deleteAll()
            UsersTable.deleteAll()
        }
    }

    @Test
    fun `createOrder should persist order and decrease stock`() = runBlocking {
        val product = transaction {
            ProductsTable.insert { row ->
                row[name] = "Test Product"
                row[description] = "Desc"
                row[price] = BigDecimal.valueOf(100)
                row[stock] = 10
            } get (ProductsTable.id)
        }

        val userId = transaction {
            UsersTable.insert { row ->
                row[username] = "testuser"
                row[email] = "test@example.com"
                row[passwordHash] = "hash"
                row[role] = "USER"
            } get (UsersTable.id)
        }

        val request = OrderRequest(listOf(OrderItemRequest(product, 3)))

        val result = orderService.createOrder(userId, request)

        Assertions.assertNotNull(result.id)
        Assertions.assertEquals(0,
            BigDecimal.valueOf(300).compareTo(result.totalAmount)
        )

        val updatedStock = transaction {
            ProductsTable.select { ProductsTable.id eq product }
                .single()[ProductsTable.stock]
        }
        Assertions.assertEquals(7, updatedStock)

        val auditCount = transaction {
            AuditLogsTable.selectAll().count()
        }
        Assertions.assertEquals(1, auditCount)
    }
}