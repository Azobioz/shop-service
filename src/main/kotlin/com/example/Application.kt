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

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureDatabase()
    initializeData() // По умолчанию создаем админа при запуске
    configureSerialization()
    configureSecurity()
    configureMonitoring()
    configureSwagger()
    configureRouting()

    // Запускаем Kafka consumer в фоновом режиме
    val config = environment.config
    val kafkaConsumer = KafkaConsumer(
        bootstrapServers = System.getProperty("KAFKA_BOOTSTRAP_SERVERS") ?: System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: config.propertyOrNull("kafka.bootstrapServers")?.getString() ?: "localhost:9092",
        topic = System.getProperty("KAFKA_TOPIC") ?: System.getenv("KAFKA_TOPIC") ?: config.propertyOrNull("kafka.topic")?.getString() ?: "order-events"
    )

    launch {
        kafkaConsumer.startConsuming()
    }

    // Graceful shutdown
    monitor.subscribe(ApplicationStopped) {
        kafkaConsumer.close()
    }
}