package com.defenderofegril.editor

/**
 * iOS stub implementation - Editor not supported on iOS
 */
class IosFileStorage : FileStorage {
    override fun writeFile(path: String, content: String) {
        throw UnsupportedOperationException("Level editor not supported on iOS")
    }
    
    override fun readFile(path: String): String? {
        throw UnsupportedOperationException("Level editor not supported on iOS")
    }
    
    override fun listFiles(directory: String): List<String> {
        throw UnsupportedOperationException("Level editor not supported on iOS")
    }
    
    override fun fileExists(path: String): Boolean {
        return false
    }
    
    override fun createDirectory(path: String) {
        throw UnsupportedOperationException("Level editor not supported on iOS")
    }
}

actual fun getFileStorage(): FileStorage = IosFileStorage()
