package com.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.config.userId
import com.example.module.CreateOrderRequest
import com.example.service.OrderService

fun Route.orderRoutes(orderService: OrderService) {
    authenticate("auth-jwt") {
        route("/orders") {
            // Создание заказа
            post {
                val userId = call.userId
                val request = call.receive<CreateOrderRequest>()

                if (request.items.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Order must contain at least one item"))
                    return@post
                }

                val result = orderService.createOrder(userId, request)

                result.onSuccess { order ->
                    call.respond(HttpStatusCode.Created, order)
                }.onFailure { error ->
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse(error.message ?: "Failed to create order"))
                }
            }

            // Получение всех заказов пользователя
            get {
                val userId = call.userId
                val orders = orderService.getUserOrders(userId)
                call.respond(HttpStatusCode.OK, orders)
            }

            // Отмена заказа
            delete("/{id}") {
                val userId = call.userId
                val orderId = call.parameters["id"]?.toLongOrNull()

                if (orderId == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid order ID"))
                    return@delete
                }

                val cancelled = orderService.cancelOrder(orderId, userId)

                if (cancelled) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Order cancelled successfully"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Cannot cancel order"))
                }
            }
        }
    }
}