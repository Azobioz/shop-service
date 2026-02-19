package com.example.integration

import com.example.config.DatabaseConfig
import com.example.dto.AuthResponse
import com.example.dto.LoginRequest
import com.example.dto.ProductRequest
import com.example.dto.ProductResponse
import com.example.migrations.FlywayMigration
import com.example.module
import com.example.repository.impl.ProductRepositoryImpl
import com.example.repository.impl.ProductsTable
import com.example.repository.impl.UsersTable
import com.example.utils.PasswordHasher
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import com.example.plugins.BigDecimalSerializer
import kotlinx.serialization.modules.SerializersModule

@Testcontainers
class ProductIntegrationTest {
    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb_product")
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

    private lateinit var productRepository: ProductRepositoryImpl

    @BeforeEach
    fun init() {
        productRepository = ProductRepositoryImpl()
        transaction {
            ProductsTable.deleteAll()
            UsersTable.deleteAll()
        }
    }

    @Test
    fun `create product via API`() = testApplication {
        System.setProperty("DATABASE_URL", postgres.jdbcUrl)
        System.setProperty("DATABASE_USER", postgres.username)
        System.setProperty("DATABASE_PASSWORD", postgres.password)
        System.setProperty("REDIS_HOST", redis.host)
        System.setProperty("REDIS_PORT", redis.getMappedPort(6379).toString())
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", "localhost:${kafka.getMappedPort(9093)}")
        System.setProperty("JWT_SECRET", "testsecret")

        application {
            module()
        }

        val client = createClient {
            install(ContentNegotiation) {
                val json = Json {
                    ignoreUnknownKeys = true
                    serializersModule = SerializersModule {
                        contextual(BigDecimal::class, BigDecimalSerializer)
                    }
                }
            }
        }

        val adminId = transaction {
            UsersTable.insert {
                it[UsersTable.username] = "admin"
                it[UsersTable.email] = "admin@example.com"
                it[UsersTable.passwordHash] = PasswordHasher.hash("adminpass")
                it[UsersTable.role] = "ADMIN"
            } get (UsersTable.id)
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest("admin@example.com", "adminpass"))
        }
        Assertions.assertEquals(HttpStatusCode.OK, loginResponse.status)
        val token = loginResponse.body<AuthResponse>().token


        val request = ProductRequest("Test Product", "Description", BigDecimal.valueOf(99.99), 10)
        val response = client.post("/products") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(request)
        }
        Assertions.assertEquals(HttpStatusCode.Created, response.status)
        val product = response.body<ProductResponse>()
        Assertions.assertNotNull(product.id)
        Assertions.assertEquals("Test Product", product.name)

        val dbProduct = transaction {
            ProductsTable.select { ProductsTable.id eq product.id }.singleOrNull()
        }
        Assertions.assertNotNull(dbProduct)
        Assertions.assertEquals(10, dbProduct!![ProductsTable.stock])
    }
}