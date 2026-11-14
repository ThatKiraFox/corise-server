package com.corise.dnd.service

/** Хват оружия. Щит можно носить только с одноручным оружием. */
object Grip {
    const val ONE_HANDED = "one_handed"
    const val TWO_HANDED = "two_handed"
    const val POLEARM = "polearm"
    val ALL = setOf(ONE_HANDED, TWO_HANDED, POLEARM)
}