package com.corise.dnd.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Characters : LongIdTable("character") {
    val user = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", 100)
    val race = reference("race_id", Races)
    val culture = reference("culture_id", Cultures).nullable()
    val religion = reference("religion_id", Religions).nullable()
    val backstory = reference("backstory_id", Backstories).nullable()
    val level = integer("level")
    val exp = integer("exp")
    val hpCur = integer("hp_cur")
    val mlCur = integer("ml_cur")
    val equippedWeapon = reference("equipped_weapon_id", Weapons, onDelete = ReferenceOption.SET_NULL).nullable()
    val equippedArmour = reference("equipped_armour_id", Armours, onDelete = ReferenceOption.SET_NULL).nullable()
    val equippedShield = reference("equipped_shield_id", Armours, onDelete = ReferenceOption.SET_NULL).nullable()
}

/** Связь N:M персонаж - заклинание (таблица создаётся миграцией, не Exposed). */
object CharacterSpells : Table("character_spells") {
    val character = reference("character_id", Characters, onDelete = ReferenceOption.CASCADE)
    val spell = reference("spell_id", Spells)
    override val primaryKey = PrimaryKey(character, spell)
}

class CharacterEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CharacterEntity>(Characters)

    var user by UserEntity referencedOn Characters.user
    var name by Characters.name
    var race by RaceEntity referencedOn Characters.race
    var culture by CultureEntity optionalReferencedOn Characters.culture
    var religion by ReligionEntity optionalReferencedOn Characters.religion
    var backstory by BackstoryEntity optionalReferencedOn Characters.backstory
    var level by Characters.level
    var exp by Characters.exp
    var hpCur by Characters.hpCur
    var mlCur by Characters.mlCur
    var equippedWeapon by WeaponEntity optionalReferencedOn Characters.equippedWeapon
    var equippedArmour by ArmourEntity optionalReferencedOn Characters.equippedArmour
    var equippedShield by ArmourEntity optionalReferencedOn Characters.equippedShield

    var spells by SpellEntity via CharacterSpells
}

/** Инвентарь персонажа (таблица character_items): предмет и количество. */
object CharacterItems : LongIdTable("character_items") {
    val character = reference("character_id", Characters, onDelete = ReferenceOption.CASCADE)
    val item = reference("item_id", Items)
    val amount = integer("amount")
}

class CharacterItemEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<CharacterItemEntity>(CharacterItems)

    var character by CharacterEntity referencedOn CharacterItems.character
    var item by ItemEntity referencedOn CharacterItems.item
    var amount by CharacterItems.amount
}
