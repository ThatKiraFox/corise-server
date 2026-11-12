package com.corise.dnd.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val confirmPassword: String,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UserResponse(val id: Long, val email: String, val username: String)

@Serializable
data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val user: UserResponse,
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null,
)
