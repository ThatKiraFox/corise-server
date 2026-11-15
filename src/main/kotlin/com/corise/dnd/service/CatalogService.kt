package com.corise.dnd.service

import com.corise.dnd.database.dbQuery
import com.corise.dnd.dto.ArmourResponse
import com.corise.dnd.dto.ClassResponse
import com.corise.dnd.dto.LoreListItem
import com.corise.dnd.dto.LoreResponse
import com.corise.dnd.dto.NamedRef
import com.corise.dnd.dto.NamedResponse
import com.corise.dnd.dto.PageResponse
import com.corise.dnd.dto.RaceResponse
import com.corise.dnd.dto.SkillResponse
import com.corise.dnd.dto.SpellResponse
import com.corise.dnd.dto.WeaponResponse
import com.corise.dnd.model.Backstories
import com.corise.dnd.model.Classes
import com.corise.dnd.model.Cultures
import com.corise.dnd.model.Races
import com.corise.dnd.model.Religions
import com.corise.dnd.model.Spells
import com.corise.dnd.repository.CatalogRepository

/**
 * Чтение справочников. Пользователь видит стандартный контент и свой собственный;
 * чужой пользовательский контент для него не существует (404).
 */
class CatalogService(private val catalog: CatalogRepository) {

    suspend fun races(userId: Long): List<RaceResponse> =
        dbQuery { catalog.races.listVisible(userId, Races.outName).map { it.toResponse() } }

    suspend fun race(userId: Long, id: Long): RaceResponse =
        dbQuery { (catalog.races.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun cultures(userId: Long): List<NamedResponse> =
        dbQuery { catalog.cultures.listVisible(userId, Cultures.name).map { it.toResponse() } }

    suspend fun culture(userId: Long, id: Long): NamedResponse =
        dbQuery { (catalog.cultures.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun religions(userId: Long): List<NamedResponse> =
        dbQuery { catalog.religions.listVisible(userId, Religions.name).map { it.toResponse() } }

    suspend fun religion(userId: Long, id: Long): NamedResponse =
        dbQuery { (catalog.religions.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun backstories(userId: Long): List<NamedResponse> =
        dbQuery { catalog.backstories.listVisible(userId, Backstories.name).map { it.toResponse() } }

    suspend fun backstory(userId: Long, id: Long): NamedResponse =
        dbQuery { (catalog.backstories.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun classes(userId: Long): List<ClassResponse> =
        dbQuery { catalog.classes.listVisible(userId, Classes.name).map { it.toResponse() } }

    suspend fun classById(userId: Long, id: Long): ClassResponse =
        dbQuery { (catalog.classes.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun spell(userId: Long, id: Long): SpellResponse =
        dbQuery { (catalog.spells.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun spells(userId: Long, search: String?, type: String?, limit: Int?, offset: Long?): PageResponse<SpellResponse> {
        val lim = limit ?: DEFAULT_LIMIT
        val off = offset ?: 0L
        val errors = FieldErrors()
        if (lim !in 1..MAX_LIMIT) errors.add("limit", "От 1 до $MAX_LIMIT")
        if (off < 0) errors.add("offset", "Не может быть отрицательным")
        if ((search?.length ?: 0) > 100) errors.add("search", "Не длиннее 100 символов")
        errors.throwIfAny()

        val cleanSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val cleanType = type?.trim()?.takeIf { it.isNotEmpty() }
        return dbQuery {
            val (items, total) = catalog.spellsPage(userId, cleanSearch, cleanType, lim, off)
            PageResponse(items.map { it.toResponse() }, total, lim, off, off + items.size < total)
        }
    }

    suspend fun loreEntry(userId: Long, id: Long): LoreResponse =
        dbQuery { (catalog.lore.findVisible(id, userId) ?: notFound()).toResponse() }

    suspend fun loreList(userId: Long, search: String?, category: String?, limit: Int?, offset: Long?): PageResponse<LoreListItem> {
        val lim = limit ?: DEFAULT_LIMIT
        val off = offset ?: 0L
        val errors = FieldErrors()
        if (lim !in 1..MAX_LIMIT) errors.add("limit", "От 1 до $MAX_LIMIT")
        if (off < 0) errors.add("offset", "Не может быть отрицательным")
        if ((search?.length ?: 0) > 100) errors.add("search", "Не длиннее 100 символов")
        if ((category?.length ?: 0) > 50) errors.add("category", "Не длиннее 50 символов")
        errors.throwIfAny()

        val cleanSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val cleanCategory = category?.trim()?.takeIf { it.isNotEmpty() }
        return dbQuery {
            val (items, total) = catalog.lorePage(userId, cleanSearch, cleanCategory, lim, off)
            PageResponse(items.map { it.toListItem() }, total, lim, off, off + items.size < total)
        }
    }

    suspend fun skills(): List<SkillResponse> = dbQuery { catalog.skills().map { it.toResponse() } }

    suspend fun weapons(userId: Long): List<WeaponResponse> =
        dbQuery { catalog.weaponsVisible(userId).map { it.toResponse() } }

    suspend fun weapon(userId: Long, id: Long): WeaponResponse =
        dbQuery { (catalog.findWeapon(id, userId) ?: notFound()).toResponse() }

    suspend fun armours(userId: Long): List<ArmourResponse> =
        dbQuery { catalog.armoursVisible(userId).map { it.toResponse() } }

    suspend fun armour(userId: Long, id: Long): ArmourResponse =
        dbQuery { (catalog.findArmour(id, userId) ?: notFound()).toResponse() }

    private fun notFound(): Nothing = throw NotFoundException()

    suspend fun attributes(): List<NamedRef> = dbQuery { catalog.attributes() }

    companion object {
        const val DEFAULT_LIMIT = 20
        const val MAX_LIMIT = 100
    }
}
