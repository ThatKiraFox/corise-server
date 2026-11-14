package com.corise.dnd

import com.corise.dnd.config.loadDotEnv
import com.corise.dnd.dto.AuthResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.UUID

val testJson = Json { ignoreUnknownKeys = true }

/** Настройки тестовой БД. Рабочая база `corise` в тестах не используется. */
object TestEnv {
    init { loadDotEnv() }

    private fun prop(key: String): String? = System.getenv(key) ?: System.getProperty(key)

    val dbUrl: String = prop("TEST_DB_URL")
        ?: prop("DB_URL")?.substringBeforeLast("/")?.plus("/corise_test")
        ?: "jdbc:postgresql://localhost:5432/corise_test"
    val dbUser: String = prop("TEST_DB_USER") ?: prop("DB_USER") ?: "postgres"
    val dbPassword: String = prop("TEST_DB_PASSWORD") ?: prop("DB_PASSWORD") ?: ""

    init {
        require(dbUrl.contains("test")) { "Тесты должны идти на тестовой БД, а не на рабочей: $dbUrl" }
    }
}

private fun testConfig() = MapApplicationConfig(
    "db.url" to TestEnv.dbUrl,
    "db.user" to TestEnv.dbUser,
    "db.password" to TestEnv.dbPassword,
    "jwt.secret" to "test-secret-test-secret-test-secret-123456",
    "jwt.issuer" to "corise-test",
    "jwt.audience" to "corise-test-users",
    "jwt.expiresMinutes" to "60",
)

/** Поднимает приложение на corise_test (Flyway накатывает миграции сам) и даёт HTTP-клиент. */
fun withApp(block: suspend (HttpClient) -> Unit) = testApplication {
    environment { config = testConfig() }
    application { module() }
    block(client)
}

suspend fun HttpClient.api(
    method: HttpMethod,
    path: String,
    token: String? = null,
    body: String? = null,
): HttpResponse = request("/api/v1$path") {
    this.method = method
    if (token != null) header(HttpHeaders.Authorization, "Bearer $token")
    if (body != null) {
        contentType(ContentType.Application.Json)
        setBody(body)
    }
}

suspend fun HttpResponse.obj(): JsonObject = testJson.parseToJsonElement(bodyAsText()).jsonObject
suspend fun HttpResponse.arr(): JsonArray = testJson.parseToJsonElement(bodyAsText()).jsonArray
fun JsonObject.long(key: String): Long = getValue(key).jsonPrimitive.long
fun JsonObject.str(key: String): String = getValue(key).jsonPrimitive.content

class TestUser(val token: String, val id: Long, val email: String)

fun registerBody(email: String, username: String, password: String = "Passw0rd!123", confirm: String = password) =
    """{"email":"$email","username":"$username","password":"$password","confirmPassword":"$confirm"}"""

/** Регистрирует нового пользователя со случайным email: тесты не мешают друг другу. */
suspend fun HttpClient.registerUser(): TestUser {
    val n = UUID.randomUUID().toString().take(8)
    val email = "u_$n@example.com"
    val resp = api(HttpMethod.Post, "/auth/register", body = registerBody(email, "user_$n"))
    check(resp.status.value == 201) { "Регистрация в тесте не удалась: ${resp.status} ${resp.bodyAsText()}" }
    val auth = testJson.decodeFromString<AuthResponse>(resp.bodyAsText())
    return TestUser(auth.token, auth.user.id, email)
}

/** Id любой расы из сид-данных. */
suspend fun HttpClient.anyRaceId(token: String): Long {
    val races = api(HttpMethod.Get, "/races", token).arr()
    check(races.isNotEmpty()) { "В базе нет рас: проверьте сид-данные миграций" }
    return races.first().jsonObject.long("id")
}

fun characterBody(name: String, raceId: Long, level: Int = 1) =
    """{"name":"$name","raceId":$raceId,"cultureId":null,"religionId":null,"backstoryId":null,""" +
            """"level":$level,"exp":0,"hpCur":10,"mlCur":5}"""