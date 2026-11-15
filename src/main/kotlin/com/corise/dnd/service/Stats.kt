package com.corise.dnd.service

/** Расчёт характеристик по правилам системы. Ничего не хранится: считается из данных персонажа. */
object Stats {
    const val ENDURANCE = "Выносливость"
    private const val BASE_HP = 10
    private const val HP_PER_LEVEL = 2
    const val MAGIC_RESISTANCE = "Магическая Стойкость"
    const val MAX_START_STAT = 5
    const val START_ATTRIBUTE_POINTS = 27
    const val START_SKILL_POINTS = 35

    /** Модификатор навыка: уровень / 2, округляя вниз. */
    fun skillModifier(skillLevel: Int): Int = skillLevel.coerceAtLeast(0) / 2

    /** Максимум магической нагрузки: (уровень + модификатор Магической Стойкости + Выносливость) * 10. */
    fun maxMl(level: Int, magicResistanceLevel: Int, endurance: Int): Int =
        (level + skillModifier(magicResistanceLevel) + endurance.coerceAtLeast(0)) * 10

    /** Бонус к HP от Выносливости: очки 1-5 дают по +1, очки 6-10 по +2. */
    fun enduranceHpBonus(endurance: Int): Int {
        val v = endurance.coerceAtLeast(0)
        return minOf(v, 5) + maxOf(v - 5, 0) * 2
    }

    fun maxHp(level: Int, endurance: Int): Int =
        BASE_HP + enduranceHpBonus(endurance) + level * HP_PER_LEVEL

    fun attributeCost(value: Int): Int = value * (value + 1) / 2
}