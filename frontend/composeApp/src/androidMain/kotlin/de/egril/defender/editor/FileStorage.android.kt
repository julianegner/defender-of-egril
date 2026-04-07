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
    
    // Use shared JVM implementation when possible, fallback for tests
    private val jvmStorage = object : JvmFileStorage() {
        override val baseDir: File?
            get() = this@AndroidFileStorage.baseDir
    }
    
    override fun writeFile(path: String, content: String) {
        if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryFiles[path] = content
        } else {
            jvmStorage.writeFile(path, content)
        }
    }
    
    override fun readFile(path: String): String? {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryFiles[path]
        } else {
            jvmStorage.readFile(path)
        }
    }
    
    override fun listFiles(directory: String): List<String> {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryFiles.keys
                .filter { it.startsWith("$directory/") }
                .map { it.removePrefix("$directory/").substringBefore("/") }
                .distinct()
                .filter { it.isNotBlank() && !inMemoryFiles.keys.any { key -> key.startsWith("$directory/$it/") } }
        } else {
            jvmStorage.listFiles(directory)
        }
    }
    
    override fun fileExists(path: String): Boolean {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryFiles.containsKey(path)
        } else {
            jvmStorage.fileExists(path)
        }
    }
    
    override fun createDirectory(path: String) {
        if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryDirectories.add(path)
        } else {
            jvmStorage.createDirectory(path)
        }
    }
    
    override fun deleteFile(path: String) {
        if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryFiles.remove(path)
        } else {
            jvmStorage.deleteFile(path)
        }
    }
    
    override fun renameDirectory(oldPath: String, newPath: String): Boolean {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests - not implemented for in-memory
            false
        } else {
            jvmStorage.renameDirectory(oldPath, newPath)
        }
    }
    
    override fun copyDirectory(sourcePath: String, targetPath: String): Boolean {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests - not implemented for in-memory
            false
        } else {
            jvmStorage.copyDirectory(sourcePath, targetPath)
        }
    }
    
    override fun deleteDirectory(path: String): Boolean {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests
            inMemoryFiles.keys.removeAll { it.startsWith("$path/") }
            inMemoryDirectories.remove(path)
            true
        } else {
            jvmStorage.deleteDirectory(path)
        }
    }
    
    override fun getAbsolutePath(path: String): String {
        return if (useInMemory || baseDir == null) {
            // Fallback for tests
            path
        } else {
            jvmStorage.getAbsolutePath(path)
        }
    }

    override fun writeBinaryFile(path: String, content: ByteArray) {
        if (useInMemory || baseDir == null) {
            // No-op for tests
        } else {
            jvmStorage.writeBinaryFile(path, content)
        }
    }

    override fun readBinaryFile(path: String): ByteArray? {
        return if (useInMemory || baseDir == null) {
            null
        } else {
            jvmStorage.readBinaryFile(path)
        }
    }
}

actual fun getFileStorage(): FileStorage = AndroidFileStorage()
