package com.corise.dnd.dto

import kotlinx.serialization.Serializable

// Общие поля создания и изменения; сами схемы запросов - отдельные классы.
interface RaceFields {
    val outName: String
    val inName: String
    val shortDesc: String
    val appearance: String
    val lore: String
    val relationships: String
    val status: String
    val archetype: String
    val lifespan: Int?
}

@Serializable
data class RaceCreateRequest(
    override val outName: String,
    override val inName: String,
    override val shortDesc: String = "",
    override val appearance: String = "",
    override val lore: String = "",
    override val relationships: String = "",
    override val status: String = "",
    override val archetype: String = "",
    override val lifespan: Int? = null,
) : RaceFields

@Serializable
data class RaceUpdateRequest(
    override val outName: String,
    override val inName: String,
    override val shortDesc: String,
    override val appearance: String,
    override val lore: String,
    override val relationships: String,
    override val status: String,
    override val archetype: String,
    override val lifespan: Int?,
) : RaceFields

/** Культура, религия, предыстория. */
interface NamedFields {
    val name: String
    val description: String
}

@Serializable
data class NamedCreateRequest(
    override val name: String,
    override val description: String = "",
) : NamedFields

@Serializable
data class NamedUpdateRequest(
    override val name: String,
    override val description: String,
) : NamedFields

interface ClassFields {
    val name: String
    val socialStatus: Int
    val description: String
    val primarySkills: String
}

@Serializable
data class ClassCreateRequest(
    override val name: String,
    override val socialStatus: Int = 0,
    override val description: String = "",
    override val primarySkills: String = "",
) : ClassFields

@Serializable
data class ClassUpdateRequest(
    override val name: String,
    override val socialStatus: Int,
    override val description: String,
    override val primarySkills: String,
) : ClassFields

interface SpellFields {
    val name: String
    val time: String
    val type: String
    val distance: Int
    val effect: String
    val cost: Int
    val description: String
}

@Serializable
data class SpellCreateRequest(
    override val name: String,
    override val time: String = "",
    override val type: String = "",
    override val distance: Int = 0,
    override val effect: String = "",
    override val cost: Int = 0,
    override val description: String = "",
) : SpellFields

@Serializable
data class SpellUpdateRequest(
    override val name: String,
    override val time: String,
    override val type: String,
    override val distance: Int,
    override val effect: String,
    override val cost: Int,
    override val description: String,
) : SpellFields

interface WeaponFields {
    val name: String
    val weight: Int
    val value: Int
    val description: String
    val damage: String
    val damageType: String
    val grip: String
    val weaponSkillId: Long?
}

@Serializable
data class WeaponCreateRequest(
    override val name: String,
    override val damage: String,
    override val damageType: String,
    override val grip: String = "one_handed",
    override val weight: Int = 0,
    override val value: Int = 0,
    override val description: String = "",
    override val weaponSkillId: Long? = null,
) : WeaponFields

@Serializable
data class WeaponUpdateRequest(
    override val name: String,
    override val weight: Int,
    override val value: Int,
    override val description: String,
    override val damage: String,
    override val damageType: String,
    override val grip: String,
    override val weaponSkillId: Long?,
) : WeaponFields

/** Броня и щит: для щита armourType = "shield" (или "щит"). */
interface ArmourFields {
    val name: String
    val weight: Int
    val value: Int
    val description: String
    val damageMitigation: Int
    val requirements: String
    val armourType: String
    val disadvantages: String
    val resistance: String
    val vulnerability: String
}

@Serializable
data class ArmourCreateRequest(
    override val name: String,
    override val armourType: String,
    override val damageMitigation: Int = 0,
    override val weight: Int = 0,
    override val value: Int = 0,
    override val description: String = "",
    override val requirements: String = "",
    override val disadvantages: String = "",
    override val resistance: String = "",
    override val vulnerability: String = "",
) : ArmourFields

@Serializable
data class ArmourUpdateRequest(
    override val name: String,
    override val weight: Int,
    override val value: Int,
    override val description: String,
    override val damageMitigation: Int,
    override val requirements: String,
    override val armourType: String,
    override val disadvantages: String,
    override val resistance: String,
    override val vulnerability: String,
) : ArmourFields
