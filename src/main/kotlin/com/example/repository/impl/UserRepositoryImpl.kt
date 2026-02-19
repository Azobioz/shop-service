package com.example.repository.impl

import com.example.domain.Role
import com.example.domain.User
import com.example.repository.UserRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.ZoneOffset

object UsersTable : Table("users") {
    val id = integer("id").autoIncrement()
    val username = varchar("username", 255)
    val email = varchar("email", 255)
    val passwordHash = varchar("password_hash", 255)
    val role = varchar("role", 50)
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

class UserRepositoryImpl : UserRepository {
    override suspend fun create(user: User): User = newSuspendedTransaction {
        val insert = UsersTable.insert {
            it[username] = user.username
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[role] = user.role.name
        }
        user.copy(id = insert[UsersTable.id])
    }

    override suspend fun findByEmail(email: String): User? = newSuspendedTransaction {
        UsersTable.select { UsersTable.email eq email }
            .map { toUser(it) }
            .singleOrNull()
    }

    override suspend fun findById(id: Int): User? = newSuspendedTransaction {
        UsersTable.select { UsersTable.id eq id }
            .map { toUser(it) }
            .singleOrNull()
    }

    override suspend fun update(user: User): User = newSuspendedTransaction {
        UsersTable.update({ UsersTable.id eq user.id!! }) {
            it[username] = user.username
            it[email] = user.email
            it[passwordHash] = user.passwordHash
            it[role] = user.role.name
            it[updatedAt] = LocalDateTime.now()
        }
        user
    }

    override suspend fun delete(id: Int): Boolean = newSuspendedTransaction {
        UsersTable.deleteWhere { UsersTable.id eq id } > 0
    }

    private fun toUser(row: ResultRow): User = User(
        id = row[UsersTable.id],
        username = row[UsersTable.username],
        email = row[UsersTable.email],
        passwordHash = row[UsersTable.passwordHash],
        role = Role.valueOf(row[UsersTable.role]),
        createdAt = row[UsersTable.createdAt].toInstant(ZoneOffset.UTC),
        updatedAt = row[UsersTable.updatedAt].toInstant(ZoneOffset.UTC)
    )
}