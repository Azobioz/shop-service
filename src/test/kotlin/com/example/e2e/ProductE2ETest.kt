package com.example.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import com.example.module
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductE2ETest : CoreE2ETest() {

    @Test
    fun `should get all products`() = testApplication {
        application {
            System.setProperty("REDIS_HOST", redis.host)
            System.setProperty("REDIS_PORT", redis.getMappedPort(6379).toString())
            moduleForTest()
        }

        val response = client.get("/products")

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.startsWith("["))
    }

    @Test
    fun `should get product by id`() = testApplication {
        application {
            System.setProperty("REDIS_HOST", redis.host)
            System.setProperty("REDIS_PORT", redis.getMappedPort(6379).toString())
            moduleForTest()
        }

        val productsResponse = client.get("/products")
        val productsBody = productsResponse.bodyAsText()

        if (productsBody != "[]" && productsBody.contains("\"id\":")) {
            val productId = productsBody
                .substringAfter("\"id\":")
                .substringBefore(",")
                .trim()

            val response = client.get("/products/$productId")
            assertEquals(HttpStatusCode.OK, response.status)
        } else {
            assertEquals("[]", productsBody)
        }
    }
}