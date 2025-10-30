package com.defenderofegril.editor

/**
 * Platform-specific file storage interface
 */
interface FileStorage {
    fun writeFile(path: String, content: String)
    fun readFile(path: String): String?
    fun listFiles(directory: String): List<String>
    fun fileExists(path: String): Boolean
    fun createDirectory(path: String)
}

expect fun getFileStorage(): FileStorage
