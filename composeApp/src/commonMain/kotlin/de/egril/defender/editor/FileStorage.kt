package de.egril.defender.editor

/**
 * Platform-specific file storage interface
 */
interface FileStorage {
    fun writeFile(path: String, content: String)
    fun readFile(path: String): String?
    fun listFiles(directory: String): List<String>
    fun fileExists(path: String): Boolean
    fun createDirectory(path: String)
    fun deleteFile(path: String)
    fun renameDirectory(oldPath: String, newPath: String): Boolean
    fun copyDirectory(sourcePath: String, targetPath: String): Boolean
    fun deleteDirectory(path: String): Boolean
    fun getAbsolutePath(path: String): String
    fun writeBinaryFile(path: String, content: ByteArray)
    fun readBinaryFile(path: String): ByteArray?
}

expect fun getFileStorage(): FileStorage
