package com.example.qwen3chat.data

import android.content.Context
import java.io.File

object NoteFileStorage {

    private const val NOTES_DIR = "notes"

    fun getNotesDir(context: Context): File {
        val dir = File(context.filesDir, NOTES_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listNoteFiles(context: Context): List<File> {
        return getNotesDir(context).listFiles()
            ?.filter { it.isFile && it.name.endsWith(".md") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun readNote(context: Context, filename: String): String? {
        val file = File(getNotesDir(context), filename)
        return if (file.exists()) file.readText() else null
    }

    fun writeNote(context: Context, filename: String, content: String) {
        val file = File(getNotesDir(context), filename)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun deleteNoteFile(context: Context, filename: String): Boolean {
        val file = File(getNotesDir(context), filename)
        return if (file.exists()) file.delete() else false
    }

    fun renameNoteFile(context: Context, oldFilename: String, newFilename: String): Boolean {
        val oldFile = File(getNotesDir(context), oldFilename)
        val newFile = File(getNotesDir(context), newFilename)
        return if (oldFile.exists()) oldFile.renameTo(newFile) else false
    }

    fun getAbsolutePath(context: Context, filename: String): String {
        return File(getNotesDir(context), filename).absolutePath
    }

    fun ensureMdExtension(filename: String): String {
        return if (filename.endsWith(".md")) filename else "$filename.md"
    }

    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim()
    }
}