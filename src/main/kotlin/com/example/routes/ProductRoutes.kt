package com.example.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.config.userId
import com.example.service.ProductService

fun Route.productRoutes(productService: ProductService) {
    route("/products") {
        // Публичный доступ к списку товаров
        get {
            val products = productService.getAllProducts()
            call.respond(HttpStatusCode.OK, products)
        }

        // Публичный доступ к конкретному товару
        get("/{id}") {
            val id = call.parameters["id"]?.toLongOrNull()

            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid product ID"))
                return@get
            }

            val product = productService.getProduct(id)

            if (product == null) {
                call.respond(HttpStatusCode.NotFound, ErrorResponse("Product not found"))
            } else {
                call.respond(HttpStatusCode.OK, product)
            }
        }
    }
}