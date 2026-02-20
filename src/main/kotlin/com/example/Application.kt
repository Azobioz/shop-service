package com.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.launch
import com.example.config.configureDatabase
import com.example.config.configureMonitoring
import com.example.config.configureRouting
import com.example.config.configureSecurity
import com.example.config.configureSerialization
import com.example.config.configureSwagger
import com.example.config.initializeData
import com.example.kafka.KafkaConsumer
import org.flywaydb.core.Flyway

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Конфигурация базы данных
    configureDatabase()

    val dbUrl = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/shop"
    val dbUser = System.getProperty("DB_USER") ?: System.getenv("DB_USER") ?: "shop"
    val dbPass = System.getProperty("DB_PASSWORD") ?: System.getenv("DB_PASSWORD") ?: "shop"

    Flyway.configure()
        .dataSource(dbUrl, dbUser, dbPass)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    // Инициализация данных (админ, тестовые записи и т.д.)
    initializeData()

    // Конфигурация Ktor
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureSwagger()
    configureRouting()

    // Запуск Kafka consumer в отдельном корутине
    val kafkaConsumer = KafkaConsumer(
        bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "localhost:9092",
        topic = System.getenv("KAFKA_TOPIC") ?: "order-events"
    )

    launch {
        kafkaConsumer.startConsuming()
    }

    // Graceful shutdown Kafka consumer
    monitor.subscribe(ApplicationStopped) {
        kafkaConsumer.close()
    }
}