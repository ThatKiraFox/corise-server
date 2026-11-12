package com.corise.dnd.service

import io.ktor.http.HttpStatusCode

/** Ошибки, которые сервис сообщает клиенту. Внутренние исключения наружу не уходят. */
sealed class ApiException(
    val status: HttpStatusCode,
    val code: String,
    override val message: String,
    val details: Map<String, String>? = null,
) : RuntimeException(message)

class ValidationException(details: Map<String, String>) :
    ApiException(HttpStatusCode.UnprocessableEntity, "validation_error", "Некорректные данные запроса", details)

class ConflictException(message: String, details: Map<String, String>? = null) :
    ApiException(HttpStatusCode.Conflict, "conflict", message, details)

class UnauthorizedException(message: String = "Требуется авторизация") :
    ApiException(HttpStatusCode.Unauthorized, "unauthorized", message)

class ForbiddenException(message: String = "Недостаточно прав") :
    ApiException(HttpStatusCode.Forbidden, "forbidden", message)

class NotFoundException(message: String = "Не найдено") :
    ApiException(HttpStatusCode.NotFound, "not_found", message)
