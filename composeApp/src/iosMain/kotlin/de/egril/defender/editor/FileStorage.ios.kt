package de.egril.defender.editor

import platform.Foundation.*

/**
 * iOS implementation using actual file storage in the app's documents directory.
 * Files are stored in NSDocumentDirectory which persists between app restarts.
 */
class IosFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager
    private val baseDir: String
        get() {
            val paths = fileManager.URLsForDirectory(
                NSDocumentDirectory,
                NSUserDomainMask
            )
            val documentsDirectory = paths.first() as NSURL
            val defenderDir = documentsDirectory.URLByAppendingPathComponent("defender-of-egril")!!
            
            // Create base directory if it doesn't exist
            if (!fileManager.fileExistsAtPath(defenderDir.path!!)) {
                fileManager.createDirectoryAtURL(
                    defenderDir,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null
                )
            }
            
            return defenderDir.path!!
        }
    
    override fun writeFile(path: String, content: String) {
        val filePath = "$baseDir/$path"
        val fileURL = NSURL.fileURLWithPath(filePath)
        
        // Create parent directory if needed
        fileURL.URLByDeletingLastPathComponent?.let { parentURL ->
            fileManager.createDirectoryAtURL(
                parentURL,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
        
        // Write file
        (content as NSString).writeToFile(
            filePath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }
    
    override fun readFile(path: String): String? {
        val filePath = "$baseDir/$path"
        return if (fileManager.fileExistsAtPath(filePath)) {
            NSString.stringWithContentsOfFile(
                filePath,
                encoding = NSUTF8StringEncoding,
                error = null
            ) as? String
        } else {
            null
        }
    }
    
    override fun listFiles(directory: String): List<String> {
        val dirPath = "$baseDir/$directory"
        return if (fileManager.fileExistsAtPath(dirPath)) {
            val contents = fileManager.contentsOfDirectoryAtPath(dirPath, error = null)
            (contents as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    override fun fileExists(path: String): Boolean {
        val filePath = "$baseDir/$path"
        return fileManager.fileExistsAtPath(filePath)
    }
    
    override fun createDirectory(path: String) {
        val dirPath = "$baseDir/$path"
        fileManager.createDirectoryAtPath(
            dirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }
    
    override fun deleteFile(path: String) {
        val filePath = "$baseDir/$path"
        fileManager.removeItemAtPath(filePath, error = null)
    }
}

actual fun getFileStorage(): FileStorage = IosFileStorage()
