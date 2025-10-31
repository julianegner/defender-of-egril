package com.defenderofegril.editor

import java.io.File

class DesktopFileStorage : FileStorage {
    private val baseDir = File(System.getProperty("user.home"), ".defender-of-egril")
    
    init {
        // Ensure base directory exists
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
    }
    
    override fun writeFile(path: String, content: String) {
        val file = File(baseDir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
    
    override fun readFile(path: String): String? {
        val file = File(baseDir, path)
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }
    
    override fun listFiles(directory: String): List<String> {
        val dir = File(baseDir, directory)
        return if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    override fun fileExists(path: String): Boolean {
        return File(baseDir, path).exists()
    }
    
    override fun createDirectory(path: String) {
        File(baseDir, path).mkdirs()
    }
    
    override fun deleteFile(path: String) {
        File(baseDir, path).delete()
    }
}

actual fun getFileStorage(): FileStorage = DesktopFileStorage()
