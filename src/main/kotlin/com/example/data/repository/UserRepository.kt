package com.example.data.repository

import com.example.data.table.Users
import com.example.module.User
import com.example.module.UserRole
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

class UserRepository {

    fun create(email: String, password: String, role: UserRole = UserRole.USER): User? = transaction {
        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())

        val userId = Users.insert {
            it[Users.email] = email
            it[Users.passwordHash] = hashedPassword
            it[Users.role] = role.name
        } get Users.id

        findById(userId.value)
    }

    fun findById(id: Long): User? = transaction {
        Users.selectAll().where { Users.id eq id }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    fun findByEmail(email: String): User? = transaction {
        Users.selectAll().where { Users.email eq email }
            .map { rowToUser(it) }
            .singleOrNull()
    }

    fun verifyPassword(email: String, password: String): User? = transaction {
        val row = Users.selectAll().where { Users.email eq email }.singleOrNull() ?: return@transaction null

        val storedHash = row[Users.passwordHash]
        if (BCrypt.checkpw(password, storedHash)) {
            rowToUser(row)
        } else {
            null
        }
    }

    private fun rowToUser(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[Users.email],
            role = UserRole.valueOf(row[Users.role]),
            createdAt = row[Users.createdAt].toString()
        )
    }
}