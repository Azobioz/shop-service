package com.example.integration

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.example.data.repository.UserRepository
import com.example.data.table.Users
import com.example.module.UserRole
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Testcontainers
class UserRepositoryIntegrationTest : CoreIntegrationTest() {

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
                SchemaUtils.create(Users)
            }
        }

        @AfterAll
        @JvmStatic
        fun teardown() {
            transaction {
                SchemaUtils.drop(Users)
            }
        }
    }

    private val repository = UserRepository()

    @Test
    fun `should create user with hashed password`() {
        val email = "test@example.com"
        val password = "password123"

        val user = repository.create(email, password, UserRole.USER)

        assertNotNull(user)
        assertEquals(email, user.email)
        assertEquals(UserRole.USER, user.role)
    }

    @Test
    fun `should verify correct password`() {
        val email = "verify@example.com"
        val password = "correctPassword"

        repository.create(email, password, UserRole.USER)

        val verified = repository.verifyPassword(email, password)
        assertNotNull(verified)
        assertEquals(email, verified.email)
    }

    @Test
    fun `should not verify incorrect password`() {
        val email = "wrong@example.com"
        val password = "correctPassword"

        repository.create(email, password, UserRole.USER)

        val verified = repository.verifyPassword(email, "wrongPassword")
        assertNull(verified)
    }
}