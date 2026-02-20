package com.example.e2e

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import com.example.module
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthE2ETest : CoreE2ETest() {

    @Test
    fun `should register new user and return token`() = testApplication {
        application {
            module()
        }

        val randomEmail = "user${System.currentTimeMillis()}@example.com"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$randomEmail","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("token"))
        assertTrue(body.contains("user"))
    }

    @Test
    fun `should reject registration with short password`() = testApplication {
        application {
            module()
        }

        val randomEmail = "test${System.currentTimeMillis()}@example.com"
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$randomEmail","password":"123"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `should login with valid credentials`() = testApplication {
        application {
            module()
        }

        val randomEmail = "login${System.currentTimeMillis()}@example.com"
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$randomEmail","password":"password123"}""")
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$randomEmail","password":"password123"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = response.bodyAsText()
        assertTrue(body.contains("token"))
    }
}