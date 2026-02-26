package de.egril.defender.editor

import kotlinx.browser.localStorage
import kotlinx.browser.window

/**
 * WasmJs implementation of FileStorage using browser localStorage.
 * This provides persistence for save games and world map state in the browser.
 */
class WasmJsFileStorage : FileStorage {
    private val PREFIX = "defender-of-egril:"
    private val memoryFiles = mutableMapOf<String, String>()
    
    override fun writeFile(path: String, content: String) {
        val key = PREFIX + path
        try {
            localStorage.setItem(key, content)
        } catch (_: Throwable) {
            memoryFiles[key] = content
        }
    }
    
    override fun readFile(path: String): String? {
        val key = PREFIX + path
        memoryFiles[key]?.let { return it }
        return localStorage.getItem(key)
    }
    
    override fun listFiles(directory: String): List<String> {
        val files = mutableListOf<String>()
        val prefix = PREFIX + directory + "/"
        
        // Iterate through all localStorage keys
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith(prefix)) {
                // Extract filename from the full key
                val filename = key.removePrefix(prefix)
                // Only add if it's a direct child (no subdirectories)
                if (!filename.contains("/")) {
                    files.add(filename)
                }
            }
        }

        // Include in-memory files
        memoryFiles.keys.filter { it.startsWith(prefix) }.forEach { key ->
            val filename = key.removePrefix(prefix)
            if (!filename.contains("/")) {
                files.add(filename)
            }
        }
        
        return files
    }
    
    override fun fileExists(path: String): Boolean {
        if (memoryFiles.containsKey(PREFIX + path)) {
            return true
        }
        // Check if it exists as a file
        if (localStorage.getItem(PREFIX + path) != null) {
            return true
        }
        
        // Check if it exists as a virtual directory (any keys start with path + "/")
        // Note: This iterates through all localStorage keys, which is O(n) where n is the
        // total number of keys. This is acceptable for the game's use case because:
        // 1. Game data is limited (typically < 100 files)
        // 2. This check is infrequent (once per session at most)
        // 3. Early termination on first match
        val dirPrefix = PREFIX + path + "/"
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith(dirPrefix)) {
                return true  // Early termination on first match
            }
        }
        
        return false
    }
    
    override fun createDirectory(path: String) {
        // No-op for localStorage - directories are virtual
    }
    
    override fun deleteFile(path: String) {
        val key = PREFIX + path
        memoryFiles.remove(key)
        localStorage.removeItem(key)
    }
    
    override fun renameDirectory(oldPath: String, newPath: String): Boolean {
        // For localStorage, we need to rename all keys with the old prefix
        val oldPrefix = PREFIX + oldPath + "/"
        val newPrefix = PREFIX + newPath + "/"
        val keysToRename = mutableListOf<Pair<String, String>>()
        
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith(oldPrefix)) {
                val newKey = key.replaceFirst(oldPrefix, newPrefix)
                keysToRename.add(key to newKey)
            }
        }

        // In-memory entries
        memoryFiles.keys.filter { it.startsWith(oldPrefix) }.forEach { key ->
            val newKey = key.replaceFirst(oldPrefix, newPrefix)
            keysToRename.add(key to newKey)
        }
        
        // Perform the rename
        keysToRename.forEach { (oldKey, newKey) ->
            val content = memoryFiles.remove(oldKey) ?: localStorage.getItem(oldKey)
            if (content != null) {
                try {
                    localStorage.setItem(newKey, content)
                    localStorage.removeItem(oldKey)
                } catch (_: Throwable) {
                    memoryFiles[newKey] = content
                    localStorage.removeItem(oldKey)
                }
            }
        }
        
        return keysToRename.isNotEmpty()
    }
    
    override fun copyDirectory(sourcePath: String, targetPath: String): Boolean {
        // For localStorage, we need to copy all keys with the source prefix
        val sourcePrefix = PREFIX + sourcePath + "/"
        val targetPrefix = PREFIX + targetPath + "/"
        var copied = false
        
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith(sourcePrefix)) {
                val content = localStorage.getItem(key) ?: continue
                val newKey = key.replaceFirst(sourcePrefix, targetPrefix)
                localStorage.setItem(newKey, content)
                copied = true
            }
        }

        // In-memory entries
        memoryFiles.keys.filter { it.startsWith(sourcePrefix) }.forEach { key ->
            val content = memoryFiles[key] ?: return@forEach
            val newKey = key.replaceFirst(sourcePrefix, targetPrefix)
            try {
                localStorage.setItem(newKey, content)
            } catch (_: Throwable) {
                memoryFiles[newKey] = content
            }
            copied = true
        }
        
        return copied
    }
    
    override fun deleteDirectory(path: String): Boolean {
        // For localStorage, we need to delete all keys with the prefix
        val prefix = PREFIX + path + "/"
        val keysToDelete = mutableListOf<String>()
        
        for (i in 0 until localStorage.length) {
            val key = localStorage.key(i)
            if (key != null && key.startsWith(prefix)) {
                keysToDelete.add(key)
            }
        }

        // In-memory entries
        memoryFiles.keys.filter { it.startsWith(prefix) }.forEach { key ->
            keysToDelete.add(key)
        }
        
        keysToDelete.forEach { key ->
            memoryFiles.remove(key)
            localStorage.removeItem(key)
        }
        
        return true
    }
    
    override fun getAbsolutePath(path: String): String {
        // For browser storage, return a descriptive path
        return "Browser Storage: $path"
    }

    override fun writeBinaryFile(path: String, content: ByteArray) {
        val builder = StringBuilder(content.size)
        content.forEach { byte ->
            builder.append((byte.toInt() and 0xFF).toChar())
        }
        val base64 = window.btoa(builder.toString())
        val key = PREFIX + path
        val payload = "base64:$base64"
        try {
            localStorage.setItem(key, payload)
        } catch (_: Throwable) {
            memoryFiles[key] = payload
        }
    }

    override fun readBinaryFile(path: String): ByteArray? {
        val key = PREFIX + path
        val stored = memoryFiles[key] ?: localStorage.getItem(key) ?: return null
        if (!stored.startsWith("base64:")) return null
        val base64 = stored.removePrefix("base64:")
        val binary = window.atob(base64)
        return ByteArray(binary.length) { idx -> binary[idx].code.toByte() }
    }
}

actual fun getFileStorage(): FileStorage = WasmJsFileStorage()
