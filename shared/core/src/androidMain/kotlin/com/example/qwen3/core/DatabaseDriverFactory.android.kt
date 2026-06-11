package com.example.qwen3.core

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.qwen3.core.db.ChatDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver =
        AndroidSqliteDriver(
            schema = ChatDatabase.Schema,
            context = context,
            name = "qwen3.db",
            callback = object : AndroidSqliteDriver.Callback(ChatDatabase.Schema) {
                override fun onConfigure(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onConfigure(db)
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
}
