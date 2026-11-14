package com.corise.dnd.service

/** Сбор ошибок валидации по полям; одно исключение со всеми ошибками сразу. */
internal class FieldErrors {
    private val errors = linkedMapOf<String, String>()

    fun text(field: String, value: String, max: Int, required: Boolean = true) {
        val len = value.trim().length
        if (required && len == 0) errors[field] = "Обязательное поле"
        else if (len > max) errors[field] = "Не длиннее $max символов"
    }

    fun nonNegative(field: String, value: Int) {
        if (value < 0) errors[field] = "Не может быть отрицательным"
    }

    fun positiveOrNull(field: String, value: Int?) {
        if (value != null && value <= 0) errors[field] = "Должно быть больше нуля"
    }

    fun add(field: String, message: String) {
        errors[field] = message
    }

    fun oneOf(field: String, value: String, allowed: Set<String>) {
        if (value !in allowed) errors[field] = "Допустимо: ${allowed.joinToString()}"
    }

    fun throwIfAny() {
        if (errors.isNotEmpty()) throw ValidationException(errors)
    }
}
