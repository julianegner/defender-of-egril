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
    
    override fun renameDirectory(oldPath: String, newPath: String): Boolean {
        val dir = baseDir ?: return false
        val oldDir = File(dir, oldPath)
        val newDir = File(dir, newPath)
        
        if (!oldDir.exists() || !oldDir.isDirectory) {
            return false
        }
        
        return oldDir.renameTo(newDir)
    }
    
    override fun copyDirectory(sourcePath: String, targetPath: String): Boolean {
        val dir = baseDir ?: return false
        val sourceDir = File(dir, sourcePath)
        val targetDir = File(dir, targetPath)
        
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            return false
        }
        
        // Check if target already exists
        if (targetDir.exists()) {
            println("Target directory already exists: $targetPath")
            return false
        }
        
        return try {
            sourceDir.copyRecursively(targetDir, overwrite = false)
            true
        } catch (e: Exception) {
            println("Error copying directory: ${e.message}")
            false
        }
    }
    
    override fun deleteDirectory(path: String): Boolean {
        val dir = baseDir ?: return false
        val targetDir = File(dir, path)
        
        if (!targetDir.exists()) {
            return true // Already doesn't exist
        }
        
        return targetDir.deleteRecursively()
    }
    
    override fun getAbsolutePath(path: String): String {
        val dir = baseDir ?: return path
        return File(dir, path).absolutePath
    }

    override fun writeBinaryFile(path: String, content: ByteArray) {
        val dir = baseDir ?: return
        val file = File(dir, path)
        file.parentFile?.mkdirs()
        file.writeBytes(content)
    }

    override fun readBinaryFile(path: String): ByteArray? {
        val dir = baseDir ?: return null
        val file = File(dir, path)
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }
}
