package com.example.plugins

import com.example.config.SecurityConfig
import com.example.security.JwtService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.PipelineContext

fun Application.configureSecurity(config: SecurityConfig) {
    val jwtService = JwtService(config)
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtService.verifier)
            validate { credential ->
                val payload = credential.payload
                val userId = payload.getClaim("userId")?.asInt()
                val email = payload.getClaim("email")?.asString()
                val role = payload.getClaim("role")?.asString()
                if (userId != null && email != null && role != null) {
                    JWTPrincipal(payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Invalid or missing token")
            }
        }
    }
}

fun PipelineContext<*, ApplicationCall>.getUserId(): Int {
    val principal = call.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("userId")?.asInt() ?: throw SecurityException("Unauthorized")
}

fun PipelineContext<*, ApplicationCall>.getUserRole(): String {
    val principal = call.principal<JWTPrincipal>()
    return principal?.payload?.getClaim("role")?.asString() ?: throw SecurityException("Unauthorized")
}