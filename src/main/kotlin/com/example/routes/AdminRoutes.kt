package com.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import com.example.config.userRole
import com.example.data.repository.OrderRepository
import com.example.module.CreateProductRequest
import com.example.module.UpdateProductRequest
import com.example.module.UserRole
import com.example.service.ProductService

@Serializable
data class StatsResponse(
    val totalOrders: Long,
    val totalRevenue: String,
    val orders: List<com.example.module.Order>
)

fun Route.adminRoutes(productService: ProductService, orderRepository: OrderRepository) {
    authenticate("auth-jwt") {
        route("/admin") {
            // Добавление товара
            post("/products") {
                // Проверка роли админа
                if (call.userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                    return@post
                }

                val request = call.receive<CreateProductRequest>()

                if (request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Product name is required"))
                    return@post
                }

                val product = productService.createProduct(
                    request.name,
                    request.description,
                    request.price,
                    request.stock
                )

                call.respond(HttpStatusCode.Created, product)
            }

            // Обновление товара
            put("/products/{id}") {
                // Проверка роли админа
                if (call.userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                    return@put
                }

                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid product ID"))
                    return@put
                }

                val request = call.receive<UpdateProductRequest>()

                // Валидация price если указан
                if (request.price != null) {
                    try {
                        request.price.toBigDecimal()
                    } catch (_: NumberFormatException) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid price format. Price must be a valid number"))
                        return@put
                    }
                }

                // Валидация stock если указан
                if (request.stock != null && request.stock < 0) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Stock cannot be negative"))
                    return@put
                }

                // Валидация name если указан
                if (request.name != null && request.name.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Product name cannot be empty"))
                    return@put
                }

                val product = productService.updateProduct(
                    id,
                    request.name,
                    request.description,
                    request.price,
                    request.stock
                )

                if (product == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("Product not found"))
                } else {
                    call.respond(HttpStatusCode.OK, product)
                }
            }

            // Удаление товара
            delete("/products/{id}") {
                // Проверка роли админа
                if (call.userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                    return@delete
                }

                val id = call.parameters["id"]?.toLongOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid product ID"))
                    return@delete
                }

                val result = productService.deleteProduct(id)

                result.onSuccess {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Product deleted successfully"))
                }.onFailure { error ->
                    val statusCode = if (error.message?.contains("not found") == true) {
                        HttpStatusCode.NotFound
                    } else {
                        HttpStatusCode.Conflict
                    }
                    call.respond(statusCode, ErrorResponse(error.message ?: "Failed to delete product"))
                }
            }

            // Статистика заказов
            get("/stats/orders") {
                // Проверка роли админа
                if (call.userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ErrorResponse("Access denied"))
                    return@get
                }

                val totalOrders = orderRepository.countAll()
                val totalRevenue = orderRepository.getTotalRevenue()
                val allOrders = orderRepository.findAll()

                call.respond(HttpStatusCode.OK, StatsResponse(totalOrders, totalRevenue.toString(), allOrders))
            }
        }
    }
}