package com.defenderofegril.editor

/**
 * Android stub implementation - Editor not supported on Android
 */
class AndroidFileStorage : FileStorage {
    override fun writeFile(path: String, content: String) {
        throw UnsupportedOperationException("Level editor not supported on Android")
    }
    
    override fun readFile(path: String): String? {
        throw UnsupportedOperationException("Level editor not supported on Android")
    }
    
    override fun listFiles(directory: String): List<String> {
        throw UnsupportedOperationException("Level editor not supported on Android")
    }
    
    override fun fileExists(path: String): Boolean {
        return false
    }
    
    override fun createDirectory(path: String) {
        throw UnsupportedOperationException("Level editor not supported on Android")
    }
}

actual fun getFileStorage(): FileStorage = AndroidFileStorage()
