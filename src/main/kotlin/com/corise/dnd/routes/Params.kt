package com.corise.dnd.routes

import com.corise.dnd.service.ValidationException
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.pathId(name: String): Long =
    parameters[name]?.toLongOrNull()
        ?: throw ValidationException(mapOf(name to "Некорректный идентификатор"))

fun ApplicationCall.queryLong(name: String): Long? {
    val raw = request.queryParameters[name] ?: return null
    return raw.toLongOrNull() ?: throw ValidationException(mapOf(name to "Должно быть числом"))
}

fun ApplicationCall.queryInt(name: String): Int? {
    val raw = request.queryParameters[name] ?: return null
    return raw.toIntOrNull() ?: throw ValidationException(mapOf(name to "Должно быть числом"))
}
