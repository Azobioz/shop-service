package com.example.repository

import com.example.domain.AuditLog

interface AuditLogRepository {
    suspend fun create(log: AuditLog): AuditLog
    suspend fun findByUser(userId: Int, limit: Int, offset: Int): List<AuditLog>
}