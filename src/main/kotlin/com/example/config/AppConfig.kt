package com.example.config

import com.typesafe.config.ConfigFactory

data class AppConfig(
    val environment: String
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val config = ConfigFactory.load()
            return AppConfig(
                environment = config.getString("ktor.environment")
            )
        }
    }
}

data class DatabaseConfig(
    val url: String,
    val user: String,
    val password: String,
    val driver: String = "org.postgresql.Driver",
    val maxPoolSize: Int = 10
) {


    companion object {
        fun fromEnvironment(): DatabaseConfig {
            val config = ConfigFactory.load()
            return DatabaseConfig(
                url = System.getProperty("DATABASE_URL") ?: System.getenv("DATABASE_URL") ?: config.getString("database.url"),
                user = System.getProperty("DATABASE_USER") ?: System.getenv("DATABASE_USER") ?: config.getString("database.user"),
                password = System.getProperty("DATABASE_PASSWORD") ?: System.getenv("DATABASE_PASSWORD") ?: config.getString("database.password"),
                maxPoolSize = config.getInt("database.maxPoolSize")
            )
        }
    }
}

data class RedisConfig(
    val host: String,
    val port: Int,
    val password: String? = null,
    val timeout: Int = 2000
) {
    companion object {
        fun fromEnvironment(): RedisConfig {
            val config = ConfigFactory.load()
            return RedisConfig(
                host = System.getProperty("REDIS_HOST") ?: System.getenv("REDIS_HOST") ?: config.getString("redis.host"),
                port = (System.getProperty("REDIS_PORT") ?: System.getenv("REDIS_PORT") ?: config.getString("redis.port")).toInt(),
                password = System.getProperty("REDIS_PASSWORD") ?: System.getenv("REDIS_PASSWORD") ?: config.getString("redis.password").takeIf { it.isNotBlank() }
            )
        }
    }
}

data class KafkaConfig(
    val bootstrapServers: String,
    val groupId: String,
    val topic: String
) {
    companion object {
        fun fromEnvironment(): KafkaConfig {
            val config = ConfigFactory.load()
            return KafkaConfig(
                bootstrapServers = System.getProperty("KAFKA_BOOTSTRAP_SERVERS") ?: System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: config.getString("kafka.bootstrapServers"),
                groupId = config.getString("kafka.groupId"),
                topic = config.getString("kafka.topic")
            )
        }
    }
}