package com.example.cache

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import redis.clients.jedis.JedisPool

class RedisCache(@PublishedApi internal val jedisPool: JedisPool) {
    @PublishedApi
    internal val json = Json { ignoreUnknownKeys = true }

    fun set(key: String, value: Any, ttlSeconds: Int = 3600) {
        jedisPool.resource.use { jedis ->
            val serialized = json.encodeToString(value)
            jedis.setex(key, ttlSeconds, serialized)
        }
    }

    inline fun <reified T : Any> get(key: String): T? {
        jedisPool.resource.use { jedis ->
            val value = jedis.get(key) ?: return null
            return json.decodeFromString<T>(value)
        }
    }

    fun delete(key: String) {
        jedisPool.resource.use { jedis ->
            jedis.del(key)
        }
    }

    fun deleteByPattern(pattern: String) {
        jedisPool.resource.use { jedis ->
            val keys = jedis.keys(pattern)
            if (keys.isNotEmpty()) {
                jedis.del(*keys.toTypedArray())
            }
        }
    }
}