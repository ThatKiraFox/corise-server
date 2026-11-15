package com.corise.dnd.dto

import kotlinx.serialization.Serializable

/** Создание: обязательны name и raceId, остальное имеет значения по умолчанию. */
@Serializable
data class CharacterCreateRequest(
    val name: String,
    val raceId: Long,
    val cultureId: Long? = null,
    val religionId: Long? = null,
    val backstoryId: Long? = null,
    val level: Int = 1,
    val exp: Int = 0,
    val hpCur: Int = 0,
    val mlCur: Int = 0,
    val spellIds: List<Long> = emptyList(),
    val classId: Long? = null,
    val personality: PersonalityDto = PersonalityDto(),
    val attributes: List<StatInput> = emptyList(),
    val skills: List<StatInput> = emptyList(),
)

/** Изменение (PUT): все основные поля обязательны, заклинания меняются отдельными запросами. */
@Serializable
data class CharacterUpdateRequest(
    val name: String,
    val raceId: Long,
    val cultureId: Long? = null,
    val religionId: Long? = null,
    val backstoryId: Long? = null,
    val level: Int,
    val exp: Int,
    val hpCur: Int,
    val mlCur: Int,
)

@Serializable
data class AddSpellRequest(val spellId: Long)

@Serializable
data class CharacterResponse(
    val id: Long,
    val name: String,
    val race: NamedRef,
    val culture: NamedRef?,
    val religion: NamedRef?,
    val backstory: NamedRef?,
    val level: Int,
    val exp: Int,
    val hpCur: Int,
    val mlCur: Int,
    val spells: List<SpellResponse>,
    val equipment: EquipmentResponse,
    val inventory: List<InventoryItemResponse>,
    val attributes: List<CharacterAttributeResponse>,
    val skills: List<CharacterSkillResponse>,
    val hpMax: Int? = null,
    val mlMax: Int? = null,
    val personality: PersonalityDto? = null,
    val languages: List<NamedRef> = emptyList(),
    val classes: List<NamedRef> = emptyList(),
)

@Serializable
data class InventoryItemRequest(val amount: Int = 1)

/** Заменяет все три слота: что не передано (null), то снимается. */
@Serializable
data class EquipmentRequest(
    val weaponId: Long? = null,
    val armourId: Long? = null,
    val shieldId: Long? = null,
)

@Serializable
data class InventoryItemResponse(
    val itemId: Long, val name: String, val category: String,
    val weight: Int, val value: Int, val amount: Int,
)

@Serializable
data class EquipmentResponse(
    val weapon: WeaponResponse?, val armour: ArmourResponse?, val shield: ArmourResponse?,
)

@Serializable
data class SetValueRequest(val value: Int)

@Serializable
data class CharacterAttributeResponse(val attributeId: Long, val name: String, val value: Int)

@Serializable
data class CharacterSkillResponse(
    val skillId: Long,
    val name: String,
    val attributeId: Long,
    val level: Int,
)

@Serializable
data class PersonalityDto(
    val biography: String = "",
    val goal: String = "",
    val uniqueTraits: String = "",
    val personalityTraits: String = "",
    val personalValues: String = "",
    val personalEvils: String = "",
)

/** Значение атрибута или навыка при создании: id справочника и уровень. */
@Serializable
data class StatInput(val id: Long, val value: Int)