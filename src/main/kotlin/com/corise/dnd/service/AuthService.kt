package com.corise.dnd.service

import at.favre.lib.crypto.bcrypt.BCrypt
import com.corise.dnd.database.dbQuery
import com.corise.dnd.dto.AuthResponse
import com.corise.dnd.dto.LoginRequest
import com.corise.dnd.dto.RegisterRequest
import com.corise.dnd.dto.UserResponse
import com.corise.dnd.model.UserEntity
import com.corise.dnd.repository.UserRepository
import com.corise.dnd.security.JwtService
import org.jetbrains.exposed.exceptions.ExposedSQLException

class AuthService(
    private val users: UserRepository,
    private val jwt: JwtService,
) {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    private val usernameRegex = Regex("^[A-Za-z0-9_.-]+$")

    // Хеш-пустышка: при неизвестном email проверяем пароль так же долго, как при известном.
    private val dummyHash: String = BCrypt.withDefaults().hashToString(12, "dummy-password".toCharArray())

    suspend fun register(req: RegisterRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        val username = req.username.trim()
        validateRegistration(email, username, req.password, req.confirmPassword)

        val hash = BCrypt.withDefaults().hashToString(12, req.password.toCharArray())
        val user = try {
            dbQuery {
                if (users.findByEmail(email) != null) {
                    throw ConflictException("Пользователь с таким email уже существует")
                }
                if (users.existsByUsername(username)) {
                    throw ConflictException("Это имя пользователя уже занято")
                }
                users.create(email, username, hash).toResponse()
            }
        } catch (e: ExposedSQLException) {
            // Гонка двух одинаковых регистраций: уникальный индекс в БД - последний рубеж.
            if (e.sqlState == "23505") throw ConflictException("Пользователь с такими данными уже существует")
            throw e
        }
        return authResponse(user)
    }

    suspend fun login(req: LoginRequest): AuthResponse {
        val email = req.email.trim().lowercase()
        val found = dbQuery { users.findByEmail(email)?.let { it.passwordHash to it.toResponse() } }

        val hashToCheck = found?.first ?: dummyHash
        val ok = BCrypt.verifyer().verify(req.password.toCharArray(), hashToCheck).verified
        if (found == null || !ok) throw UnauthorizedException("Неверный email или пароль")
        return authResponse(found.second)
    }

    suspend fun me(userId: Long): UserResponse =
        dbQuery { users.findById(userId)?.toResponse() } ?: throw UnauthorizedException()

    suspend fun userExists(userId: Long): Boolean = dbQuery { users.findById(userId) != null }

    private fun validateRegistration(email: String, username: String, password: String, confirm: String) {
        val errors = linkedMapOf<String, String>()
        if (email.length > 255 || !emailRegex.matches(email)) errors["email"] = "Некорректный email"
        if (username.length !in 3..50 || !usernameRegex.matches(username)) {
            errors["username"] = "От 3 до 50 символов: латиница, цифры, _ . -"
        }
        if (password.length < 8) {
            errors["password"] = "Минимум 8 символов"
        } else if (password.toByteArray().size > 72) {
            errors["password"] = "Слишком длинный пароль (максимум 72 байта)"
        }
        if (password != confirm) errors["confirmPassword"] = "Пароли не совпадают"
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun authResponse(user: UserResponse) = AuthResponse(
        token = jwt.createToken(user.id),
        expiresInSeconds = jwt.expiresInSeconds,
        user = user,
    )

    private fun UserEntity.toResponse() = UserResponse(id.value, email, username)
}
