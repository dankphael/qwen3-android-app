package com.example.qwen3.core

import app.cash.sqldelight.db.SqlDriver

/** Provides the platform-specific SQLDelight driver (Android vs iOS native). */
interface DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
