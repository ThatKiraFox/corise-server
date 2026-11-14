package com.corise.dnd

import com.corise.dnd.dto.ErrorResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthApiTest {

    @Test
    fun `регистрация возвращает 201 и рабочий токен`() = withApp { client ->
        val user = client.registerUser()

        val me = client.api(HttpMethod.Companion.Get, "/me", user.token)
        assertEquals(HttpStatusCode.Companion.OK, me.status)
        assertEquals(user.email, me.obj().str("email"))
    }

    @Test
    fun `вход с верным паролем даёт 200, а неверный пароль и неизвестный email дают одинаковый 401`() = withApp { client ->
        val user = client.registerUser()

        val ok = client.api(
            HttpMethod.Companion.Post, "/auth/login",
            body = """{"email":"${user.email}","password":"Passw0rd!123"}""",
        )
        assertEquals(HttpStatusCode.Companion.OK, ok.status)

        val wrongPassword = client.api(
            HttpMethod.Companion.Post, "/auth/login",
            body = """{"email":"${user.email}","password":"WrongPassword1"}""",
        )
        val unknownEmail = client.api(
            HttpMethod.Companion.Post, "/auth/login",
            body = """{"email":"nobody_${UUID.randomUUID()}@example.com","password":"Passw0rd!123"}""",
        )
        assertEquals(HttpStatusCode.Companion.Unauthorized, wrongPassword.status)
        assertEquals(HttpStatusCode.Companion.Unauthorized, unknownEmail.status)

        // ошибка входа всегда одинаковая: нельзя узнать, существует ли email
        val m1 = testJson.decodeFromString<ErrorResponse>(wrongPassword.bodyAsText()).message
        val m2 = testJson.decodeFromString<ErrorResponse>(unknownEmail.bodyAsText()).message
        assertEquals(m1, m2)
    }

    @Test
    fun `повторная регистрация с тем же email даёт 409`() = withApp { client ->
        val user = client.registerUser()
        val n = UUID.randomUUID().toString().take(8)

        val again = client.api(
            HttpMethod.Companion.Post, "/auth/register",
            body = registerBody(user.email, "other_$n"),
        )
        assertEquals(HttpStatusCode.Companion.Conflict, again.status)
    }

    @Test
    fun `короткий пароль и несовпадение паролей дают 422`() = withApp { client ->
        val n = UUID.randomUUID().toString().take(8)

        val shortPassword = client.api(
            HttpMethod.Companion.Post, "/auth/register",
            body = registerBody("s_$n@example.com", "short_$n", password = "abc"),
        )
        assertEquals(HttpStatusCode.Companion.UnprocessableEntity, shortPassword.status)

        val mismatch = client.api(
            HttpMethod.Companion.Post, "/auth/register",
            body = registerBody("m_$n@example.com", "mismatch_$n", password = "Passw0rd!123", confirm = "Different123"),
        )
        assertEquals(HttpStatusCode.Companion.UnprocessableEntity, mismatch.status)
    }

    @Test
    fun `запросы без токена и с поддельным токеном дают 401`() = withApp { client ->
        assertEquals(HttpStatusCode.Companion.Unauthorized, client.api(HttpMethod.Companion.Get, "/characters").status)
        assertEquals(
            HttpStatusCode.Companion.Unauthorized,
            client.api(HttpMethod.Companion.Get, "/characters", token = "not-a-real-token").status,
        )
    }
}