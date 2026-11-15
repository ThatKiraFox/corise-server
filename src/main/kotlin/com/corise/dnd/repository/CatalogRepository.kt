package com.corise.dnd.repository

import com.corise.dnd.model.ArmourEntity
import com.corise.dnd.model.Armours
import com.corise.dnd.model.BackstoryEntity
import com.corise.dnd.model.Backstories
import com.corise.dnd.model.CharacterEntity
import com.corise.dnd.model.Characters
import com.corise.dnd.model.ClassEntity
import com.corise.dnd.model.CultureEntity
import com.corise.dnd.model.Cultures
import com.corise.dnd.model.ItemEntity
import com.corise.dnd.model.Items
import com.corise.dnd.model.RaceEntity
import com.corise.dnd.model.Races
import com.corise.dnd.model.ReligionEntity
import com.corise.dnd.model.Religions
import com.corise.dnd.model.SkillEntity
import com.corise.dnd.model.SpellEntity
import com.corise.dnd.model.Spells
import com.corise.dnd.model.WeaponEntity
import com.corise.dnd.model.Weapons
import com.corise.dnd.model.Classes
import com.corise.dnd.model.LoreEntities
import com.corise.dnd.model.LoreEntity
import com.corise.dnd.model.visibleTo
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.with
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import com.corise.dnd.dto.NamedRef
import com.corise.dnd.model.AttributeTable

class CatalogRepository {
    val races = OwnedRepository(Races, RaceEntity)
    val cultures = OwnedRepository(Cultures, CultureEntity)
    val religions = OwnedRepository(Religions, ReligionEntity)
    val backstories = OwnedRepository(Backstories, BackstoryEntity)
    val classes = OwnedRepository(Classes, ClassEntity)
    val spells = OwnedRepository(Spells, SpellEntity)
    val items = OwnedRepository(Items, ItemEntity)
    val lore = OwnedRepository(LoreEntities, LoreEntity)

    // ---------- Заклинания: поиск, фильтр, страницы ----------

    fun spellsByIds(ids: List<Long>, userId: Long): List<SpellEntity> =
        if (ids.isEmpty()) emptyList() else SpellEntity.forIds(ids).filter { it.isVisibleTo(userId) }

    private fun spellsFiltered(userId: Long, search: String?, type: String?): org.jetbrains.exposed.sql.SizedIterable<SpellEntity> {
        var cond: Op<Boolean> = visibleTo(Spells, userId)
        if (search != null) cond = cond and (Spells.name.lowerCase() like likePattern(search))
        if (type != null) cond = cond and (Spells.type eq type)
        return SpellEntity.find(cond)
    }

    fun spellsPage(userId: Long, search: String?, type: String?, limit: Int, offset: Long): Pair<List<SpellEntity>, Long> {
        val total = spellsFiltered(userId, search, type).count()
        val items = spellsFiltered(userId, search, type)
            .orderBy(Spells.name to SortOrder.ASC, Spells.id to SortOrder.ASC)
            .limit(limit)
            .offset(offset)
            .toList()
        return items to total
    }

    // ---------- Лор: поиск, категория, страницы ----------

    private fun loreFiltered(userId: Long, search: String?, category: String?): org.jetbrains.exposed.sql.SizedIterable<LoreEntity> {
        var cond: Op<Boolean> = visibleTo(LoreEntities, userId)
        if (search != null) cond = cond and (LoreEntities.title.lowerCase() like likePattern(search))
        if (category != null) cond = cond and (LoreEntities.category eq category)
        return LoreEntity.find(cond)
    }

    fun lorePage(userId: Long, search: String?, category: String?, limit: Int, offset: Long): Pair<List<LoreEntity>, Long> {
        val total = loreFiltered(userId, search, category).count()
        val items = loreFiltered(userId, search, category)
            .orderBy(
                LoreEntities.category to SortOrder.ASC,
                LoreEntities.title to SortOrder.ASC,
                LoreEntities.id to SortOrder.ASC,
            )
            .limit(limit)
            .offset(offset)
            .toList()
        return items to total
    }

    // ---------- Навыки ----------

    fun skills(): List<SkillEntity> = SkillEntity.all().toList()
    fun skill(id: Long): SkillEntity? = SkillEntity.findById(id)
    fun attributes(): List<NamedRef> =
        AttributeTable.selectAll().orderBy(AttributeTable.id to SortOrder.ASC)
            .map { NamedRef(it[AttributeTable.id].value, it[AttributeTable.name]) }

    // ---------- Оружие и броня (владелец - у связанного предмета) ----------

    fun weaponsVisible(userId: Long): List<WeaponEntity> =
        WeaponEntity.wrapRows(
            Weapons.innerJoin(Items).selectAll().where { visibleTo(Items, userId) }.orderBy(Items.name to SortOrder.ASC),
        ).toList().with(WeaponEntity::item)

    fun findWeapon(id: Long, userId: Long): WeaponEntity? =
        WeaponEntity.findById(id)?.takeIf { it.item.isVisibleTo(userId) }

    fun createWeapon(item: ItemEntity, damage: String, damageType: String, grip: String, skill: SkillEntity?): WeaponEntity =
        WeaponEntity.new {
            this.item = item
            this.damage = damage
            this.damageType = damageType
            this.grip = grip
            this.weaponSkill = skill
        }

    /** Надето ли это оружие у какого-либо персонажа вместе со щитом. */
    fun weaponEquippedWithShield(weaponId: Long): Boolean =
        !CharacterEntity.find {
            (Characters.equippedWeapon eq EntityID(weaponId, Weapons)) and Characters.equippedShield.isNotNull()
        }.empty()

    fun armoursVisible(userId: Long): List<ArmourEntity> =
        ArmourEntity.wrapRows(
            Armours.innerJoin(Items).selectAll().where { visibleTo(Items, userId) }.orderBy(Items.name to SortOrder.ASC),
        ).toList().with(ArmourEntity::item)

    fun findArmour(id: Long, userId: Long): ArmourEntity? =
        ArmourEntity.findById(id)?.takeIf { it.item.isVisibleTo(userId) }

    fun createArmour(
        item: ItemEntity,
        damageMitigation: Int,
        requirements: String,
        armourType: String,
        disadvantages: String,
        resistance: String,
        vulnerability: String,
    ): ArmourEntity = ArmourEntity.new {
        this.item = item
        this.damageMitigation = damageMitigation
        this.requirements = requirements
        this.armourType = armourType
        this.disadvantages = disadvantages
        this.resistance = resistance
        this.vulnerability = vulnerability
    }
}
