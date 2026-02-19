package com.example.service

import com.example.domain.Role
import com.example.domain.User
import com.example.dto.LoginRequest
import com.example.dto.RegisterRequest
import com.example.repository.UserRepository
import com.example.security.JwtService
import com.example.utils.PasswordHasher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class AuthServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val jwtService = mockk<JwtService>()
    private val authService = AuthService(userRepository, jwtService)

    @Test
    fun `register should create user and return token`() = runBlocking<Unit> {
        val request = RegisterRequest("testuser", "test@example.com", "password")
        coEvery { userRepository.findByEmail(request.email) } returns null
        val user = User(
            id = 1,
            username = request.username,
            email = request.email,
            passwordHash = PasswordHasher.hash(request.password),
            role = Role.USER
        )
        coEvery { userRepository.create(any()) } returns user
        coEvery { jwtService.generateToken(1, request.email, Role.USER.name) } returns "token123"

        val response = authService.register(request)

        assertEquals("token123", response.token)
        coVerify { userRepository.create(match { it.email == request.email }) }
    }

    @Test
    fun `register should throw if email exists`() = runBlocking<Unit> {
        val request = RegisterRequest("testuser", "test@example.com", "password")
        coEvery { userRepository.findByEmail(request.email) } returns mockk()

        assertThrows<IllegalArgumentException> {
            authService.register(request)
        }
    }

    @Test
    fun `login should return token for valid credentials`() = runBlocking<Unit> {
        val request = LoginRequest("test@example.com", "password")
        val user = User(
            id = 1,
            username = "testuser",
            email = request.email,
            passwordHash = PasswordHasher.hash(request.password),
            role = Role.USER
        )
        coEvery { userRepository.findByEmail(request.email) } returns user
        coEvery { jwtService.generateToken(1, request.email, Role.USER.name) } returns "token123"

        val response = authService.login(request)

        assertEquals("token123", response.token)
    }

    @Test
    fun `login should throw for invalid password`() = runBlocking<Unit> {
        val request = LoginRequest("test@example.com", "wrongpass")
        val user = User(
            id = 1,
            username = "testuser",
            email = request.email,
            passwordHash = PasswordHasher.hash("correctpass"),
            role = Role.USER
        )
        coEvery { userRepository.findByEmail(request.email) } returns user

        assertThrows<IllegalArgumentException> {
            authService.login(request)
        }
    }
}