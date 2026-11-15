package com.corise.dnd.model

import org.jetbrains.exposed.sql.Table

// Лёгкие описания таблиц блока характера, языков и классов персонажа (только DSL-запросы).

object PersonalityTable : Table("personality") {
    val character = long("character_id")
    val biography = text("biography")
    val goal = text("goal")
    val uniqueTraits = text("unique_traits")
    val personalityTraits = text("personality_traits")
    val personalValues = text("personal_values")
    val personalEvils = text("personal_evils")
}

object LanguageTable : Table("language") {
    val id = long("id")
    val name = varchar("name", 100)
}

object CultureLanguageTable : Table("culture_languages") {
    val culture = long("culture_id")
    val language = long("language_id")
}

object CharacterLanguageTable : Table("character_languages") {
    val character = long("character_id")
    val language = long("language_id")
}

object CharacterClassTable : Table("character_classes") {
    val character = long("character_id")
    val classId = long("class_id")
    val classLevel = integer("class_level")
}