package com.example.domain

import java.time.Instant

data class User(
    val id: Int? = null,
    val username: String,
    val email: String,
    val passwordHash: String,
    val role: Role = Role.USER,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class Role {
    USER, ADMIN
}