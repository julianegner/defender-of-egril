package de.egril.defender.editor

import java.io.File

/**
 * Android implementation using actual file storage in app's internal storage directory.
 * Files are stored in Context.filesDir which is private to the app and persists between app restarts.
 * 
 * For unit tests without Android context, falls back to in-memory storage.
 */
class AndroidFileStorage : FileStorage {
    // Fallback to in-memory storage for tests where context is not available
    private val inMemoryFiles = mutableMapOf<String, String>()
    private val inMemoryDirectories = mutableSetOf<String>()
    private var useInMemory = false
    
    private val baseDir: File?
        get() {
            return try {
                val context = de.egril.defender.AndroidContextProvider.getContext()
                File(context.filesDir, "defender-of-egril").also { 
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                }
            } catch (e: IllegalStateException) {
                // Context not available (likely in unit tests)
                useInMemory = true
                null
            }
        }
    
    override fun writeFile(path: String, content: String) {
        val dir = baseDir
        if (dir != null) {
            val file = File(dir, path)
            file.parentFile?.mkdirs()
            file.writeText(content)
        } else {
            // Fallback for tests
            inMemoryFiles[path] = content
        }
    }
    
    override fun readFile(path: String): String? {
        val dir = baseDir
        return if (dir != null) {
            val file = File(dir, path)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } else {
            // Fallback for tests
            inMemoryFiles[path]
        }
    }
    
    override fun listFiles(directory: String): List<String> {
        val dir = baseDir
        return if (dir != null) {
            val targetDir = File(dir, directory)
            if (targetDir.exists() && targetDir.isDirectory) {
                targetDir.listFiles()?.map { it.name } ?: emptyList()
            } else {
                emptyList()
            }
        } else {
            // Fallback for tests
            inMemoryFiles.keys
                .filter { it.startsWith("$directory/") }
                .map { it.removePrefix("$directory/").substringBefore("/") }
                .distinct()
                .filter { it.isNotBlank() && !inMemoryFiles.keys.any { key -> key.startsWith("$directory/$it/") } }
        }
    }
    
    override fun fileExists(path: String): Boolean {
        val dir = baseDir
        return if (dir != null) {
            File(dir, path).exists()
        } else {
            // Fallback for tests
            inMemoryFiles.containsKey(path)
        }
    }
    
    override fun createDirectory(path: String) {
        val dir = baseDir
        if (dir != null) {
            File(dir, path).mkdirs()
        } else {
            // Fallback for tests
            inMemoryDirectories.add(path)
        }
    }
    
    override fun deleteFile(path: String) {
        val dir = baseDir
        if (dir != null) {
            File(dir, path).delete()
        } else {
            // Fallback for tests
            inMemoryFiles.remove(path)
        }
    }
}

actual fun getFileStorage(): FileStorage = AndroidFileStorage()
