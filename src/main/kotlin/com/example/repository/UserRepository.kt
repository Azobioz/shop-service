package com.example.repository

import com.example.domain.User

interface UserRepository {
    suspend fun create(user: User): User
    suspend fun findByEmail(email: String): User?
    suspend fun findById(id: Int): User?
    suspend fun update(user: User): User
    suspend fun delete(id: Int): Boolean
}