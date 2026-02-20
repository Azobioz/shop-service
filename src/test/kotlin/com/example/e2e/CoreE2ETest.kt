package com.example.e2e

import com.example.config.configureDatabase
import com.example.config.configureMonitoring
import com.example.config.configureRouting
import com.example.config.configureSecurity
import com.example.config.configureSerialization
import com.example.config.configureSwagger
import com.example.config.initializeData
import io.ktor.server.application.Application
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName


@Testcontainers
abstract class CoreE2ETest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer<Nothing>("postgres:15-alpine").apply {
            withDatabaseName("e2e_test_db")
            withUsername("test")
            withPassword("test")
        }

        @Container
        @JvmStatic
        val redis = GenericContainer<Nothing>(DockerImageName.parse("redis:7-alpine")).apply {
            withExposedPorts(6379)
        }

        fun Application.moduleForTest() {
            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)

            Flyway.configure()
                .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .locations("classpath:db/migration")
                .load()
                .migrate()

            configureDatabase()
            configureSerialization()
            configureSecurity()
            configureMonitoring()
            configureSwagger()
            configureRouting()
            initializeData()
        }

        @Container
        @JvmStatic
        val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0")).apply {
            withEmbeddedZookeeper()
        }

        @JvmStatic
        @BeforeAll
        fun setupInfrastructure() {
            // Настраиваем PostgreSQL
            System.setProperty("DB_URL", postgres.jdbcUrl)
            System.setProperty("DB_USER", postgres.username)
            System.setProperty("DB_PASSWORD", postgres.password)
            System.setProperty("DB_DRIVER", postgres.driverClassName)

            // Настраиваем Redis
            System.setProperty("REDIS_HOST", redis.host)
            System.setProperty("REDIS_PORT", redis.getMappedPort(6379).toString())

            // Настраиваем Kafka
            System.getProperty("KAFKA_BOOTSTRAP_SERVERS") ?: kafka.bootstrapServers

            println("E2E Test infrastructure configured:")
            println("  PostgreSQL: ${postgres.jdbcUrl}")
            println("  Redis: ${redis.host}:${redis.getMappedPort(6379)}")
            println("  Kafka: ${kafka.bootstrapServers}")
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            // Очищаем системные переменные после тестов
            System.clearProperty("DB_URL")
            System.clearProperty("DB_USER")
            System.clearProperty("DB_PASSWORD")
            System.clearProperty("DB_DRIVER")
            System.clearProperty("REDIS_HOST")
            System.clearProperty("REDIS_PORT")
            System.clearProperty("KAFKA_BOOTSTRAP_SERVERS")
        }
    }
}