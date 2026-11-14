package com.corise.dnd

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterApiTest {

    @Test
    fun `создание персонажа даёт 201 и возвращает его данные`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)

        val resp = client.api(HttpMethod.Companion.Post, "/characters", user.token, characterBody("Арагорн", raceId, level = 3))

        assertEquals(HttpStatusCode.Companion.Created, resp.status)
        val body = resp.obj()
        assertEquals("Арагорн", body.str("name"))
        assertEquals(3, body.long("level"))
        assertEquals(raceId, body.getValue("race").jsonObject.long("id"))
    }

    @Test
    fun `созданного персонажа можно получить по id и найти в списке`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Companion.Post, "/characters", user.token, characterBody("Боромир", raceId)).obj().long("id")

        val one = client.api(HttpMethod.Companion.Get, "/characters/$id", user.token)
        assertEquals(HttpStatusCode.Companion.OK, one.status)
        assertEquals("Боромир", one.obj().str("name"))

        val page = client.api(HttpMethod.Companion.Get, "/characters", user.token).obj()
        assertEquals(1, page.long("total")) // у нового пользователя только этот персонаж
    }

    @Test
    fun `изменение персонажа сохраняет новые значения`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Companion.Post, "/characters", user.token, characterBody("Гимли", raceId)).obj().long("id")

        val upd = client.api(HttpMethod.Companion.Put, "/characters/$id", user.token, characterBody("Гимли Сын Глоина", raceId, level = 5))
        assertEquals(HttpStatusCode.Companion.OK, upd.status)

        val after = client.api(HttpMethod.Companion.Get, "/characters/$id", user.token).obj()
        assertEquals("Гимли Сын Глоина", after.str("name"))
        assertEquals(5, after.long("level"))
    }

    @Test
    fun `удалённый персонаж больше недоступен`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Companion.Post, "/characters", user.token, characterBody("Леголас", raceId)).obj().long("id")

        val del = client.api(HttpMethod.Companion.Delete, "/characters/$id", user.token)
        assertEquals(HttpStatusCode.Companion.NoContent, del.status)

        assertEquals(
            HttpStatusCode.Companion.NotFound,
            client.api(HttpMethod.Companion.Get, "/characters/$id", user.token).status
        )
    }

    @Test
    fun `некорректные данные при создании дают 422`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)

        // нет обязательного поля raceId
        val noRace = client.api(HttpMethod.Companion.Post, "/characters", user.token, """{"name":"Без расы"}""")
        assertEquals(HttpStatusCode.Companion.UnprocessableEntity, noRace.status)

        // уровень меньше 1
        val badLevel = client.api(HttpMethod.Companion.Post, "/characters", user.token, characterBody("Нулевой", raceId, level = 0))
        assertEquals(HttpStatusCode.Companion.UnprocessableEntity, badLevel.status)
    }

    @Test
    fun `чужой персонаж недоступен другому пользователю (403)`() = withApp { client ->
        val owner = client.registerUser()
        val stranger = client.registerUser()
        val raceId = client.anyRaceId(owner.token)
        val id = client.api(HttpMethod.Companion.Post, "/characters", owner.token, characterBody("Фродо", raceId)).obj().long("id")

        assertEquals(
            HttpStatusCode.Companion.Forbidden,
            client.api(HttpMethod.Companion.Get, "/characters/$id", stranger.token).status
        )
        assertEquals(
            HttpStatusCode.Companion.Forbidden,
            client.api(HttpMethod.Companion.Delete, "/characters/$id", stranger.token).status
        )

        // персонаж владельца при этом на месте
        assertEquals(
            HttpStatusCode.Companion.OK,
            client.api(HttpMethod.Companion.Get, "/characters/$id", owner.token).status
        )
    }

    @Test
    fun `заклинание можно добавить персонажу и убрать`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Companion.Post, "/characters", user.token, characterBody("Гэндальф", raceId)).obj().long("id")

        val spells = client.api(HttpMethod.Companion.Get, "/spells", user.token).obj().getValue("items")
        val spellId = (spells as JsonArray).firstOrNull()?.jsonObject?.long("id")
        assertTrue(spellId != null, "В базе нет заклинаний: проверьте сид-данные миграций")

        val add = client.api(HttpMethod.Companion.Post, "/characters/$id/spells", user.token, """{"spellId":$spellId}""")
        assertTrue(add.status.isSuccess(), "Добавление заклинания: ${add.status}")
        val withSpell = client.api(HttpMethod.Companion.Get, "/characters/$id", user.token).obj()
        assertEquals(1, (withSpell.getValue("spells") as JsonArray).size)

        val remove = client.api(HttpMethod.Companion.Delete, "/characters/$id/spells/$spellId", user.token)
        assertTrue(remove.status.isSuccess(), "Удаление заклинания: ${remove.status}")
        val without = client.api(HttpMethod.Companion.Get, "/characters/$id", user.token).obj()
        assertEquals(0, (without.getValue("spells") as JsonArray).size)
    }

    @Test
    fun `в ответе есть все атрибуты и навыки с нулевыми значениями`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Бильбо", raceId)).obj().long("id")

        val body = client.api(HttpMethod.Get, "/characters/$id", user.token).obj()
        val attributes = body.getValue("attributes").jsonArray
        val skills = body.getValue("skills").jsonArray
        val catalogSkills = client.api(HttpMethod.Get, "/skills", user.token).arr()

        assertTrue(attributes.isNotEmpty(), "В ответе нет атрибутов")
        assertEquals(catalogSkills.size, skills.size)
        assertTrue(attributes.all { it.jsonObject.long("value") == 0L })
        assertTrue(skills.all { it.jsonObject.long("level") == 0L })
    }

    @Test
    fun `значение атрибута можно задать и изменить`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Торин", raceId)).obj().long("id")
        val attributeId = client.api(HttpMethod.Get, "/characters/$id", user.token).obj()
            .getValue("attributes").jsonArray.first().jsonObject.long("attributeId")

        val first = client.api(HttpMethod.Put, "/characters/$id/attributes/$attributeId", user.token, """{"value":7}""")
        assertEquals(HttpStatusCode.OK, first.status)
        assertEquals(7, first.obj().attributeValue(attributeId))

        // повторный вызов обновляет, а не дублирует запись
        val second = client.api(HttpMethod.Put, "/characters/$id/attributes/$attributeId", user.token, """{"value":3}""")
        assertEquals(HttpStatusCode.OK, second.status)
        val after = client.api(HttpMethod.Get, "/characters/$id", user.token).obj()
        assertEquals(3, after.attributeValue(attributeId))
        assertEquals(
            1,
            after.getValue("attributes").jsonArray.count { it.jsonObject.long("attributeId") == attributeId },
        )
    }

    @Test
    fun `уровень навыка можно задать`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Балин", raceId)).obj().long("id")
        val skillId = client.api(HttpMethod.Get, "/characters/$id", user.token).obj()
            .getValue("skills").jsonArray.first().jsonObject.long("skillId")

        val resp = client.api(HttpMethod.Put, "/characters/$id/skills/$skillId", user.token, """{"value":10}""")
        assertEquals(HttpStatusCode.OK, resp.status)
        val level = resp.obj().getValue("skills").jsonArray
            .first { it.jsonObject.long("skillId") == skillId }.jsonObject.long("level")
        assertEquals(10, level)
    }

    @Test
    fun `значение вне диапазона 0-10 и несуществующий атрибут дают ошибку`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Двалин", raceId)).obj().long("id")
        val attributeId = client.api(HttpMethod.Get, "/characters/$id", user.token).obj()
            .getValue("attributes").jsonArray.first().jsonObject.long("attributeId")

        val tooHigh = client.api(HttpMethod.Put, "/characters/$id/attributes/$attributeId", user.token, """{"value":11}""")
        assertEquals(HttpStatusCode.UnprocessableEntity, tooHigh.status)
        val negative = client.api(HttpMethod.Put, "/characters/$id/attributes/$attributeId", user.token, """{"value":-1}""")
        assertEquals(HttpStatusCode.UnprocessableEntity, negative.status)

        val unknown = client.api(HttpMethod.Put, "/characters/$id/attributes/999999", user.token, """{"value":5}""")
        assertEquals(HttpStatusCode.NotFound, unknown.status)
    }

    @Test
    fun `чужому персонажу нельзя менять атрибуты`() = withApp { client ->
        val owner = client.registerUser()
        val stranger = client.registerUser()
        val raceId = client.anyRaceId(owner.token)
        val id = client.api(HttpMethod.Post, "/characters", owner.token, characterBody("Оин", raceId)).obj().long("id")
        val attributeId = client.api(HttpMethod.Get, "/characters/$id", owner.token).obj()
            .getValue("attributes").jsonArray.first().jsonObject.long("attributeId")

        val resp = client.api(HttpMethod.Put, "/characters/$id/attributes/$attributeId", stranger.token, """{"value":5}""")
        assertEquals(HttpStatusCode.Forbidden, resp.status)
    }
}

private fun kotlinx.serialization.json.JsonObject.attributeValue(attributeId: Long): Long =
    getValue("attributes").jsonArray
        .first { it.jsonObject.long("attributeId") == attributeId }.jsonObject.long("value")