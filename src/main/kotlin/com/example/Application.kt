package com.example

import com.example.config.*
import com.example.messaging.OrderEventConsumer
import com.example.migrations.FlywayMigration
import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val appConfig = AppConfig.fromEnvironment()
    val dbConfig = DatabaseConfig.fromEnvironment()
    val redisConfig = RedisConfig.fromEnvironment()
    val kafkaConfig = KafkaConfig.fromEnvironment()
    val securityConfig = SecurityConfig.fromEnvironment()

    FlywayMigration.migrate(dbConfig)

    val database = DatabaseFactory.connect(dbConfig)
    val jedisPool = RedisFactory.createPool(redisConfig)
    val kafkaProducer = KafkaFactory.createProducer(kafkaConfig)
    val kafkaConsumer = KafkaFactory.createConsumer(kafkaConfig)

    val consumerJob = CoroutineScope(Dispatchers.IO).launch {
        OrderEventConsumer.consume(kafkaConsumer)
    }

    configureSerialization()
    configureMonitoring()
    configureCORS()
    configureStatusPages()
    configureSecurity(securityConfig)
    configureRouting(database, jedisPool, kafkaProducer, securityConfig)

    configureSwagger()

    environment.monitor.subscribe(ApplicationStopped) {
        runBlocking {
            consumerJob.cancelAndJoin()
            kafkaProducer.close()
            kafkaConsumer.close()
            jedisPool.close()
        }
    }
}