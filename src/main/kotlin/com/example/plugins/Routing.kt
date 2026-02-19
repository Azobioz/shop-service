package com.example.plugins

import com.example.cache.RedisCache
import com.example.config.SecurityConfig
import com.example.controller.adminRoutes
import com.example.controller.authRoutes
import com.example.controller.orderRoutes
import com.example.controller.productRoutes
import com.example.messaging.OrderEventProducer
import com.example.repository.impl.*
import com.example.service.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Database
import redis.clients.jedis.JedisPool
import org.apache.kafka.clients.producer.KafkaProducer

fun Application.configureRouting(
    database: Database,
    jedisPool: JedisPool,
    kafkaProducer: KafkaProducer<String, String>,
    securityConfig: SecurityConfig
) {
    val userRepository = UserRepositoryImpl()
    val productRepository = ProductRepositoryImpl()
    val orderRepository = OrderRepositoryImpl()
    val auditLogRepository = AuditLogRepositoryImpl()

    val cache = RedisCache(jedisPool)

    val kafkaConfig = com.example.config.KafkaConfig.fromEnvironment()
    val eventProducer = OrderEventProducer(kafkaProducer, kafkaConfig)

    val jwtService = com.example.security.JwtService(securityConfig)
    val authService = AuthService(userRepository, jwtService)
    val productService = ProductService(productRepository, cache)
    val orderService = OrderService(orderRepository, productRepository, auditLogRepository, eventProducer)
    val statsService = StatsService(orderRepository)

    routing {
        authRoutes(authService)
        productRoutes(productService)
        orderRoutes(orderService)
        adminRoutes(statsService)
    }
}