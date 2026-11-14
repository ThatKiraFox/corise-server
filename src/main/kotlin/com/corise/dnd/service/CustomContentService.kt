package com.corise.dnd.service

import com.corise.dnd.database.dbQuery
import com.corise.dnd.dto.ArmourFields
import com.corise.dnd.dto.ArmourResponse
import com.corise.dnd.dto.ClassFields
import com.corise.dnd.dto.ClassResponse
import com.corise.dnd.dto.NamedFields
import com.corise.dnd.dto.NamedResponse
import com.corise.dnd.dto.RaceFields
import com.corise.dnd.dto.RaceResponse
import com.corise.dnd.dto.SpellFields
import com.corise.dnd.dto.SpellResponse
import com.corise.dnd.dto.WeaponFields
import com.corise.dnd.dto.WeaponResponse
import com.corise.dnd.model.ArmourEntity
import com.corise.dnd.model.ClassEntity
import com.corise.dnd.model.CultureEntity
import com.corise.dnd.model.BackstoryEntity
import com.corise.dnd.model.Items.category
import com.corise.dnd.model.Items.description
import com.corise.dnd.model.Items.name
import com.corise.dnd.model.Items.value
import com.corise.dnd.model.Items.weight
import com.corise.dnd.model.OwnedEntity
import com.corise.dnd.model.RaceEntity
import com.corise.dnd.model.ReligionEntity
import com.corise.dnd.model.SpellEntity
import com.corise.dnd.model.WeaponEntity
import com.corise.dnd.repository.CatalogRepository
import com.corise.dnd.repository.OwnedRepository

/**
 * Создание, изменение и удаление пользовательского контента (UC-14, UC-15).
 * Менять и удалять можно только собственный контент: чужой невидим (404),
 * стандартный виден, но защищён (403).
 */
class CustomContentService(private val catalog: CatalogRepository) {

    // ---------- Раса ----------

    suspend fun createRace(userId: Long, f: RaceFields): RaceResponse {
        validate(f)
        return dbQuery { catalog.races.create(userId) { fill(f) }.toResponse() }
    }

    suspend fun updateRace(userId: Long, id: Long, f: RaceFields): RaceResponse {
        validate(f)
        return dbQuery { editable(catalog.races, userId, id).apply { fill(f) }.toResponse() }
    }

    suspend fun deleteRace(userId: Long, id: Long) {
        dbQuery { editable(catalog.races, userId, id).delete() }
    }

    private fun validate(f: RaceFields) {
        val e = FieldErrors()
        e.text("outName", f.outName, 100)
        e.text("inName", f.inName, 100)
        e.text("shortDesc", f.shortDesc, TEXT_MAX, required = false)
        e.text("appearance", f.appearance, TEXT_MAX, required = false)
        e.text("lore", f.lore, TEXT_MAX, required = false)
        e.text("relationships", f.relationships, TEXT_MAX, required = false)
        e.text("status", f.status, 50, required = false)
        e.text("archetype", f.archetype, 50, required = false)
        e.positiveOrNull("lifespan", f.lifespan)
        e.throwIfAny()
    }

    private fun RaceEntity.fill(f: RaceFields) {
        outName = f.outName.trim()
        inName = f.inName.trim()
        shortDesc = f.shortDesc.trim()
        appearance = f.appearance.trim()
        lore = f.lore.trim()
        relationships = f.relationships.trim()
        status = f.status.trim()
        archetype = f.archetype.trim()
        lifespan = f.lifespan
    }

    // ---------- Культура ----------

    suspend fun createCulture(userId: Long, f: NamedFields): NamedResponse {
        validate(f)
        return dbQuery { catalog.cultures.create(userId) { fill(f) }.toResponse() }
    }

    suspend fun updateCulture(userId: Long, id: Long, f: NamedFields): NamedResponse {
        validate(f)
        return dbQuery { editable(catalog.cultures, userId, id).apply { fill(f) }.toResponse() }
    }

    suspend fun deleteCulture(userId: Long, id: Long) {
        dbQuery { editable(catalog.cultures, userId, id).delete() }
    }

    private fun CultureEntity.fill(f: NamedFields) {
        name = f.name.trim()
        description = f.description.trim()
    }

    // ---------- Религия ----------

    suspend fun createReligion(userId: Long, f: NamedFields): NamedResponse {
        validate(f)
        return dbQuery { catalog.religions.create(userId) { fill(f) }.toResponse() }
    }

    suspend fun updateReligion(userId: Long, id: Long, f: NamedFields): NamedResponse {
        validate(f)
        return dbQuery { editable(catalog.religions, userId, id).apply { fill(f) }.toResponse() }
    }

    suspend fun deleteReligion(userId: Long, id: Long) {
        dbQuery { editable(catalog.religions, userId, id).delete() }
    }

    private fun ReligionEntity.fill(f: NamedFields) {
        name = f.name.trim()
        description = f.description.trim()
    }

    // ---------- Предыстория ----------

    suspend fun createBackstory(userId: Long, f: NamedFields): NamedResponse {
        validate(f)
        return dbQuery { catalog.backstories.create(userId) { fill(f) }.toResponse() }
    }

    suspend fun updateBackstory(userId: Long, id: Long, f: NamedFields): NamedResponse {
        validate(f)
        return dbQuery { editable(catalog.backstories, userId, id).apply { fill(f) }.toResponse() }
    }

    suspend fun deleteBackstory(userId: Long, id: Long) {
        dbQuery { editable(catalog.backstories, userId, id).delete() }
    }

    private fun BackstoryEntity.fill(f: NamedFields) {
        name = f.name.trim()
        description = f.description.trim()
    }

    private fun validate(f: NamedFields) {
        val e = FieldErrors()
        e.text("name", f.name, 100)
        e.text("description", f.description, TEXT_MAX, required = false)
        e.throwIfAny()
    }

    // ---------- Класс ----------

    suspend fun createClass(userId: Long, f: ClassFields): ClassResponse {
        validate(f)
        return dbQuery { catalog.classes.create(userId) { fill(f) }.toResponse() }
    }

    suspend fun updateClass(userId: Long, id: Long, f: ClassFields): ClassResponse {
        validate(f)
        return dbQuery { editable(catalog.classes, userId, id).apply { fill(f) }.toResponse() }
    }

    suspend fun deleteClass(userId: Long, id: Long) {
        dbQuery { editable(catalog.classes, userId, id).delete() }
    }

    private fun validate(f: ClassFields) {
        val e = FieldErrors()
        e.text("name", f.name, 100)
        e.text("description", f.description, TEXT_MAX, required = false)
        e.text("primarySkills", f.primarySkills, TEXT_MAX, required = false)
        e.throwIfAny()
    }

    private fun ClassEntity.fill(f: ClassFields) {
        name = f.name.trim()
        socialStatus = f.socialStatus
        description = f.description.trim()
        primarySkills = f.primarySkills.trim()
    }

    // ---------- Заклинание ----------

    suspend fun createSpell(userId: Long, f: SpellFields): SpellResponse {
        validate(f)
        return dbQuery { catalog.spells.create(userId) { fill(f) }.toResponse() }
    }

    suspend fun updateSpell(userId: Long, id: Long, f: SpellFields): SpellResponse {
        validate(f)
        return dbQuery { editable(catalog.spells, userId, id).apply { fill(f) }.toResponse() }
    }

    suspend fun deleteSpell(userId: Long, id: Long) {
        dbQuery { editable(catalog.spells, userId, id).delete() }
    }

    private fun validate(f: SpellFields) {
        val e = FieldErrors()
        e.text("name", f.name, 100)
        e.text("time", f.time, 50, required = false)
        e.text("type", f.type, 50, required = false)
        e.text("effect", f.effect, TEXT_MAX, required = false)
        e.text("description", f.description, TEXT_MAX, required = false)
        e.nonNegative("distance", f.distance)
        e.nonNegative("cost", f.cost)
        e.throwIfAny()
    }

    private fun SpellEntity.fill(f: SpellFields) {
        name = f.name.trim()
        time = f.time.trim()
        type = f.type.trim()
        distance = f.distance
        effect = f.effect.trim()
        cost = f.cost
        description = f.description.trim()
    }

    // ---------- Оружие (предмет + оружие в одной транзакции) ----------

    suspend fun createWeapon(userId: Long, f: WeaponFields): WeaponResponse {
        validate(f)
        return dbQuery {
            val skill = f.weaponSkillId?.let {
                catalog.skill(it) ?: throw ValidationException(mapOf("weaponSkillId" to "Навык не найден"))
            }
            val item = catalog.items.create(userId) {
                name = f.name.trim()
                category = "weapon"
                weight = f.weight
                value = f.value
                description = f.description.trim()
            }
            catalog.createWeapon(item, f.damage.trim(), f.damageType.trim(), f.grip, skill).toResponse()
        }
    }

    suspend fun updateWeapon(userId: Long, id: Long, f: WeaponFields): WeaponResponse {
        validate(f)
        return dbQuery {
            val weapon = editableWeapon(userId, id)
            val skill = f.weaponSkillId?.let {
                catalog.skill(it) ?: throw ValidationException(mapOf("weaponSkillId" to "Навык не найден"))
            }
            if (f.grip != Grip.ONE_HANDED && catalog.weaponEquippedWithShield(weapon.id.value)) {
                throw ConflictException("Оружие надето у персонажа вместе со щитом: сначала снимите щит")
            }
            weapon.item.apply {
                name = f.name.trim()
                weight = f.weight
                value = f.value
                description = f.description.trim()
            }
            weapon.damage = f.damage.trim()
            weapon.damageType = f.damageType.trim()
            weapon.grip = f.grip
            weapon.weaponSkill = skill
            weapon.toResponse()
        }
    }

    suspend fun deleteWeapon(userId: Long, id: Long) {
        // Удаление предмета каскадом удаляет и строку оружия (ON DELETE CASCADE).
        dbQuery { editableWeapon(userId, id).item.delete() }
    }

    private fun editableWeapon(userId: Long, id: Long): WeaponEntity {
        val weapon = catalog.findWeapon(id, userId) ?: throw NotFoundException()
        if (!weapon.item.isOwnedBy(userId)) throw ForbiddenException(PROTECTED_MESSAGE)
        return weapon
    }

    private fun validate(f: WeaponFields) {
        val e = FieldErrors()
        e.text("name", f.name, 100)
        e.text("description", f.description, TEXT_MAX, required = false)
        e.text("damage", f.damage, 50)
        e.text("damageType", f.damageType, 50)
        e.oneOf("grip", f.grip, Grip.ALL)
        e.nonNegative("weight", f.weight)
        e.nonNegative("value", f.value)
        e.throwIfAny()
    }

    // ---------- Броня и щит ----------

    suspend fun createArmour(userId: Long, f: ArmourFields): ArmourResponse {
        validate(f)
        return dbQuery {
            val item = catalog.items.create(userId) {
                name = f.name.trim()
                category = armourCategory(f.armourType)
                weight = f.weight
                value = f.value
                description = f.description.trim()
            }
            catalog.createArmour(
                item, f.damageMitigation, f.requirements.trim(), f.armourType.trim(),
                f.disadvantages.trim(), f.resistance.trim(), f.vulnerability.trim(),
            ).toResponse()
        }
    }

    suspend fun updateArmour(userId: Long, id: Long, f: ArmourFields): ArmourResponse {
        validate(f)
        return dbQuery {
            val armour = editableArmour(userId, id)
            armour.item.apply {
                name = f.name.trim()
                category = armourCategory(f.armourType)
                weight = f.weight
                value = f.value
                description = f.description.trim()
            }
            armour.damageMitigation = f.damageMitigation
            armour.requirements = f.requirements.trim()
            armour.armourType = f.armourType.trim()
            armour.disadvantages = f.disadvantages.trim()
            armour.resistance = f.resistance.trim()
            armour.vulnerability = f.vulnerability.trim()
            armour.toResponse()
        }
    }

    suspend fun deleteArmour(userId: Long, id: Long) {
        dbQuery { editableArmour(userId, id).item.delete() }
    }

    private fun editableArmour(userId: Long, id: Long): ArmourEntity {
        val armour = catalog.findArmour(id, userId) ?: throw NotFoundException()
        if (!armour.item.isOwnedBy(userId)) throw ForbiddenException(PROTECTED_MESSAGE)
        return armour
    }

    private fun armourCategory(armourType: String): String =
        if (armourType.trim().lowercase() in setOf("shield", "щит")) "shield" else "armour"

    private fun validate(f: ArmourFields) {
        val e = FieldErrors()
        e.text("name", f.name, 100)
        e.text("armourType", f.armourType, 50)
        e.text("description", f.description, TEXT_MAX, required = false)
        e.text("requirements", f.requirements, TEXT_MAX, required = false)
        e.text("disadvantages", f.disadvantages, TEXT_MAX, required = false)
        e.text("resistance", f.resistance, TEXT_MAX, required = false)
        e.text("vulnerability", f.vulnerability, TEXT_MAX, required = false)
        e.nonNegative("damageMitigation", f.damageMitigation)
        e.nonNegative("weight", f.weight)
        e.nonNegative("value", f.value)
        e.throwIfAny()
    }

    // ---------- Общее ----------

    /** 404 - записи нет или она чужая пользовательская; 403 - стандартная (только чтение). */
    private fun <E : OwnedEntity> editable(repo: OwnedRepository<E>, userId: Long, id: Long): E {
        val entity = repo.findVisible(id, userId) ?: throw NotFoundException()
        if (!entity.isOwnedBy(userId)) throw ForbiddenException(PROTECTED_MESSAGE)
        return entity
    }

    private companion object {
        const val TEXT_MAX = 5000
        const val PROTECTED_MESSAGE = "Стандартный контент нельзя изменять или удалять"
    }
}
