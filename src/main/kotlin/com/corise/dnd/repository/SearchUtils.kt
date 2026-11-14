package com.corise.dnd.repository

/**
 * Шаблон для LIKE: служебные символы из пользовательского ввода экранируются,
 * сама строка в SQL не подставляется - Exposed передаёт её параметром.
 */
fun likePattern(search: String): String {
    val escaped = search.lowercase()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%$escaped%"
}
