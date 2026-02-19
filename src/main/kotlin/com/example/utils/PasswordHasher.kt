package com.example.utils

import java.security.MessageDigest
import java.util.*

object PasswordHasher {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }

    fun verify(password: String, hash: String): Boolean {
        return hash(password) == hash
    }
}