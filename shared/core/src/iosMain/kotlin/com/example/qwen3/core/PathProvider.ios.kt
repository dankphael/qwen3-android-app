package com.example.qwen3.core

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class IosPathProvider : PathProvider {
    private fun documentsDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSDocumentDirectory, NSUserDomainMask, true,
        )
        return paths.firstOrNull() as? String ?: NSFileManager.defaultManager.currentDirectoryPath
    }

    override fun modelsDir(): String = documentsDir()

    override fun notesDir(): String {
        val dir = documentsDir().trimEnd('/') + "/notes"
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
        return dir
    }

    override fun databasePath(): String = documentsDir().trimEnd('/') + "/qwen3.db"
}
