package com.corise.dnd.repository

import com.corise.dnd.model.ArmourEntity
import com.corise.dnd.model.BackstoryEntity
import com.corise.dnd.model.CharacterEntity
import com.corise.dnd.model.CharacterItemEntity
import com.corise.dnd.model.CharacterItems
import com.corise.dnd.model.Characters
import com.corise.dnd.model.CultureEntity
import com.corise.dnd.model.ItemEntity
import com.corise.dnd.model.Items
import com.corise.dnd.model.RaceEntity
import com.corise.dnd.model.ReligionEntity
import com.corise.dnd.model.UserEntity
import com.corise.dnd.model.WeaponEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import com.corise.dnd.dto.CharacterAttributeResponse
import com.corise.dnd.dto.CharacterSkillResponse
import com.corise.dnd.model.AttributeTable
import com.corise.dnd.model.CharacterAttributeTable
import com.corise.dnd.model.CharacterSkillTable
import com.corise.dnd.model.SkillTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import com.corise.dnd.dto.NamedRef
import com.corise.dnd.dto.PersonalityDto
import com.corise.dnd.dto.StatInput
import com.corise.dnd.model.CharacterClassTable
import com.corise.dnd.model.CharacterLanguageTable
import com.corise.dnd.model.ClassEntity
import com.corise.dnd.model.CultureLanguageTable
import com.corise.dnd.model.LanguageTable
import com.corise.dnd.model.PersonalityTable
import org.jetbrains.exposed.sql.batchInsert

class CharacterStats(
    val attributes: List<CharacterAttributeResponse>,
    val skills: List<CharacterSkillResponse>,
)

private class SkillRow(val id: Long, val name: String, val attributeId: Long)

class CharacterRepository {
    fun findById(id: Long): CharacterEntity? = CharacterEntity.findById(id)

    private fun filtered(userId: Long, search: String?, raceId: Long?) = CharacterEntity.find {
        var cond: Op<Boolean> = Characters.user eq userId
        if (search != null) cond = cond and (Characters.name.lowerCase() like likePattern(search))
        if (raceId != null) cond = cond and (Characters.race eq raceId)
        cond
    }

    /**
     * Страница персонажей пользователя: условия, сортировка, limit/offset.
     * Связи (раса, культура, религия, предыстория, заклинания, надетое снаряжение)
     * подгружаются пачкой через with() - один запрос на связь, а не по запросу на персонажа (без N+1).
     */
    fun page(
        userId: Long,
        search: String?,
        raceId: Long?,
        sortColumn: Column<*>,
        order: SortOrder,
        limit: Int,
        offset: Long,
    ): Pair<List<CharacterEntity>, Long> {
        val total = filtered(userId, search, raceId).count()
        val items = filtered(userId, search, raceId)
            .orderBy(sortColumn to order, Characters.id to SortOrder.ASC)
            .limit(limit)
            .offset(offset)
            .toList()
            .with(
                CharacterEntity::race,
                CharacterEntity::culture,
                CharacterEntity::religion,
                CharacterEntity::backstory,
                CharacterEntity::spells,
                CharacterEntity::equippedWeapon,
                CharacterEntity::equippedArmour,
                CharacterEntity::equippedShield,
            )
        // Предметы надетого снаряжения - тоже одним запросом на тип
        items.mapNotNull { it.equippedWeapon }.with(WeaponEntity::item)
        items.flatMap { listOfNotNull(it.equippedArmour, it.equippedShield) }.with(ArmourEntity::item)
        return items to total
    }

    fun create(
        userId: Long,
        name: String,
        race: RaceEntity,
        culture: CultureEntity?,
        religion: ReligionEntity?,
        backstory: BackstoryEntity?,
        level: Int,
        exp: Int,
        hpCur: Int,
        mlCur: Int,
    ): CharacterEntity = CharacterEntity.new {
        this.user = UserEntity[userId]
        this.name = name
        this.race = race
        this.culture = culture
        this.religion = religion
        this.backstory = backstory
        this.level = level
        this.exp = exp
        this.hpCur = hpCur
        this.mlCur = mlCur
    }

    // ---------- Инвентарь ----------

    /** Инвентарь сразу нескольких персонажей: один запрос на записи и один на их предметы. */
    fun inventoryFor(chars: List<CharacterEntity>): Map<Long, List<CharacterItemEntity>> {
        if (chars.isEmpty()) return emptyMap()
        val ids = chars.map { it.id }
        val rows = CharacterItemEntity.find { CharacterItems.character inList ids }
            .toList()
            .with(CharacterItemEntity::item)
        return rows.groupBy { it.readValues[CharacterItems.character].value }
    }

    fun inventoryOf(character: CharacterEntity): List<CharacterItemEntity> =
        inventoryFor(listOf(character))[character.id.value].orEmpty()

    fun findInventoryEntry(character: CharacterEntity, itemId: Long): CharacterItemEntity? =
        CharacterItemEntity.find {
            (CharacterItems.character eq character.id) and (CharacterItems.item eq EntityID(itemId, Items))
        }.firstOrNull()

    fun addInventory(character: CharacterEntity, item: ItemEntity, amount: Int): CharacterItemEntity =
        CharacterItemEntity.new {
            this.character = character
            this.item = item
            this.amount = amount
        }

    // ---------- Атрибуты и навыки ----------

    /**
     * Атрибуты и навыки сразу нескольких персонажей: четыре запроса на всех.
     * Для каждого персонажа возвращаются ВСЕ атрибуты и навыки; если значения нет в таблице, это 0.
     */
    fun statsFor(characterIds: List<Long>): Map<Long, CharacterStats> {
        if (characterIds.isEmpty()) return emptyMap()

        val attributes = AttributeTable.selectAll()
            .orderBy(AttributeTable.id to SortOrder.ASC)
            .map { it[AttributeTable.id].value to it[AttributeTable.name] }
        val skills = SkillTable.selectAll()
            .orderBy(SkillTable.attribute to SortOrder.ASC, SkillTable.id to SortOrder.ASC)
            .map { SkillRow(it[SkillTable.id].value, it[SkillTable.name], it[SkillTable.attribute]) }

        val attributeValues = CharacterAttributeTable.selectAll()
            .where { CharacterAttributeTable.character inList characterIds }
            .groupBy(
                { it[CharacterAttributeTable.character] },
                { it[CharacterAttributeTable.attribute] to it[CharacterAttributeTable.value] },
            )
            .mapValues { it.value.toMap() }
        val skillValues = CharacterSkillTable.selectAll()
            .where { CharacterSkillTable.character inList characterIds }
            .groupBy(
                { it[CharacterSkillTable.character] },
                { it[CharacterSkillTable.skill] to it[CharacterSkillTable.level] },
            )
            .mapValues { it.value.toMap() }

        return characterIds.associateWith { cid ->
            val av = attributeValues[cid].orEmpty()
            val sv = skillValues[cid].orEmpty()
            CharacterStats(
                attributes.map { (id, name) -> CharacterAttributeResponse(id, name, av[id] ?: 0) },
                skills.map { s -> CharacterSkillResponse(s.id, s.name, s.attributeId, sv[s.id] ?: 0) },
            )
        }
    }

    fun attributeExists(attributeId: Long): Boolean =
        !AttributeTable.selectAll().where { AttributeTable.id eq attributeId }.empty()

    fun skillExists(skillId: Long): Boolean =
        !SkillTable.selectAll().where { SkillTable.id eq skillId }.empty()

    /** Задаёт значение атрибута: обновляет запись или создаёт её (повторный вызов не дублирует). */
    fun setAttributeValue(characterId: Long, attributeId: Long, value: Int) {
        val updated = CharacterAttributeTable.update({
            (CharacterAttributeTable.character eq characterId) and (CharacterAttributeTable.attribute eq attributeId)
        }) { it[CharacterAttributeTable.value] = value }
        if (updated == 0) {
            CharacterAttributeTable.insert {
                it[CharacterAttributeTable.character] = characterId
                it[CharacterAttributeTable.attribute] = attributeId
                it[CharacterAttributeTable.value] = value
            }
        }
    }

    /** Задаёт уровень навыка: обновляет запись или создаёт её. */
    fun setSkillLevel(characterId: Long, skillId: Long, level: Int) {
        val updated = CharacterSkillTable.update({
            (CharacterSkillTable.character eq characterId) and (CharacterSkillTable.skill eq skillId)
        }) { it[CharacterSkillTable.level] = level }
        if (updated == 0) {
            CharacterSkillTable.insert {
                it[CharacterSkillTable.character] = characterId
                it[CharacterSkillTable.skill] = skillId
                it[CharacterSkillTable.level] = level
            }
        }
    }

    // ---------- Создание по правилам: очки, характер, языки, класс ----------

    fun attributeIds(): Set<Long> = AttributeTable.selectAll().map { it[AttributeTable.id].value }.toSet()

    fun skillIds(): Set<Long> = SkillTable.selectAll().map { it[SkillTable.id].value }.toSet()

    /** Стартовые значения нового персонажа одним запросом на таблицу; нулевые не записываются. */
    fun insertStats(characterId: Long, attributes: List<StatInput>, skills: List<StatInput>) {
        CharacterAttributeTable.batchInsert(
            attributes.filter { it.value > 0 }, shouldReturnGeneratedValues = false,
        ) { a ->
            this[CharacterAttributeTable.character] = characterId
            this[CharacterAttributeTable.attribute] = a.id
            this[CharacterAttributeTable.value] = a.value
        }
        CharacterSkillTable.batchInsert(
            skills.filter { it.value > 0 }, shouldReturnGeneratedValues = false,
        ) { s ->
            this[CharacterSkillTable.character] = characterId
            this[CharacterSkillTable.skill] = s.id
            this[CharacterSkillTable.level] = s.value
        }
    }

    fun personalityOf(characterId: Long): PersonalityDto? =
        PersonalityTable.selectAll().where { PersonalityTable.character eq characterId }.firstOrNull()?.let {
            PersonalityDto(
                biography = it[PersonalityTable.biography],
                goal = it[PersonalityTable.goal],
                uniqueTraits = it[PersonalityTable.uniqueTraits],
                personalityTraits = it[PersonalityTable.personalityTraits],
                personalValues = it[PersonalityTable.personalValues],
                personalEvils = it[PersonalityTable.personalEvils],
            )
        }

    /** Записывает блок характера: обновляет запись или создаёт её. */
    fun setPersonality(characterId: Long, p: PersonalityDto) {
        val updated = PersonalityTable.update({ PersonalityTable.character eq characterId }) {
            it[PersonalityTable.biography] = p.biography
            it[PersonalityTable.goal] = p.goal
            it[PersonalityTable.uniqueTraits] = p.uniqueTraits
            it[PersonalityTable.personalityTraits] = p.personalityTraits
            it[PersonalityTable.personalValues] = p.personalValues
            it[PersonalityTable.personalEvils] = p.personalEvils
        }
        if (updated == 0) {
            PersonalityTable.insert {
                it[PersonalityTable.character] = characterId
                it[PersonalityTable.biography] = p.biography
                it[PersonalityTable.goal] = p.goal
                it[PersonalityTable.uniqueTraits] = p.uniqueTraits
                it[PersonalityTable.personalityTraits] = p.personalityTraits
                it[PersonalityTable.personalValues] = p.personalValues
                it[PersonalityTable.personalEvils] = p.personalEvils
            }
        }
    }

    /** Персонаж по умолчанию знает языки своей культуры. */
    fun addCultureLanguages(characterId: Long, cultureId: Long) {
        val languageIds = CultureLanguageTable.selectAll()
            .where { CultureLanguageTable.culture eq cultureId }
            .map { it[CultureLanguageTable.language] }
        CharacterLanguageTable.batchInsert(languageIds, shouldReturnGeneratedValues = false) { languageId ->
            this[CharacterLanguageTable.character] = characterId
            this[CharacterLanguageTable.language] = languageId
        }
    }

    fun languagesOf(characterId: Long): List<NamedRef> {
        val ids = CharacterLanguageTable.selectAll()
            .where { CharacterLanguageTable.character eq characterId }
            .map { it[CharacterLanguageTable.language] }
        if (ids.isEmpty()) return emptyList()
        return LanguageTable.selectAll()
            .where { LanguageTable.id inList ids }
            .map { NamedRef(it[LanguageTable.id], it[LanguageTable.name]) }
            .sortedBy { it.name }
    }

    fun addClass(characterId: Long, classId: Long) {
        CharacterClassTable.insert {
            it[CharacterClassTable.character] = characterId
            it[CharacterClassTable.classId] = classId
            it[CharacterClassTable.classLevel] = 1
        }
    }

    fun classesOf(characterId: Long): List<NamedRef> {
        val ids = CharacterClassTable.selectAll()
            .where { CharacterClassTable.character eq characterId }
            .map { it[CharacterClassTable.classId] }
        if (ids.isEmpty()) return emptyList()
        return ClassEntity.forIds(ids).map { NamedRef(it.id.value, it.name) }.sortedBy { it.name }
    }
}
