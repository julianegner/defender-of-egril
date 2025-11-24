package de.egril.defender.editor

import de.egril.defender.utils.runBlockingCompat

/**
 * Manages repository data operations, including detection and restoration.
 */
object RepositoryManager {
    private val fileStorage = getFileStorage()
    
    /**
     * Restore game data from repository.
     * This will:
     * 1. Backup current gamedata folder to gamedata-N (where N is the lowest available number)
     * 2. Copy repository files to a new gamedata folder
     * 
     * @return The absolute path to the backup folder, or null if restoration failed
     */
    suspend fun restoreFromRepository(): String? {
        try {
            // Check if repository has files
            if (!RepositoryLoader.hasRepositoryFiles()) {
                println("No repository files found")
                return null
            }
            
            // Find the next available backup folder name
            val backupFolderName = findNextBackupFolderName()
            
            // Backup current gamedata if it exists
            if (fileStorage.fileExists("gamedata")) {
                val renamed = fileStorage.renameDirectory("gamedata", backupFolderName)
                if (!renamed) {
                    println("Failed to backup gamedata folder")
                    return null
                }
                println("Backed up gamedata to $backupFolderName")
            }
            
            // Create new gamedata directory
            fileStorage.createDirectory("gamedata")
            fileStorage.createDirectory("gamedata/maps")
            fileStorage.createDirectory("gamedata/levels")
            
            // Load and save repository files
            val success = RepositoryLoader.loadAndSaveRepositoryFiles(fileStorage)
            
            if (!success) {
                println("Failed to load repository files")
                // Try to restore backup if loading failed
                if (fileStorage.fileExists(backupFolderName)) {
                    fileStorage.deleteDirectory("gamedata")
                    fileStorage.renameDirectory(backupFolderName, "gamedata")
                }
                return null
            }
            
            // Return the absolute path to the backup folder
            return fileStorage.getAbsolutePath(backupFolderName)
        } catch (e: Exception) {
            println("Error restoring from repository: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Restore game data from repository synchronously.
     * This is a blocking wrapper around the suspending function.
     */
    fun restoreFromRepositoryBlocking(): String? {
        return runBlockingCompat {
            restoreFromRepository()
        }
    }
    
    /**
     * Find the next available backup folder name (gamedata-1, gamedata-2, etc.)
     */
    private fun findNextBackupFolderName(): String {
        var counter = 1
        while (fileStorage.fileExists("gamedata-$counter")) {
            counter++
        }
        return "gamedata-$counter"
    }
    
    /**
     * Check if repository files exist
     */
    suspend fun hasRepositoryFiles(): Boolean {
        return RepositoryLoader.hasRepositoryFiles()
    }
}
