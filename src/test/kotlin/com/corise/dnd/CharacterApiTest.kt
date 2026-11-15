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

    @Test
    fun `при создании текущее HP равно максимальному`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)

        // characterBody отправляет hpCur = 10, но сервер его игнорирует
        val body = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Эовин", raceId, level = 3)).obj()

        // Выносливость 0: 10 + 0 + 3 * 2
        assertEquals(16, body.long("hpMax"))
        assertEquals(16, body.long("hpCur"))
    }

    @Test
    fun `при создании магическая нагрузка равна нулю, максимум рассчитан`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)

        // characterBody отправляет mlCur = 5, но сервер его игнорирует
        val body = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Арвен", raceId, level = 3)).obj()

        // Навык и Выносливость 0: (3 + 0 + 0) * 10
        assertEquals(30, body.long("mlMax"))
        assertEquals(0, body.long("mlCur"))
    }
    private fun stats(ids: List<Long>, values: List<Int>) =
        ids.zip(values).joinToString(",", "[", "]") { """{"id":${it.first},"value":${it.second}}""" }

    private fun createBody(raceId: Long, cultureId: Long?, attributes: String, skills: String) =
        """{"name":"По правилам","raceId":$raceId,"cultureId":${cultureId ?: "null"},"level":1,"exp":0,""" +
                """"attributes":$attributes,"skills":$skills,""" +
                """"personality":{"goal":"Найти брата","personalValues":"Семья"}}"""

    @Test
    fun `создание по правилам сохраняет очки, характер и язык культуры, ровно 27 и 35 очков разрешены`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val cultureId = client.api(HttpMethod.Get, "/cultures", user.token).arr()
            .first { it.jsonObject.str("name") == "Науры" }.jsonObject.long("id")
        val probe = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Проба", raceId)).obj()
        val attrIds = probe.getValue("attributes").jsonArray.map { it.jsonObject.long("attributeId") }
        val skillIds = probe.getValue("skills").jsonArray.map { it.jsonObject.long("skillId") }

        // атрибуты 5 + 3 + 3 = 15 + 6 + 6 = 27 очков, навыки 7 по 5 = 35 очков
        val resp = client.api(
            HttpMethod.Post, "/characters", user.token,
            createBody(
                raceId, cultureId,
                stats(attrIds.take(3), listOf(5, 3, 3)),
                stats(skillIds.take(7), List(7) { 5 }),
            ),
        )
        assertEquals(HttpStatusCode.Created, resp.status)
        val body = resp.obj()
        assertEquals(5, body.attributeValue(attrIds[0]))
        assertEquals("Найти брата", body.getValue("personality").jsonObject.str("goal"))
        assertTrue(body.getValue("languages").jsonArray.any { it.jsonObject.str("name") == "Наурский" })
    }

    @Test
    fun `422 code`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val probe = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Проба", raceId)).obj()
        val attrIds = probe.getValue("attributes").jsonArray.map { it.jsonObject.long("attributeId") }
        val skillIds = probe.getValue("skills").jsonArray.map { it.jsonObject.long("skillId") }

        // 28 очков атрибутов: 5 + 3 + 3 + 1
        val attrOver = client.api(
            HttpMethod.Post, "/characters", user.token,
            createBody(raceId, null, stats(attrIds.take(4), listOf(5, 3, 3, 1)), "[]"),
        )
        assertEquals(HttpStatusCode.UnprocessableEntity, attrOver.status)

        // уровень атрибута выше 5
        val tooHigh = client.api(
            HttpMethod.Post, "/characters", user.token,
            createBody(raceId, null, stats(attrIds.take(1), listOf(6)), "[]"),
        )
        assertEquals(HttpStatusCode.UnprocessableEntity, tooHigh.status)

        // 36 очков навыков
        val skillOver = client.api(
            HttpMethod.Post, "/characters", user.token,
            createBody(raceId, null, "[]", stats(skillIds.take(8), listOf(5, 5, 5, 5, 5, 5, 5, 1))),
        )
        assertEquals(HttpStatusCode.UnprocessableEntity, skillOver.status)

        // в списке только пробный персонаж
        assertEquals(1, client.api(HttpMethod.Get, "/characters", user.token).obj().long("total"))
    }

    @Test
    fun `блок характера можно изменить`() = withApp { client ->
        val user = client.registerUser()
        val raceId = client.anyRaceId(user.token)
        val id = client.api(HttpMethod.Post, "/characters", user.token, characterBody("Галадриэль", raceId)).obj().long("id")

        val resp = client.api(HttpMethod.Put, "/characters/$id/personality", user.token, """{"goal":"Защитить лес"}""")
        assertEquals(HttpStatusCode.OK, resp.status)
        val after = client.api(HttpMethod.Get, "/characters/$id", user.token).obj()
        assertEquals("Защитить лес", after.getValue("personality").jsonObject.str("goal"))
    }
    @Test
    fun `справочник атрибутов доступен и не пуст`() = withApp { client ->
        val user = client.registerUser()
        val resp = client.api(HttpMethod.Get, "/attributes", user.token)
        assertEquals(HttpStatusCode.OK, resp.status)
        assertTrue(resp.arr().isNotEmpty())
    }
}

private fun kotlinx.serialization.json.JsonObject.attributeValue(attributeId: Long): Long =
    getValue("attributes").jsonArray
        .first { it.jsonObject.long("attributeId") == attributeId }.jsonObject.long("value")