package com.example.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.example.config.SecurityConfig
import java.util.*

class JwtService(private val config: SecurityConfig) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    val verifier = JWT.require(algorithm)
        .withIssuer(config.jwtIssuer)
        .withAudience(config.jwtAudience)
        .build()

    fun generateToken(userId: Int, email: String, role: String): String {
        return JWT.create()
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("role", role)
            .withIssuer(config.jwtIssuer)
            .withAudience(config.jwtAudience)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + config.jwtExpiration))
            .sign(algorithm)
    }

    fun verifyToken(token: String): JwtClaims? {
        return try {
            val decoded = verifier.verify(token)
            JwtClaims(
                userId = decoded.subject?.toInt() ?: return null,
                email = decoded.getClaim("email").asString(),
                role = decoded.getClaim("role").asString()
            )
        } catch (e: JWTVerificationException) {
            null
        }
    }
}

data class JwtClaims(
    val userId: Int,
    val email: String,
    val role: String
)