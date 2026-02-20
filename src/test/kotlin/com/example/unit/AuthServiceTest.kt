package com.example.unit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import com.example.data.repository.UserRepository
import com.example.module.User
import com.example.module.UserRole
import com.example.service.AuthService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val authService = AuthService(
        userRepository = userRepository,
        jwtSecret = "test-secret",
        jwtIssuer = "test-issuer",
        jwtAudience = "test-audience"
    )

    @Test
    fun `register should create user and return auth response`() {
        val email = "test@example.com"
        val password = "password123"
        val user = User(1, email, UserRole.USER, "2024-01-01")

        every { userRepository.findByEmail(email) } returns null
        every { userRepository.create(email, password, UserRole.USER) } returns user

        val result = authService.register(email, password)

        assertNotNull(result)
        assertEquals(user, result.user)
        assertNotNull(result.token)

        verify { userRepository.create(email, password, UserRole.USER) }
    }

    @Test
    fun `register should return null when user already exists`() {
        val email = "existing@example.com"
        val password = "password123"
        val existingUser = User(1, email, UserRole.USER, "2024-01-01")

        every { userRepository.findByEmail(email) } returns existingUser

        val result = authService.register(email, password)

        assertNull(result)
        verify(exactly = 0) { userRepository.create(any(), any(), any()) }
    }

    @Test
    fun `login should return auth response for valid credentials`() {
        val email = "test@example.com"
        val password = "password123"
        val user = User(1, email, UserRole.USER, "2024-01-01")

        every { userRepository.verifyPassword(email, password) } returns user

        val result = authService.login(email, password)

        assertNotNull(result)
        assertEquals(user, result.user)
        assertNotNull(result.token)
    }
}