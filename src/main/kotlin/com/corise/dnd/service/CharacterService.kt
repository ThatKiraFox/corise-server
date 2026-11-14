package com.corise.dnd.service

import com.corise.dnd.database.dbQuery
import com.corise.dnd.dto.CharacterCreateRequest
import com.corise.dnd.dto.CharacterResponse
import com.corise.dnd.dto.CharacterUpdateRequest
import com.corise.dnd.dto.EquipmentRequest
import com.corise.dnd.dto.InventoryItemRequest
import com.corise.dnd.dto.PageResponse
import com.corise.dnd.dto.SetValueRequest
import com.corise.dnd.model.BackstoryEntity
import com.corise.dnd.model.CharacterEntity
import com.corise.dnd.model.Characters
import com.corise.dnd.model.CultureEntity
import com.corise.dnd.model.RaceEntity
import com.corise.dnd.model.ReligionEntity
import com.corise.dnd.repository.CatalogRepository
import com.corise.dnd.repository.CharacterRepository
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.SortOrder

const val MAX_STAT = 10

data class CharacterListQuery(
    val search: String?,
    val raceId: Long?,
    val sort: String?,
    val order: String?,
    val limit: Int?,
    val offset: Long?,
)

class CharacterService(
    private val characters: CharacterRepository,
    private val catalog: CatalogRepository,
) {
    private val sortable: Map<String, Column<*>> = mapOf(
        "name" to Characters.name,
        "level" to Characters.level,
        "id" to Characters.id,
    )

    private class Refs(
        val race: RaceEntity,
        val culture: CultureEntity?,
        val religion: ReligionEntity?,
        val backstory: BackstoryEntity?,
    )

    /** Ответ с инвентарём персонажа (инвентарь читается отдельным запросом). */
    /** Ответ с инвентарём, атрибутами и навыками персонажа (читаются отдельными запросами). */
    private fun CharacterEntity.full(): CharacterResponse {
        val stats = characters.statsFor(listOf(id.value)).getValue(id.value)
        return toResponse(characters.inventoryOf(this))
            .copy(attributes = stats.attributes, skills = stats.skills)
    }

    // ---------- Чтение ----------

    suspend fun list(userId: Long, q: CharacterListQuery): PageResponse<CharacterResponse> {
        val limit = q.limit ?: CatalogService.DEFAULT_LIMIT
        val offset = q.offset ?: 0L
        val sortKey = q.sort ?: "name"
        val orderKey = (q.order ?: "asc").lowercase()

        val errors = linkedMapOf<String, String>()
        if (limit !in 1..CatalogService.MAX_LIMIT) errors["limit"] = "От 1 до ${CatalogService.MAX_LIMIT}"
        if (offset < 0) errors["offset"] = "Не может быть отрицательным"
        if (sortKey !in sortable) errors["sort"] = "Допустимо: ${sortable.keys.joinToString()}"
        if (orderKey != "asc" && orderKey != "desc") errors["order"] = "Допустимо: asc, desc"
        if ((q.search?.length ?: 0) > 100) errors["search"] = "Не длиннее 100 символов"
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val search = q.search?.trim()?.takeIf { it.isNotEmpty() }
        val order = if (orderKey == "desc") SortOrder.DESC else SortOrder.ASC
        return dbQuery {
            val (items, total) = characters.page(userId, search, q.raceId, sortable.getValue(sortKey), order, limit, offset)
            val inventories = characters.inventoryFor(items)
            PageResponse(
                items.map { it.toResponse(inventories[it.id.value].orEmpty()) },
                total, limit, offset, offset + items.size < total,
            )
        }
    }

    suspend fun get(userId: Long, id: Long): CharacterResponse =
        dbQuery { owned(userId, id).full() }

    // ---------- Создание: персонаж + заклинания в одной транзакции ----------

    suspend fun create(userId: Long, req: CharacterCreateRequest): CharacterResponse {
        validateFields(req.name, req.level, req.exp, req.hpCur, req.mlCur)
        return dbQuery {
            val refs = resolveRefs(userId, req.raceId, req.cultureId, req.religionId, req.backstoryId)
            val spellIds = req.spellIds.distinct()
            val spells = catalog.spellsByIds(spellIds, userId)
            if (spells.size != spellIds.size) {
                val found = spells.map { it.id.value }.toSet()
                val missing = spellIds.filter { it !in found }
                throw ValidationException(mapOf("spellIds" to "Заклинания не найдены: ${missing.joinToString()}"))
            }
            // Любое исключение ниже откатывает всю транзакцию: персонажа без заклинаний не останется.
            val entity = characters.create(
                userId, req.name.trim(), refs.race, refs.culture, refs.religion, refs.backstory,
                req.level, req.exp, req.hpCur, req.mlCur,
            )
            entity.spells = SizedCollection(spells)
            entity.full()
        }
    }

    // ---------- Изменение / удаление ----------

    suspend fun update(userId: Long, id: Long, req: CharacterUpdateRequest): CharacterResponse {
        validateFields(req.name, req.level, req.exp, req.hpCur, req.mlCur)
        return dbQuery {
            val entity = owned(userId, id)
            val refs = resolveRefs(userId, req.raceId, req.cultureId, req.religionId, req.backstoryId)
            entity.name = req.name.trim()
            entity.race = refs.race
            entity.culture = refs.culture
            entity.religion = refs.religion
            entity.backstory = refs.backstory
            entity.level = req.level
            entity.exp = req.exp
            entity.hpCur = req.hpCur
            entity.mlCur = req.mlCur
            entity.full()
        }
    }

    suspend fun delete(userId: Long, id: Long) {
        dbQuery { owned(userId, id).delete() }
    }

    // ---------- Заклинания персонажа ----------

    suspend fun addSpell(userId: Long, id: Long, spellId: Long): CharacterResponse = dbQuery {
        val entity = owned(userId, id)
        val spell = catalog.spells.findVisible(spellId, userId)
            ?: throw ValidationException(mapOf("spellId" to "Заклинание не найдено"))
        if (entity.spells.any { it.id.value == spellId }) {
            throw ConflictException("У персонажа уже есть это заклинание")
        }
        entity.spells = SizedCollection(entity.spells.toList() + spell)
        entity.full()
    }

    suspend fun removeSpell(userId: Long, id: Long, spellId: Long) {
        dbQuery {
            val entity = owned(userId, id)
            val current = entity.spells.toList()
            if (current.none { it.id.value == spellId }) throw NotFoundException("У персонажа нет такого заклинания")
            entity.spells = SizedCollection(current.filter { it.id.value != spellId })
        }
    }

    // ---------- Инвентарь ----------

    /** Кладёт предмет в инвентарь или задаёт его количество (повторный вызов не дублирует запись). */
    suspend fun setInventoryItem(userId: Long, id: Long, itemId: Long, req: InventoryItemRequest): CharacterResponse {
        if (req.amount !in 1..MAX_AMOUNT) {
            throw ValidationException(mapOf("amount" to "От 1 до $MAX_AMOUNT"))
        }
        return dbQuery {
            val entity = owned(userId, id)
            val item = catalog.items.findVisible(itemId, userId) ?: throw NotFoundException("Предмет не найден")
            val entry = characters.findInventoryEntry(entity, itemId)
            if (entry != null) entry.amount = req.amount else characters.addInventory(entity, item, req.amount)
            entity.full()
        }
    }

    /** Убирает предмет из инвентаря; если он был надет, снимает его. */
    suspend fun removeInventoryItem(userId: Long, id: Long, itemId: Long) {
        dbQuery {
            val entity = owned(userId, id)
            val entry = characters.findInventoryEntry(entity, itemId)
                ?: throw NotFoundException("Этого предмета нет в инвентаре")
            entry.delete()
            if (entity.equippedWeapon?.item?.id?.value == itemId) entity.equippedWeapon = null
            if (entity.equippedArmour?.item?.id?.value == itemId) entity.equippedArmour = null
            if (entity.equippedShield?.item?.id?.value == itemId) entity.equippedShield = null
        }
    }

    // ---------- Экипировка ----------

    /**
     * Заменяет все три слота. Надеть можно только то, что лежит в инвентаре.
     * Щит нельзя использовать с двуручным или древковым оружием.
     */
    suspend fun setEquipment(userId: Long, id: Long, req: EquipmentRequest): CharacterResponse = dbQuery {
        val entity = owned(userId, id)
        val inInventory = characters.inventoryOf(entity).map { it.item.id.value }.toSet()
        val errors = FieldErrors()

        val weapon = req.weaponId?.let { weaponId ->
            val w = catalog.findWeapon(weaponId, userId)
            when {
                w == null -> { errors.add("weaponId", "Оружие не найдено"); null }
                w.item.id.value !in inInventory -> { errors.add("weaponId", "Этого оружия нет в инвентаре персонажа"); null }
                else -> w
            }
        }
        val armour = req.armourId?.let { armourId ->
            val a = catalog.findArmour(armourId, userId)
            when {
                a == null -> { errors.add("armourId", "Броня не найдена"); null }
                a.item.category == SHIELD_CATEGORY -> { errors.add("armourId", "Щит надевается в слот shieldId"); null }
                a.item.id.value !in inInventory -> { errors.add("armourId", "Этой брони нет в инвентаре персонажа"); null }
                else -> a
            }
        }
        val shield = req.shieldId?.let { shieldId ->
            val s = catalog.findArmour(shieldId, userId)
            when {
                s == null -> { errors.add("shieldId", "Щит не найден"); null }
                s.item.category != SHIELD_CATEGORY -> { errors.add("shieldId", "Это не щит"); null }
                s.item.id.value !in inInventory -> { errors.add("shieldId", "Этого щита нет в инвентаре персонажа"); null }
                else -> s
            }
        }
        if (shield != null && weapon != null && weapon.grip != Grip.ONE_HANDED) {
            errors.add("shieldId", "Щит нельзя использовать с двуручным или древковым оружием")
        }
        errors.throwIfAny()

        entity.equippedWeapon = weapon
        entity.equippedArmour = armour
        entity.equippedShield = shield
        entity.full()
    }

    // ---------- Атрибуты и навыки ----------

    /** Задаёт значение атрибута персонажа (0..10). */
    suspend fun setAttribute(userId: Long, id: Long, attributeId: Long, req: SetValueRequest): CharacterResponse {
        validateStatValue(req.value)
        return dbQuery {
            val entity = owned(userId, id)
            if (!characters.attributeExists(attributeId)) throw NotFoundException("Атрибут не найден")
            characters.setAttributeValue(entity.id.value, attributeId, req.value)
            entity.full()
        }
    }

    /** Задаёт уровень навыка персонажа (0..10). */
    suspend fun setSkill(userId: Long, id: Long, skillId: Long, req: SetValueRequest): CharacterResponse {
        validateStatValue(req.value)
        return dbQuery {
            val entity = owned(userId, id)
            if (!characters.skillExists(skillId)) throw NotFoundException("Навык не найден")
            characters.setSkillLevel(entity.id.value, skillId, req.value)
            entity.full()
        }
    }

    // ---------- Внутреннее ----------

    /** 404 - записи нет, 403 - запись есть, но принадлежит другому пользователю. */
    private fun owned(userId: Long, id: Long): CharacterEntity {
        val entity = characters.findById(id) ?: throw NotFoundException("Персонаж не найден")
        val ownerId: EntityID<Long> = entity.readValues[Characters.user]
        if (ownerId.value != userId) throw ForbiddenException("Нет доступа к этому персонажу")
        return entity
    }

    private fun resolveRefs(userId: Long, raceId: Long, cultureId: Long?, religionId: Long?, backstoryId: Long?): Refs {
        val errors = linkedMapOf<String, String>()
        val race = catalog.races.findVisible(raceId, userId)
        if (race == null) errors["raceId"] = "Раса не найдена"
        val culture = cultureId?.let { catalog.cultures.findVisible(it, userId) ?: run { errors["cultureId"] = "Культура не найдена"; null } }
        val religion = religionId?.let { catalog.religions.findVisible(it, userId) ?: run { errors["religionId"] = "Религия не найдена"; null } }
        val backstory = backstoryId?.let { catalog.backstories.findVisible(it, userId) ?: run { errors["backstoryId"] = "Предыстория не найдена"; null } }
        if (errors.isNotEmpty()) throw ValidationException(errors)
        return Refs(race!!, culture, religion, backstory)
    }

    private fun validateFields(name: String, level: Int, exp: Int, hpCur: Int, mlCur: Int) {
        val errors = linkedMapOf<String, String>()
        if (name.isBlank() || name.trim().length > 100) errors["name"] = "От 1 до 100 символов"
        if (level < 1) errors["level"] = "Не меньше 1"
        if (exp < 0) errors["exp"] = "Не может быть отрицательным"
        if (hpCur < 0) errors["hpCur"] = "Не может быть отрицательным"
        if (mlCur < 0) errors["mlCur"] = "Не может быть отрицательным"
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }

    private fun validateStatValue(value: Int) {
        if (value !in 0..MAX_STAT) throw ValidationException(mapOf("value" to "От 0 до $MAX_STAT"))
    }

    private companion object {
        const val MAX_AMOUNT = 9999
        const val SHIELD_CATEGORY = "shield"
    }
}
