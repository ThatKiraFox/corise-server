package com.corise.dnd.routes

import com.corise.dnd.dto.ArmourCreateRequest
import com.corise.dnd.dto.ArmourUpdateRequest
import com.corise.dnd.dto.ClassCreateRequest
import com.corise.dnd.dto.ClassUpdateRequest
import com.corise.dnd.dto.NamedCreateRequest
import com.corise.dnd.dto.NamedUpdateRequest
import com.corise.dnd.dto.RaceCreateRequest
import com.corise.dnd.dto.RaceUpdateRequest
import com.corise.dnd.dto.SpellCreateRequest
import com.corise.dnd.dto.SpellUpdateRequest
import com.corise.dnd.dto.WeaponCreateRequest
import com.corise.dnd.dto.WeaponUpdateRequest
import com.corise.dnd.service.CatalogService
import com.corise.dnd.service.CustomContentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * POST /path, GET|PUT|DELETE /path/{id}. Список (GET /path) регистрируется отдельно.
 * Обработчики получают (userId, ...) - идентификатор берётся только из проверенного токена.
 */
private inline fun <reified C : Any, reified U : Any, reified R : Any> Route.crud(
    path: String,
    crossinline onGet: suspend (Long, Long) -> R,
    crossinline onCreate: suspend (Long, C) -> R,
    crossinline onUpdate: suspend (Long, Long, U) -> R,
    crossinline onDelete: suspend (Long, Long) -> Unit,
) {
    route(path) {
        post {
            call.respond(HttpStatusCode.Created, onCreate(call.currentUserId(), call.receive<C>()))
        }
        route("/{id}") {
            get {
                call.respond(HttpStatusCode.OK, onGet(call.currentUserId(), call.pathId("id")))
            }
            put {
                call.respond(
                    HttpStatusCode.OK,
                    onUpdate(call.currentUserId(), call.pathId("id"), call.receive<U>()),
                )
            }
            delete {
                onDelete(call.currentUserId(), call.pathId("id"))
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

fun Route.catalogRoutes(catalog: CatalogService, custom: CustomContentService) {
    authenticate(AUTH_JWT) {
        // ---------- Списки (стандартный контент + собственный) ----------
        get("/races") { call.respond(HttpStatusCode.OK, catalog.races(call.currentUserId())) }
        get("/cultures") { call.respond(HttpStatusCode.OK, catalog.cultures(call.currentUserId())) }
        get("/religions") { call.respond(HttpStatusCode.OK, catalog.religions(call.currentUserId())) }
        get("/backstories") { call.respond(HttpStatusCode.OK, catalog.backstories(call.currentUserId())) }
        get("/classes") { call.respond(HttpStatusCode.OK, catalog.classes(call.currentUserId())) }
        get("/weapons") { call.respond(HttpStatusCode.OK, catalog.weapons(call.currentUserId())) }
        get("/armours") { call.respond(HttpStatusCode.OK, catalog.armours(call.currentUserId())) }
        get("/skills") { call.respond(HttpStatusCode.OK, catalog.skills()) }
        get("/spells") {
            val page = catalog.spells(
                userId = call.currentUserId(),
                search = call.request.queryParameters["search"],
                type = call.request.queryParameters["type"],
                limit = call.queryInt("limit"),
                offset = call.queryLong("offset"),
            )
            call.respond(HttpStatusCode.OK, page)
        }

        // ---------- Пользовательский контент ----------
        crud<RaceCreateRequest, RaceUpdateRequest, com.corise.dnd.dto.RaceResponse>(
            "/races",
            onGet = { uid, id -> catalog.race(uid, id) },
            onCreate = { uid, body -> custom.createRace(uid, body) },
            onUpdate = { uid, id, body -> custom.updateRace(uid, id, body) },
            onDelete = { uid, id -> custom.deleteRace(uid, id) },
        )
        crud<NamedCreateRequest, NamedUpdateRequest, com.corise.dnd.dto.NamedResponse>(
            "/cultures",
            onGet = { uid, id -> catalog.culture(uid, id) },
            onCreate = { uid, body -> custom.createCulture(uid, body) },
            onUpdate = { uid, id, body -> custom.updateCulture(uid, id, body) },
            onDelete = { uid, id -> custom.deleteCulture(uid, id) },
        )
        crud<NamedCreateRequest, NamedUpdateRequest, com.corise.dnd.dto.NamedResponse>(
            "/religions",
            onGet = { uid, id -> catalog.religion(uid, id) },
            onCreate = { uid, body -> custom.createReligion(uid, body) },
            onUpdate = { uid, id, body -> custom.updateReligion(uid, id, body) },
            onDelete = { uid, id -> custom.deleteReligion(uid, id) },
        )
        crud<NamedCreateRequest, NamedUpdateRequest, com.corise.dnd.dto.NamedResponse>(
            "/backstories",
            onGet = { uid, id -> catalog.backstory(uid, id) },
            onCreate = { uid, body -> custom.createBackstory(uid, body) },
            onUpdate = { uid, id, body -> custom.updateBackstory(uid, id, body) },
            onDelete = { uid, id -> custom.deleteBackstory(uid, id) },
        )
        crud<ClassCreateRequest, ClassUpdateRequest, com.corise.dnd.dto.ClassResponse>(
            "/classes",
            onGet = { uid, id -> catalog.classById(uid, id) },
            onCreate = { uid, body -> custom.createClass(uid, body) },
            onUpdate = { uid, id, body -> custom.updateClass(uid, id, body) },
            onDelete = { uid, id -> custom.deleteClass(uid, id) },
        )
        crud<SpellCreateRequest, SpellUpdateRequest, com.corise.dnd.dto.SpellResponse>(
            "/spells",
            onGet = { uid, id -> catalog.spell(uid, id) },
            onCreate = { uid, body -> custom.createSpell(uid, body) },
            onUpdate = { uid, id, body -> custom.updateSpell(uid, id, body) },
            onDelete = { uid, id -> custom.deleteSpell(uid, id) },
        )
        crud<WeaponCreateRequest, WeaponUpdateRequest, com.corise.dnd.dto.WeaponResponse>(
            "/weapons",
            onGet = { uid, id -> catalog.weapon(uid, id) },
            onCreate = { uid, body -> custom.createWeapon(uid, body) },
            onUpdate = { uid, id, body -> custom.updateWeapon(uid, id, body) },
            onDelete = { uid, id -> custom.deleteWeapon(uid, id) },
        )
        crud<ArmourCreateRequest, ArmourUpdateRequest, com.corise.dnd.dto.ArmourResponse>(
            "/armours",
            onGet = { uid, id -> catalog.armour(uid, id) },
            onCreate = { uid, body -> custom.createArmour(uid, body) },
            onUpdate = { uid, id, body -> custom.updateArmour(uid, id, body) },
            onDelete = { uid, id -> custom.deleteArmour(uid, id) },
        )
    }
}
