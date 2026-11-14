package com.corise.dnd.model

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or

/** Таблица контента, который может быть стандартным или пользовательским. */
abstract class OwnedTable(name: String) : LongIdTable(name) {
    val isCustom = bool("is_custom")
    val createdBy = reference("created_by", Users, onDelete = ReferenceOption.SET_NULL).nullable()
}

abstract class OwnedEntity(id: EntityID<Long>, table: OwnedTable) : LongEntity(id) {
    var isCustom by table.isCustom
    var createdBy by table.createdBy

    /** Стандартный контент виден всем, пользовательский - только автору. */
    fun isVisibleTo(userId: Long): Boolean = !isCustom || createdBy?.value == userId

    fun isOwnedBy(userId: Long): Boolean = isCustom && createdBy?.value == userId
}

fun visibleTo(table: OwnedTable, userId: Long): Op<Boolean> =
    (table.isCustom eq false) or (table.createdBy eq EntityID(userId, Users))
