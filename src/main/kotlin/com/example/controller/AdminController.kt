package com.example.controller

import com.example.service.StatsService
import com.example.plugins.getUserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.adminRoutes(statsService: StatsService) {
    authenticate("auth-jwt") {
        route("/admin") {
            get("/stats/orders") {
                val role = getUserRole()
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val stats = statsService.getOrdersStats()
                call.respond(HttpStatusCode.OK, stats)
            }
        }
    }
}