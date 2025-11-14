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
        return localStorage.getItem(PREFIX + path) != null
    }
    
    override fun createDirectory(path: String) {
        // No-op for localStorage - directories are virtual
    }
    
    override fun deleteFile(path: String) {
        localStorage.removeItem(PREFIX + path)
    }
}

actual fun getFileStorage(): FileStorage = WasmJsFileStorage()
