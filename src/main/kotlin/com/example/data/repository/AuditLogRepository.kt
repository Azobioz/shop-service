package com.example.data.repository


import com.example.data.table.AuditLogs
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class AuditLogRepository {

    fun log(
        userId: Long?,
        action: String,
        entityType: String,
        entityId: Long? = null,
        details: String? = null
    ) = transaction {
        AuditLogs.insert {
            it[AuditLogs.userId] = userId
            it[AuditLogs.action] = action
            it[AuditLogs.entityType] = entityType
            it[AuditLogs.entityId] = entityId
            it[AuditLogs.details] = details
        }
    }
}