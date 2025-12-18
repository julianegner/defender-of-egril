package de.egril.defender.editor

import kotlinx.browser.localStorage

/**
 * WasmJs implementation of FileStorage using browser localStorage.
 * This provides persistence for save games and world map state in the browser.
 */
class WasmJsFileStorage : FileStorage {
    private val PREFIX = "defender-of-egril:"
    
    override fun writeFile(path: String, content: String) {
        localStorage.setItem(PREFIX + path, content)
    }
    
    override fun readFile(path: String): String? {
        return localStorage.getItem(PREFIX + path)
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
        
        return files
    }
    
    override fun fileExists(path: String): Boolean {
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
        localStorage.removeItem(PREFIX + path)
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
        
        // Perform the rename
        keysToRename.forEach { (oldKey, newKey) ->
            val content = localStorage.getItem(oldKey)
            if (content != null) {
                localStorage.setItem(newKey, content)
                localStorage.removeItem(oldKey)
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
        
        keysToDelete.forEach { key ->
            localStorage.removeItem(key)
        }
        
        return true
    }
    
    override fun getAbsolutePath(path: String): String {
        // For browser storage, return a descriptive path
        return "Browser Storage: $path"
    }
}

actual fun getFileStorage(): FileStorage = WasmJsFileStorage()
