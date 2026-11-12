package com.corise.dnd.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.corise.dnd.config.JwtConfig
import java.util.Date

class JwtService(private val config: JwtConfig) {
    private val algorithm = Algorithm.HMAC256(config.secret)

    val expiresInSeconds: Long = config.expiresMinutes * 60

    /** Проверяет подпись, issuer, audience и срок действия. */
    val verifier: JWTVerifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .withAudience(config.audience)
        .build()

    fun createToken(userId: Long): String {
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .withSubject(userId.toString())
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expiresInSeconds * 1000))
            .sign(algorithm)
    }
}
