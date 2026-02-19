package com.example.repository.impl

import com.example.domain.AuditLog
import com.example.repository.AuditLogRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.ReferenceOption
import java.time.ZoneOffset

object AuditLogsTable : Table("audit_logs") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").nullable()
    val action = varchar("action", 255)
    val entityType = varchar("entity_type", 50)
    val entityId = integer("entity_id").nullable()
    val details = text("details").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)

    init {
        foreignKey(userId to UsersTable.id, onDelete = ReferenceOption.SET_NULL)
    }
}

class AuditLogRepositoryImpl : AuditLogRepository {
    override suspend fun create(log: AuditLog): AuditLog = newSuspendedTransaction {
        val insert = AuditLogsTable.insert {
            it[userId] = log.userId
            it[action] = log.action
            it[entityType] = log.entityType
            it[entityId] = log.entityId
            it[details] = log.details
        }
        log.copy(id = insert[AuditLogsTable.id])
    }

    override suspend fun findByUser(userId: Int, limit: Int, offset: Int): List<AuditLog> = newSuspendedTransaction {
        AuditLogsTable.select { AuditLogsTable.userId eq userId }
            .limit(limit, offset.toLong())
            .orderBy(AuditLogsTable.createdAt to SortOrder.DESC)
            .map { toAuditLog(it) }
    }

    private fun toAuditLog(row: ResultRow): AuditLog = AuditLog(
        id = row[AuditLogsTable.id],
        userId = row[AuditLogsTable.userId],
        action = row[AuditLogsTable.action],
        entityType = row[AuditLogsTable.entityType],
        entityId = row[AuditLogsTable.entityId],
        details = row[AuditLogsTable.details],
        createdAt = row[AuditLogsTable.createdAt].toInstant(ZoneOffset.UTC)
    )
}