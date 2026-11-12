package com.corise.dnd.config

import io.ktor.server.config.ApplicationConfig

data class DbConfig(val url: String, val user: String, val password: String)

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiresMinutes: Long,
)

data class AppConfig(val db: DbConfig, val jwt: JwtConfig) {
    companion object {
        fun from(config: ApplicationConfig): AppConfig {
            val jwt = JwtConfig(
                secret = config.property("jwt.secret").getString(),
                issuer = config.property("jwt.issuer").getString(),
                audience = config.property("jwt.audience").getString(),
                expiresMinutes = config.property("jwt.expiresMinutes").getString().toLong(),
            )
            require(jwt.secret.length >= 32) { "JWT_SECRET должен быть не короче 32 символов" }
            return AppConfig(
                db = DbConfig(
                    url = config.property("db.url").getString(),
                    user = config.property("db.user").getString(),
                    password = config.property("db.password").getString(),
                ),
                jwt = jwt,
            )
        }
    }
}
