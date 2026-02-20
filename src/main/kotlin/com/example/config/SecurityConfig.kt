package com.example.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.example.module.UserRole

fun Application.configureSecurity() {
    val config = environment.config

    val jwtSecret = System.getenv("JWT_SECRET") ?: config.propertyOrNull("jwt.secret")?.getString() ?: "your-secret-key-change-in-production"
    val jwtIssuer = System.getenv("JWT_ISSUER") ?: config.propertyOrNull("jwt.issuer")?.getString() ?: "http://localhost:8080"
    val jwtAudience = System.getenv("JWT_AUDIENCE") ?: config.propertyOrNull("jwt.audience")?.getString() ?: "http://localhost:8080/api"
    val jwtRealm = System.getenv("JWT_REALM") ?: config.propertyOrNull("jwt.realm")?.getString() ?: "Access to API"

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asLong() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

// Функции для работы с JWT
val ApplicationCall.userId: Long
    get() = principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asLong()
        ?: throw IllegalStateException("User ID not found in token")

val ApplicationCall.userRole: UserRole
    get() {
        val role = principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
            ?: throw IllegalStateException("User role not found in token")
        return UserRole.valueOf(role)
    }