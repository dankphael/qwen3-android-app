package com.example.qwen3.core

/**
 * Platform file-system locations.
 *  - Android: context.filesDir
 *  - iOS: NSDocumentDirectory
 */
interface PathProvider {
    fun modelsDir(): String
    fun notesDir(): String
    fun databasePath(): String
}
