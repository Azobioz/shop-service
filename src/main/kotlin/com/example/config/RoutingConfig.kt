package com.example.config

import io.ktor.server.application.*
import io.ktor.server.routing.*
import com.example.data.repository.*
import com.example.kafka.KafkaProducer
import com.example.routes.*
import com.example.service.*

fun Application.configureRouting() {
    val config = environment.config

    // Инициализация
    val userRepository = UserRepository()
    val productRepository = ProductRepository()
    val orderRepository = OrderRepository()
    val auditLogRepository = AuditLogRepository()

    val cacheService = CacheService(
        redisHost = System.getProperty("REDIS_HOST") ?: System.getenv("REDIS_HOST") ?: config.propertyOrNull("redis.host")?.getString() ?: "localhost",
        redisPort = System.getProperty("REDIS_PORT")?.toIntOrNull() ?: System.getenv("REDIS_PORT")?.toIntOrNull() ?: config.propertyOrNull("redis.port")?.getString()?.toIntOrNull() ?: 6379
    )

    val kafkaProducer = KafkaProducer(
        bootstrapServers = System.getProperty("KAFKA_BOOTSTRAP_SERVERS") ?: System.getenv("KAFKA_BOOTSTRAP_SERVERS")
        ?: config.propertyOrNull("kafka.bootstrapServers")?.getString() ?: "localhost:9092",
        topic = System.getProperty("KAFKA_TOPIC") ?: System.getenv("KAFKA_TOPIC")
        ?: config.propertyOrNull("kafka.topic")?.getString() ?: "order-events"
    )

    val authService = AuthService(
        userRepository = userRepository,
        jwtSecret = System.getenv("JWT_SECRET") ?: config.propertyOrNull("jwt.secret")?.getString() ?: "your-secret-key-change-in-production",
        jwtIssuer = System.getenv("JWT_ISSUER") ?: config.propertyOrNull("jwt.issuer")?.getString() ?: "http://localhost:8080",
        jwtAudience = System.getenv("JWT_AUDIENCE") ?: config.propertyOrNull("jwt.audience")?.getString() ?: "http://localhost:8080/api"
    )

    val productService = ProductService(productRepository, cacheService)
    val orderService = OrderService(
        orderRepository,
        productRepository,
        auditLogRepository,
        kafkaProducer,
        cacheService
    )

    routing {
        authRoutes(authService)
        productRoutes(productService)
        orderRoutes(orderService)
        adminRoutes(productService, orderRepository)
    }
}