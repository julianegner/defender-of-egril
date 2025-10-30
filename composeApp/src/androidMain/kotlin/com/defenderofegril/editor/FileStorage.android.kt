package com.defenderofegril.editor

/**
 * Android implementation uses in-memory storage
 * Actual file storage would require Android Context which isn't available in unit tests
 */
class AndroidFileStorage : FileStorage {
    private val files = mutableMapOf<String, String>()
    private val directories = mutableSetOf<String>()
    
    override fun writeFile(path: String, content: String) {
        files[path] = content
    }
    
    override fun readFile(path: String): String? {
        return files[path]
    }
    
    override fun listFiles(directory: String): List<String> {
        return files.keys
            .filter { it.startsWith("$directory/") }
            .map { it.removePrefix("$directory/").substringBefore("/") }
            .distinct()
            .filter { it.isNotBlank() && !files.keys.any { key -> key.startsWith("$directory/$it/") } }
    }
    
    override fun fileExists(path: String): Boolean {
        return files.containsKey(path)
    }
    
    override fun createDirectory(path: String) {
        directories.add(path)
    }
}

actual fun getFileStorage(): FileStorage = AndroidFileStorage()
