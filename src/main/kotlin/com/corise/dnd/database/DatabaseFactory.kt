package com.corise.dnd.database

import com.corise.dnd.config.DbConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseFactory {
    private var dataSource: HikariDataSource? = null

    fun init(config: DbConfig) {
        // при повторном запуске (например, в тестах) закрываем прежний пул
        dataSource?.close()

        val ds = HikariDataSource(HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
        })
        // Схема создаётся только миграциями Flyway (V1, V2, ...), вручную таблицы не создаём.
        Flyway.configure()
            .dataSource(ds)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        Database.connect(ds)
        dataSource = ds
    }
}

/** Одна транзакция: commit при успехе, rollback при любом исключении. */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
