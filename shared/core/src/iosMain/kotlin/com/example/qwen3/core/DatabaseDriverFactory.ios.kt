package com.example.qwen3.core

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.qwen3.core.db.ChatDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(
            schema = ChatDatabase.Schema,
            name = "qwen3.db",
            onConfiguration = { config ->
                config.copy(
                    extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true),
                )
            },
        )
}
