package com.corise.dnd.config

import java.io.File

/**
 * Читает файл .env из рабочей папки и кладёт значения в системные свойства,
 * откуда их подхватывает application.conf (${DB_URL} и т.д.).
 * Реальные переменные окружения приоритетнее файла.
 */
fun loadDotEnv(path: String = ".env") {
    val file = File(path)
    if (!file.exists()) return
    file.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .forEach { line ->
            val key = line.substringBefore("=").trim()
            val value = line.substringAfter("=").trim().removeSurrounding("\"")
            if (System.getenv(key) == null && System.getProperty(key) == null) {
                System.setProperty(key, value)
            }
        }
}
