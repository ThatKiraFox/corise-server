package com.corise.dnd.model

import org.jetbrains.exposed.dao.id.LongIdTable

// Лёгкие описания таблиц для атрибутов и навыков персонажа (без сущностей, только DSL-запросы).

object AttributeTable : LongIdTable("attribute") {
    val name = varchar("name", 50)
}

object SkillTable : LongIdTable("skill") {
    val name = varchar("name", 100)
    val attribute = long("attribute_id")
}

object CharacterAttributeTable : LongIdTable("character_attribute") {
    val character = long("character_id")
    val attribute = long("attribute_id")
    val value = integer("value")
}

object CharacterSkillTable : LongIdTable("character_skills") {
    val character = long("character_id")
    val skill = long("skill_id")
    val level = integer("proficiency_level")
}