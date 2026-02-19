package com.example.config

import com.typesafe.config.ConfigFactory

data class SecurityConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtRealm: String,
    val jwtExpiration: Long
) {
    companion object {
        fun fromEnvironment(): SecurityConfig {
            val config = ConfigFactory.load()
            return SecurityConfig(
                jwtSecret = System.getenv("JWT_SECRET") ?: config.getString("jwt.secret"),
                jwtIssuer = config.getString("jwt.issuer"),
                jwtAudience = config.getString("jwt.audience"),
                jwtRealm = config.getString("jwt.realm"),
                jwtExpiration = config.getLong("jwt.expiration")
            )
        }
    }
}