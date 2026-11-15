package com.corise.dnd.routes

import com.corise.dnd.dto.AddSpellRequest
import com.corise.dnd.dto.CharacterCreateRequest
import com.corise.dnd.dto.CharacterUpdateRequest
import com.corise.dnd.service.CharacterListQuery
import com.corise.dnd.service.CharacterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import com.corise.dnd.dto.EquipmentRequest
import com.corise.dnd.dto.InventoryItemRequest
import com.corise.dnd.dto.PersonalityDto
import com.corise.dnd.dto.SetValueRequest

fun Route.characterRoutes(service: CharacterService) {
    authenticate(AUTH_JWT) {
        route("/characters") {
            get {
                val q = CharacterListQuery(
                    search = call.request.queryParameters["search"],
                    raceId = call.queryLong("raceId"),
                    sort = call.request.queryParameters["sort"],
                    order = call.request.queryParameters["order"],
                    limit = call.queryInt("limit"),
                    offset = call.queryLong("offset"),
                )
                call.respond(HttpStatusCode.OK, service.list(call.currentUserId(), q))
            }
            post {
                val created = service.create(call.currentUserId(), call.receive<CharacterCreateRequest>())
                call.respond(HttpStatusCode.Created, created)
            }
            route("/{id}") {
                get {
                    call.respond(HttpStatusCode.OK, service.get(call.currentUserId(), call.pathId("id")))
                }
                put {
                    val updated = service.update(
                        call.currentUserId(), call.pathId("id"), call.receive<CharacterUpdateRequest>(),
                    )
                    call.respond(HttpStatusCode.OK, updated)
                }
                delete {
                    service.delete(call.currentUserId(), call.pathId("id"))
                    call.respond(HttpStatusCode.NoContent)
                }
                route("/items/{itemId}") {
                    put {
                        val body = call.receive<InventoryItemRequest>()
                        val result = service.setInventoryItem(
                            call.currentUserId(), call.pathId("id"), call.pathId("itemId"), body,
                        )
                        call.respond(HttpStatusCode.OK, result)
                    }
                    delete {
                        service.removeInventoryItem(call.currentUserId(), call.pathId("id"), call.pathId("itemId"))
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
                put("/equipment") {
                    val body = call.receive<EquipmentRequest>()
                    val result = service.setEquipment(call.currentUserId(), call.pathId("id"), body)
                    call.respond(HttpStatusCode.OK, result)
                }
                put("/personality") {
                    val body = call.receive<PersonalityDto>()
                    val result = service.setPersonality(call.currentUserId(), call.pathId("id"), body)
                    call.respond(HttpStatusCode.OK, result)
                }
                route("/attributes/{attributeId}") {
                    put {
                        val body = call.receive<SetValueRequest>()
                        val result = service.setAttribute(
                            call.currentUserId(), call.pathId("id"), call.pathId("attributeId"), body,
                        )
                        call.respond(HttpStatusCode.OK, result)
                    }
                }
                route("/skills/{skillId}") {
                    put {
                        val body = call.receive<SetValueRequest>()
                        val result = service.setSkill(
                            call.currentUserId(), call.pathId("id"), call.pathId("skillId"), body,
                        )
                        call.respond(HttpStatusCode.OK, result)
                    }
                }
                route("/spells") {
                    post {
                        val body = call.receive<AddSpellRequest>()
                        val result = service.addSpell(call.currentUserId(), call.pathId("id"), body.spellId)
                        call.respond(HttpStatusCode.Created, result)
                    }
                    delete("/{spellId}") {
                        service.removeSpell(call.currentUserId(), call.pathId("id"), call.pathId("spellId"))
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }
        }
    }
}
