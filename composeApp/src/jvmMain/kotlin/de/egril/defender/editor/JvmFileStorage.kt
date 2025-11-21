package de.egril.defender.editor

import java.io.File

/**
 * Base JVM implementation of FileStorage using java.io.File API.
 * Platform-specific implementations provide the base directory.
 */
abstract class JvmFileStorage : FileStorage {
    protected abstract val baseDir: File?
    
    override fun writeFile(path: String, content: String) {
        val dir = baseDir ?: return
        val file = File(dir, path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }
    
    override fun readFile(path: String): String? {
        val dir = baseDir ?: return null
        val file = File(dir, path)
        return if (file.exists()) {
            file.readText()
        } else {
            null
        }
    }
    
    override fun listFiles(directory: String): List<String> {
        val dir = baseDir ?: return emptyList()
        val targetDir = File(dir, directory)
        return if (targetDir.exists() && targetDir.isDirectory) {
            targetDir.listFiles()?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    override fun fileExists(path: String): Boolean {
        val dir = baseDir ?: return false
        return File(dir, path).exists()
    }
    
    override fun createDirectory(path: String) {
        val dir = baseDir ?: return
        File(dir, path).mkdirs()
    }
    
    override fun deleteFile(path: String) {
        val dir = baseDir ?: return
        File(dir, path).delete()
    }
}
