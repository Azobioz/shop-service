package com.example.service

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CacheService(redisHost: String, redisPort: Int) {

    private val redisClient: RedisClient = RedisClient.create("redis://$redisHost:$redisPort")
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val commands: RedisCommands<String, String> = connection.sync()

    fun <T> get(key: String, deserializer: (String) -> T): T? {
        val value = commands.get(key) ?: return null
        return try {
            deserializer(value)
        } catch (e: Exception) {
            null
        }
    }

    fun set(key: String, value: String, ttlSeconds: Long = 300) {
        commands.setex(key, ttlSeconds, value)
    }

    inline fun <reified T> setJson(key: String, value: T, ttlSeconds: Long = 300) {
        val json = Json.encodeToString(value)
        set(key, json, ttlSeconds)
    }

    fun delete(key: String) {
        commands.del(key)
    }

    fun deletePattern(pattern: String) {
        val keys = commands.keys(pattern)
        if (keys.isNotEmpty()) {
            commands.del(*keys.toTypedArray())
        }
    }

    fun close() {
        connection.close()
        redisClient.shutdown()
    }
}