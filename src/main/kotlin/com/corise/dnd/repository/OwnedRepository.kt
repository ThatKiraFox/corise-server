package com.corise.dnd.repository

import com.corise.dnd.model.OwnedEntity
import com.corise.dnd.model.OwnedTable
import com.corise.dnd.model.Users
import com.corise.dnd.model.visibleTo
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.SortOrder

/** Общий доступ к данным для контента со стандартными и пользовательскими записями. */
class OwnedRepository<E : OwnedEntity>(
    private val table: OwnedTable,
    private val entityClass: LongEntityClass<E>,
) {
    /** Запись, видимая пользователю: стандартная или его собственная. Чужая пользовательская = null. */
    fun findVisible(id: Long, userId: Long): E? =
        entityClass.findById(id)?.takeIf { it.isVisibleTo(userId) }

    fun listVisible(userId: Long, sortBy: Column<*>): List<E> =
        entityClass.find(visibleTo(table, userId)).orderBy(sortBy to SortOrder.ASC).toList()

    /** Новая запись всегда пользовательская, автор - текущий пользователь. */
    fun create(userId: Long, init: E.() -> Unit): E = entityClass.new {
        isCustom = true
        createdBy = EntityID(userId, Users)
        init()
    }
}
