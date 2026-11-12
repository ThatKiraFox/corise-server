package com.corise.dnd.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable("users") {
    val email = varchar("email", 255)
    val username = varchar("username", 50)
    val passwordHash = varchar("password_hash", 100)
}

class UserEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<UserEntity>(Users)

    var email by Users.email
    var username by Users.username
    var passwordHash by Users.passwordHash
}
