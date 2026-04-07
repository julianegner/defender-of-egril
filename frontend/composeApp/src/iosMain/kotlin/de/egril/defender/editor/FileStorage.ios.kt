package de.egril.defender.editor

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

/**
 * iOS implementation using actual file storage in the app's documents directory.
 * Files are stored in NSDocumentDirectory which persists between app restarts.
 */
class IosFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager
    
    @OptIn(ExperimentalForeignApi::class)
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
    
    @OptIn(ExperimentalForeignApi::class)
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
    
    @OptIn(ExperimentalForeignApi::class)
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
    
    @OptIn(ExperimentalForeignApi::class)
    override fun listFiles(directory: String): List<String> {
        val dirPath = "$baseDir/$directory"
        return if (fileManager.fileExistsAtPath(dirPath)) {
            val contents = fileManager.contentsOfDirectoryAtPath(dirPath, error = null)
            (contents as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun fileExists(path: String): Boolean {
        val filePath = "$baseDir/$path"
        return fileManager.fileExistsAtPath(filePath)
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun createDirectory(path: String) {
        val dirPath = "$baseDir/$path"
        fileManager.createDirectoryAtPath(
            dirPath,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun deleteFile(path: String) {
        val filePath = "$baseDir/$path"
        fileManager.removeItemAtPath(filePath, error = null)
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun renameDirectory(oldPath: String, newPath: String): Boolean {
        val oldDirPath = "$baseDir/$oldPath"
        val newDirPath = "$baseDir/$newPath"
        
        if (!fileManager.fileExistsAtPath(oldDirPath)) {
            return false
        }
        
        return fileManager.moveItemAtPath(oldDirPath, toPath = newDirPath, error = null)
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun copyDirectory(sourcePath: String, targetPath: String): Boolean {
        val sourceDirPath = "$baseDir/$sourcePath"
        val targetDirPath = "$baseDir/$targetPath"
        
        if (!fileManager.fileExistsAtPath(sourceDirPath)) {
            return false
        }
        
        return fileManager.copyItemAtPath(sourceDirPath, toPath = targetDirPath, error = null)
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun deleteDirectory(path: String): Boolean {
        val dirPath = "$baseDir/$path"
        
        if (!fileManager.fileExistsAtPath(dirPath)) {
            return true // Already doesn't exist
        }
        
        return fileManager.removeItemAtPath(dirPath, error = null)
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override fun getAbsolutePath(path: String): String {
        return "$baseDir/$path"
    }

    override fun writeBinaryFile(path: String, content: ByteArray) {
        // Binary file writing not supported on iOS in this implementation
    }

    override fun readBinaryFile(path: String): ByteArray? {
        // Binary file reading not supported on iOS in this implementation
        return null
    }
}

actual fun getFileStorage(): FileStorage = IosFileStorage()
