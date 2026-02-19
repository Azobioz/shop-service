package com.example.controller

import com.example.dto.ProductRequest
import com.example.service.ProductService
import com.example.plugins.getUserId
import com.example.plugins.getUserRole
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes(productService: ProductService) {
    route("/products") {
        get {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
            val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
            val products = productService.getAllProducts(limit, offset)
            call.respond(HttpStatusCode.OK, products)
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            val product = productService.getProduct(id)
            if (product == null) {
                call.respond(HttpStatusCode.NotFound)
            } else {
                call.respond(HttpStatusCode.OK, product)
            }
        }

        authenticate("auth-jwt") {
            post {
                val role = getUserRole()
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden, "Admin access required")
                    return@post
                }
                val request = call.receive<ProductRequest>()
                val adminId = getUserId()
                val created = productService.createProduct(request, adminId)
                call.respond(HttpStatusCode.Created, created)
            }

            put("/{id}") {
                val role = getUserRole()
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden)
                    return@put
                }
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@put
                }
                val request = call.receive<ProductRequest>()
                val adminId = getUserId()
                val updated = productService.updateProduct(id, request, adminId)
                if (updated == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.OK, updated)
                }
            }

            delete("/{id}") {
                val role = getUserRole()
                if (role != "ADMIN") {
                    call.respond(HttpStatusCode.Forbidden)
                    return@delete
                }
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                val adminId = getUserId()
                val deleted = productService.deleteProduct(id, adminId)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}