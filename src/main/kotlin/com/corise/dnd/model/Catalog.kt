package com.corise.dnd.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ReferenceOption

// ---------- Раса ----------
object Races : OwnedTable("race") {
    val outName = varchar("out_name", 100)
    val inName = varchar("in_name", 100)
    val shortDesc = text("short_desc")
    val appearance = text("appearance")
    val lore = text("lore")
    val relationships = text("relationships")
    val status = varchar("status", 50)
    val archetype = varchar("archetype", 50)
    val lifespan = integer("lifespan").nullable()
}

class RaceEntity(id: EntityID<Long>) : OwnedEntity(id, Races) {
    companion object : LongEntityClass<RaceEntity>(Races)

    var outName by Races.outName
    var inName by Races.inName
    var shortDesc by Races.shortDesc
    var appearance by Races.appearance
    var lore by Races.lore
    var relationships by Races.relationships
    var status by Races.status
    var archetype by Races.archetype
    var lifespan by Races.lifespan
}

// ---------- Культура, религия, предыстория ----------
object Cultures : OwnedTable("culture") {
    val name = varchar("name", 100)
    val description = text("description")
}

class CultureEntity(id: EntityID<Long>) : OwnedEntity(id, Cultures) {
    companion object : LongEntityClass<CultureEntity>(Cultures)

    var name by Cultures.name
    var description by Cultures.description
}

object Religions : OwnedTable("religion") {
    val name = varchar("name", 100)
    val description = text("description")
}

class ReligionEntity(id: EntityID<Long>) : OwnedEntity(id, Religions) {
    companion object : LongEntityClass<ReligionEntity>(Religions)

    var name by Religions.name
    var description by Religions.description
}

object Backstories : OwnedTable("backstory") {
    val name = varchar("name", 100)
    val description = text("description")
}

class BackstoryEntity(id: EntityID<Long>) : OwnedEntity(id, Backstories) {
    companion object : LongEntityClass<BackstoryEntity>(Backstories)

    var name by Backstories.name
    var description by Backstories.description
}

// ---------- Класс ----------
object Classes : OwnedTable("class") {
    val name = varchar("name", 100)
    val socialStatus = integer("social_status")
    val description = text("description")
    val primarySkills = text("primary_skills")
}

class ClassEntity(id: EntityID<Long>) : OwnedEntity(id, Classes) {
    companion object : LongEntityClass<ClassEntity>(Classes)

    var name by Classes.name
    var socialStatus by Classes.socialStatus
    var description by Classes.description
    var primarySkills by Classes.primarySkills
}

// ---------- Заклинание ----------
object Spells : OwnedTable("spell") {
    val name = varchar("name", 100)
    val time = varchar("time", 50)
    val type = varchar("type", 50)
    val distance = integer("distance")
    val effect = text("effect")
    val cost = integer("cost")
    val description = text("description")
}

class SpellEntity(id: EntityID<Long>) : OwnedEntity(id, Spells) {
    companion object : LongEntityClass<SpellEntity>(Spells)

    var name by Spells.name
    var time by Spells.time
    var type by Spells.type
    var distance by Spells.distance
    var effect by Spells.effect
    var cost by Spells.cost
    var description by Spells.description
}

// ---------- Навыки (только чтение, нужны оружию) ----------
object Skills : LongIdTable("skill") {
    val name = varchar("name", 100)
    val description = text("description")
    val attributeId = long("attribute_id")
}

class SkillEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<SkillEntity>(Skills)

    var name by Skills.name
    var description by Skills.description
    var attributeId by Skills.attributeId
}

// ---------- Снаряжение: предмет + расширение (оружие / броня; щит - это броня) ----------
object Items : OwnedTable("item") {
    val name = varchar("name", 100)
    val category = varchar("category", 50)
    val weight = integer("weight")
    val value = integer("value")
    val description = text("description")
}

class ItemEntity(id: EntityID<Long>) : OwnedEntity(id, Items) {
    companion object : LongEntityClass<ItemEntity>(Items)

    var name by Items.name
    var category by Items.category
    var weight by Items.weight
    var value by Items.value
    var description by Items.description
}

object Weapons : LongIdTable("weapon") {
    val damage = varchar("damage", 50)
    val damageType = varchar("damage_type", 50)
    val weaponSkill = reference("weapon_skill", Skills, onDelete = ReferenceOption.SET_NULL).nullable()
    val grip = varchar("grip", 20)
    val item = reference("item_id", Items, onDelete = ReferenceOption.CASCADE)
}

class WeaponEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<WeaponEntity>(Weapons)

    var damage by Weapons.damage
    var damageType by Weapons.damageType
    var weaponSkill by SkillEntity optionalReferencedOn Weapons.weaponSkill
    var grip by Weapons.grip
    var item by ItemEntity referencedOn Weapons.item
}

object Armours : LongIdTable("armour") {
    val damageMitigation = integer("damage_mitigation")
    val requirements = text("requirements")
    val armourType = varchar("armour_type", 50)
    val disadvantages = text("disadvantages")
    val resistance = text("resistance")
    val vulnerability = text("vulnerability")
    val item = reference("item_id", Items, onDelete = ReferenceOption.CASCADE)
}

class ArmourEntity(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<ArmourEntity>(Armours)

    var damageMitigation by Armours.damageMitigation
    var requirements by Armours.requirements
    var armourType by Armours.armourType
    var disadvantages by Armours.disadvantages
    var resistance by Armours.resistance
    var vulnerability by Armours.vulnerability
    var item by ItemEntity referencedOn Armours.item
}
