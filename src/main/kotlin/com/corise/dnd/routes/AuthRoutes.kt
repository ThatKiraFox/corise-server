package com.corise.dnd.routes

import com.corise.dnd.dto.LoginRequest
import com.corise.dnd.dto.RegisterRequest
import com.corise.dnd.service.AuthService
import com.corise.dnd.service.UnauthorizedException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/** Id текущего пользователя из проверенного JWT. */
fun ApplicationCall.currentUserId(): Long =
    principal<JWTPrincipal>()?.payload?.subject?.toLongOrNull() ?: throw UnauthorizedException()

/** Router только принимает HTTP-запросы и отвечает; логика - в сервисе. */
fun Route.authRoutes(auth: AuthService) {
    route("/auth") {
        post("/register") {
            val response = auth.register(call.receive<RegisterRequest>())
            call.respond(HttpStatusCode.Created, response)
        }
        post("/login") {
            val response = auth.login(call.receive<LoginRequest>())
            call.respond(HttpStatusCode.OK, response)
        }
    }
    authenticate(AUTH_JWT) {
        get("/me") {
            call.respond(HttpStatusCode.OK, auth.me(call.currentUserId()))
        }
    }
}
