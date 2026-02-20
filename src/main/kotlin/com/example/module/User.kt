package com.example.module

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long,
    val email: String,
    val role: UserRole,
    val createdAt: String
)

@Serializable
enum class UserRole {
    USER, ADMIN
}

@Serializable
data class UserCredentials(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: User
)
