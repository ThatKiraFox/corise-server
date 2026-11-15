package com.corise.dnd

import com.corise.dnd.service.Stats
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {

    @Test
    fun `bonus`() {
        assertEquals(0, Stats.enduranceHpBonus(0))
        assertEquals(1, Stats.enduranceHpBonus(1))
        assertEquals(5, Stats.enduranceHpBonus(5))
        assertEquals(7, Stats.enduranceHpBonus(6))
        assertEquals(15, Stats.enduranceHpBonus(10))
    }

    @Test
    fun `максимальное HP считается по формуле`() {
        assertEquals(12, Stats.maxHp(level = 1, endurance = 0))
        assertEquals(10 + 7 + 8, Stats.maxHp(level = 4, endurance = 6))
        assertEquals(10 + 15 + 20, Stats.maxHp(level = 10, endurance = 10))
    }

    @Test
    fun `модификатор навыка это уровень пополам с округлением вниз`() {
        assertEquals(0, Stats.skillModifier(0))
        assertEquals(0, Stats.skillModifier(1))
        assertEquals(2, Stats.skillModifier(5))
        assertEquals(5, Stats.skillModifier(10))
    }

    @Test
    fun `максимум магической нагрузки считается по формуле`() {
        assertEquals(10, Stats.maxMl(level = 1, magicResistanceLevel = 0, endurance = 0))
        // 2 уровень, навык 4 (модификатор 2), выносливость 5: (2 + 2 + 5) * 10
        assertEquals(90, Stats.maxMl(level = 2, magicResistanceLevel = 4, endurance = 5))
    }
    @Test
    fun `cost of attribute is correct`() {
        assertEquals(0, Stats.attributeCost(0))
        assertEquals(1, Stats.attributeCost(1))
        assertEquals(3, Stats.attributeCost(2))
        assertEquals(6, Stats.attributeCost(3))
        assertEquals(10, Stats.attributeCost(4))
        assertEquals(15, Stats.attributeCost(5))
    }
}