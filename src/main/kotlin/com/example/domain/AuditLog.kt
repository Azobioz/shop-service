package com.example.domain

import java.time.Instant

data class AuditLog(
    val id: Int? = null,
    val userId: Int?,
    val action: String,
    val entityType: String,
    val entityId: Int?,
    val details: String?,
    val createdAt: Instant = Instant.now()
)