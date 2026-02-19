package com.example.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String
)