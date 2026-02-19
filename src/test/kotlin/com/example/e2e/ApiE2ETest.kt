package com.example.e2e

import com.example.module
import com.example.config.DatabaseConfig
import com.example.dto.*
import com.example.migrations.FlywayMigration
import com.example.repository.impl.ProductsTable
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal

@Testcontainers
class ApiE2ETest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb_e2e")
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
        fun init() {
            val dbConfig = DatabaseConfig(
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password
            )

            FlywayMigration.migrate(dbConfig)
            // Подключаем Exposed к той же БД
            Database.connect(
                url = postgres.jdbcUrl,
                driver = "org.postgresql.Driver",
                user = postgres.username,
                password = postgres.password
            )

            System.setProperty("DATABASE_URL", postgres.jdbcUrl)
            System.setProperty("DATABASE_USER", postgres.username)
            System.setProperty("DATABASE_PASSWORD", postgres.password)
        }
    }

    @BeforeEach
    fun setup() {
        System.setProperty("DATABASE_URL", postgres.jdbcUrl)
        System.setProperty("DATABASE_USER", postgres.username)
        System.setProperty("DATABASE_PASSWORD", postgres.password)
        System.setProperty("REDIS_HOST", redis.host)
        System.setProperty("REDIS_PORT", redis.getMappedPort(6379).toString())
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", "localhost:${kafka.getMappedPort(9093)}")
        System.setProperty("JWT_SECRET", "testsecret")
    }

    @Test
    fun `full order flow`() = testApplication {
        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest("testuser", "test@example.com", "password"))
        }
        Assertions.assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authToken = registerResponse.body<AuthResponse>().token

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("test@example.com", "password"))
        }
        Assertions.assertEquals(HttpStatusCode.OK, loginResponse.status)

        val productId = createProductDirectly()

        val orderResponse = client.post("/orders") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $authToken")
            setBody(OrderRequest(listOf(OrderItemRequest(productId, 2))))
        }
        Assertions.assertEquals(HttpStatusCode.Created, orderResponse.status)
        val order = orderResponse.body<OrderResponse>()
        Assertions.assertEquals(2, order.items.first().quantity)

        val ordersResponse = client.get("/orders") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        Assertions.assertEquals(HttpStatusCode.OK, ordersResponse.status)
        val orders = ordersResponse.body<List<OrderResponse>>()
        Assertions.assertTrue(orders.isNotEmpty())

        val cancelResponse = client.delete("/orders/${order.id}") {
            header(HttpHeaders.Authorization, "Bearer $authToken")
        }
        Assertions.assertEquals(HttpStatusCode.NoContent, cancelResponse.status)
    }

    private fun createProductDirectly(): Int {
        return transaction {
            ProductsTable.insert {
                it[ProductsTable.name] = "Test Product"
                it[ProductsTable.description] = "Description"
                it[ProductsTable.price] = BigDecimal.valueOf(50)
                it[ProductsTable.stock] = 10
            } get (ProductsTable.id)
        }
    }
}