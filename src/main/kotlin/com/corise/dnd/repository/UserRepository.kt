package com.corise.dnd.repository

import com.corise.dnd.model.UserEntity
import com.corise.dnd.model.Users
import org.jetbrains.exposed.sql.lowerCase

/** Только доступ к данным. Транзакцию открывает сервис (dbQuery). */
class UserRepository {
    fun findById(id: Long): UserEntity? = UserEntity.findById(id)

    fun findByEmail(email: String): UserEntity? =
        UserEntity.find { Users.email.lowerCase() eq email.lowercase() }.firstOrNull()

    fun existsByUsername(username: String): Boolean =
        !UserEntity.find { Users.username eq username }.empty()

    fun create(email: String, username: String, passwordHash: String): UserEntity =
        UserEntity.new {
            this.email = email
            this.username = username
            this.passwordHash = passwordHash
        }
}
