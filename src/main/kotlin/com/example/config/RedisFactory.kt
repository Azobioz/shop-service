package com.example.config

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

object RedisFactory {
    fun createPool(config: RedisConfig): JedisPool {
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = 10
            maxIdle = 5
            minIdle = 1
        }
        return if (config.password != null && config.password.isNotBlank()) {
            JedisPool(poolConfig, config.host, config.port, config.timeout, config.password)
        } else {
            JedisPool(poolConfig, config.host, config.port, config.timeout)
        }
    }
}