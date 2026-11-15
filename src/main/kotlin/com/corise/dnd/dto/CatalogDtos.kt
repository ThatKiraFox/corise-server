package com.corise.dnd.dto

import kotlinx.serialization.Serializable

@Serializable
data class NamedRef(val id: Long, val name: String)

@Serializable
data class PageResponse<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Long,
    val hasMore: Boolean,
)

// ---------- Ответы ----------

@Serializable
data class RaceResponse(
    val id: Long,
    val outName: String,
    val inName: String,
    val shortDesc: String,
    val appearance: String,
    val lore: String,
    val relationships: String,
    val status: String,
    val archetype: String,
    val lifespan: Int?,
    val isCustom: Boolean,
)

/** Культура, религия, предыстория. */
@Serializable
data class NamedResponse(val id: Long, val name: String, val description: String, val isCustom: Boolean)

@Serializable
data class ClassResponse(
    val id: Long,
    val name: String,
    val socialStatus: Int,
    val description: String,
    val primarySkills: String,
    val isCustom: Boolean,
)

@Serializable
data class SpellResponse(
    val id: Long,
    val name: String,
    val time: String,
    val type: String,
    val distance: Int,
    val effect: String,
    val cost: Int,
    val description: String,
    val isCustom: Boolean,
)

@Serializable
data class SkillResponse(val id: Long, val name: String, val description: String, val attributeId: Long)

@Serializable
data class WeaponResponse(
    val id: Long,
    val itemId: Long,
    val name: String,
    val weight: Int,
    val value: Int,
    val description: String,
    val damage: String,
    val damageType: String,
    val grip: String,
    val weaponSkillId: Long?,
    val isCustom: Boolean,
)

@Serializable
data class ArmourResponse(
    val id: Long,
    val itemId: Long,
    val category: String,
    val name: String,
    val weight: Int,
    val value: Int,
    val description: String,
    val damageMitigation: Int,
    val requirements: String,
    val armourType: String,
    val disadvantages: String,
    val resistance: String,
    val vulnerability: String,
    val isCustom: Boolean,
)

/** Элемент списка лора: без текста статьи. */
@Serializable
data class LoreListItem(val id: Long, val category: String, val title: String, val isCustom: Boolean)

@Serializable
data class LoreResponse(
    val id: Long,
    val category: String,
    val title: String,
    val body: String,
    val isCustom: Boolean,
)