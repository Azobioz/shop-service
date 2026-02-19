package com.example.controller

import com.example.dto.OrderRequest
import com.example.service.OrderService
import com.example.plugins.getUserId
import com.example.plugins.getUserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.orderRoutes(orderService: OrderService) {
    authenticate("auth-jwt") {
        route("/orders") {
            post {
                val userId = getUserId()
                val request = call.receive<OrderRequest>()
                try {
                    val order = orderService.createOrder(userId, request)
                    call.respond(HttpStatusCode.Created, order)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                }
            }

            get {
                val userId = getUserId()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val orders = orderService.getUserOrders(userId, limit, offset)
                call.respond(HttpStatusCode.OK, orders)
            }

            delete("/{id}") {
                val userId = getUserId()
                val role = getUserRole()
                val isAdmin = role == "ADMIN"
                val orderId = call.parameters["id"]?.toIntOrNull()
                if (orderId == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                try {
                    val cancelled = orderService.cancelOrder(orderId, userId, isAdmin)
                    if (cancelled) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
                } catch (e: SecurityException) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to e.message))
                }
            }
        }
    }
}