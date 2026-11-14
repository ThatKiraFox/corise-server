package com.corise.dnd.service

import com.corise.dnd.dto.ArmourResponse
import com.corise.dnd.dto.CharacterResponse
import com.corise.dnd.dto.EquipmentResponse
import com.corise.dnd.dto.InventoryItemResponse
import com.corise.dnd.dto.ClassResponse
import com.corise.dnd.dto.NamedRef
import com.corise.dnd.dto.NamedResponse
import com.corise.dnd.dto.RaceResponse
import com.corise.dnd.dto.SkillResponse
import com.corise.dnd.dto.SpellResponse
import com.corise.dnd.dto.WeaponResponse
import com.corise.dnd.model.ArmourEntity
import com.corise.dnd.model.BackstoryEntity
import com.corise.dnd.model.CharacterEntity
import com.corise.dnd.model.CharacterItemEntity
import com.corise.dnd.model.ClassEntity
import com.corise.dnd.model.CultureEntity
import com.corise.dnd.model.RaceEntity
import com.corise.dnd.model.ReligionEntity
import com.corise.dnd.model.SkillEntity
import com.corise.dnd.model.SpellEntity
import com.corise.dnd.model.WeaponEntity

internal fun RaceEntity.toRef() = NamedRef(id.value, outName)
internal fun CultureEntity.toRef() = NamedRef(id.value, name)
internal fun ReligionEntity.toRef() = NamedRef(id.value, name)
internal fun BackstoryEntity.toRef() = NamedRef(id.value, name)

internal fun RaceEntity.toResponse() = RaceResponse(
    id.value, outName, inName, shortDesc, appearance, lore, relationships, status, archetype, lifespan, isCustom,
)
internal fun CultureEntity.toResponse() = NamedResponse(id.value, name, description, isCustom)
internal fun ReligionEntity.toResponse() = NamedResponse(id.value, name, description, isCustom)
internal fun BackstoryEntity.toResponse() = NamedResponse(id.value, name, description, isCustom)

internal fun ClassEntity.toResponse() = ClassResponse(id.value, name, socialStatus, description, primarySkills, isCustom)

internal fun SpellEntity.toResponse() =
    SpellResponse(id.value, name, time, type, distance, effect, cost, description, isCustom)

internal fun SkillEntity.toResponse() = SkillResponse(id.value, name, description, attributeId)

internal fun WeaponEntity.toResponse() = WeaponResponse(
    id = id.value,
    itemId = item.id.value,
    name = item.name,
    weight = item.weight,
    value = item.value,
    description = item.description,
    damage = damage,
    damageType = damageType,
    grip = grip,
    weaponSkillId = weaponSkill?.id?.value,
    isCustom = item.isCustom,
)

internal fun ArmourEntity.toResponse() = ArmourResponse(
    id = id.value,
    itemId = item.id.value,
    category = item.category,
    name = item.name,
    weight = item.weight,
    value = item.value,
    description = item.description,
    damageMitigation = damageMitigation,
    requirements = requirements,
    armourType = armourType,
    disadvantages = disadvantages,
    resistance = resistance,
    vulnerability = vulnerability,
    isCustom = item.isCustom,
)

internal fun CharacterItemEntity.toResponse() =
    InventoryItemResponse(item.id.value, item.name, item.category, item.weight, item.value, amount)

internal fun CharacterEntity.toResponse(inventory: List<CharacterItemEntity>) = CharacterResponse(
    id = id.value,
    name = name,
    race = race.toRef(),
    culture = culture?.toRef(),
    religion = religion?.toRef(),
    backstory = backstory?.toRef(),
    level = level,
    exp = exp,
    hpCur = hpCur,
    mlCur = mlCur,
    spells = spells.sortedBy { it.name }.map { it.toResponse() },
    equipment = EquipmentResponse(
        weapon = equippedWeapon?.toResponse(),
        armour = equippedArmour?.toResponse(),
        shield = equippedShield?.toResponse(),
    ),
    inventory = inventory.map { it.toResponse() }.sortedBy { it.name },
    attributes = emptyList(),
    skills = emptyList(),
)
