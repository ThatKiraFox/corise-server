package com.corise.dnd

import com.corise.dnd.config.AppConfig
import com.corise.dnd.database.DatabaseFactory
import com.corise.dnd.dto.ErrorResponse
import com.corise.dnd.repository.UserRepository
import com.corise.dnd.routes.AUTH_JWT
import com.corise.dnd.routes.authRoutes
import com.corise.dnd.security.JwtService
import com.corise.dnd.service.ApiException
import com.corise.dnd.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.ContentTransformationException
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun Application.module() {
    val config = AppConfig.from(environment.config)
    DatabaseFactory.init(config.db)

    val jwtService = JwtService(config.jwt)
    val authService = AuthService(UserRepository(), jwtService)

    install(CallLogging)
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    install(StatusPages) {
        exception<ApiException> { call, e ->
            call.respond(e.status, ErrorResponse(e.code, e.message, e.details))
        }
        // Битый JSON, пропущенное обязательное поле, неверный тип -> 422
        exception<BadRequestException> { call, _ -> call.respondInvalidBody() }
        exception<ContentTransformationException> { call, _ -> call.respondInvalidBody() }
        exception<SerializationException> { call, _ -> call.respondInvalidBody() }
        // Всё остальное: в лог - полностью, клиенту - без деталей (никаких traceback и SQL)
        exception<Throwable> { call, e ->
            call.application.log.error("Необработанная ошибка", e)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("internal_error", "Внутренняя ошибка сервера"),
            )
        }
    }

    install(Authentication) {
        jwt(AUTH_JWT) {
            realm = "corise"
            verifier(jwtService.verifier)
            validate { credential ->
                val id = credential.payload.subject?.toLongOrNull()
                // Токен удалённого пользователя перестаёт работать
                if (id != null && authService.userExists(id)) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("unauthorized", "Требуется авторизация"))
            }
        }
    }

    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        route("/api/v1") {
            authRoutes(authService)
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.respondInvalidBody() {
    respond(
        HttpStatusCode.UnprocessableEntity,
        ErrorResponse("validation_error", "Тело запроса некорректно: проверьте обязательные поля и типы"),
    )
}
