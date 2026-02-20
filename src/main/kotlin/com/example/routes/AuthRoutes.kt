package com.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import com.example.module.UserCredentials
import com.example.service.AuthService

@Serializable
data class ErrorResponse(val error: String)

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            val credentials = call.receive<UserCredentials>()

            // Валидация
            if (credentials.email.isBlank() || credentials.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email and password are required"))
                return@post
            }

            if (credentials.password.length < 6) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Password must be at least 6 characters"))
                return@post
            }

            val response = authService.register(credentials.email, credentials.password)

            if (response == null) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("User already exists"))
            } else {
                call.respond(HttpStatusCode.Created, response)
            }
        }

        post("/login") {
            val credentials = call.receive<UserCredentials>()

            val response = authService.login(credentials.email, credentials.password)

            if (response == null) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            } else {
                call.respond(HttpStatusCode.OK, response)
            }
        }
    }
}