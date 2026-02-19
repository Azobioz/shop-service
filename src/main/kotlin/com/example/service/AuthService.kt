package com.example.service

import com.example.domain.Role
import com.example.domain.User
import com.example.dto.AuthResponse
import com.example.dto.LoginRequest
import com.example.dto.RegisterRequest
import com.example.repository.UserRepository
import com.example.security.JwtService
import com.example.utils.PasswordHasher
import java.time.Instant

class AuthService(
    private val userRepository: UserRepository,
    private val jwtService: JwtService
) {
    suspend fun register(request: RegisterRequest): AuthResponse {
        val existing = userRepository.findByEmail(request.email)
        if (existing != null) {
            throw IllegalArgumentException("User with this email already exists")
        }
        val passwordHash = PasswordHasher.hash(request.password)
        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordHash,
            role = Role.USER
        )
        val created = userRepository.create(user)
        val token = jwtService.generateToken(created.id!!, created.email, created.role.name)
        return AuthResponse(token)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("Invalid email or password")
        if (!PasswordHasher.verify(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid email or password")
        }
        val token = jwtService.generateToken(user.id!!, user.email, user.role.name)
        return AuthResponse(token)
    }
}