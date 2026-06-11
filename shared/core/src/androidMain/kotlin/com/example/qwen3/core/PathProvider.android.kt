package com.example.qwen3.core

import android.content.Context
import java.io.File

class AndroidPathProvider(private val context: Context) : PathProvider {
    override fun modelsDir(): String = context.filesDir.absolutePath

    override fun notesDir(): String =
        File(context.filesDir, "notes").apply { mkdirs() }.absolutePath

    override fun databasePath(): String =
        context.getDatabasePath("qwen3.db").absolutePath
}
