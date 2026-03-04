package de.egril.defender.editor

import de.egril.defender.utils.runBlockingCompat
import de.egril.defender.config.LogConfig

/**
 * Manages repository data operations, including detection and restoration.
 */
object RepositoryManager {
    private val fileStorage = getFileStorage()
    private const val GAMEDATA_DIR = "gamedata"
    private const val GAMEDATA_BACKUP_PREFIX = "gamedata-"
    
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
            if (fileStorage.fileExists(GAMEDATA_DIR)) {
                val renamed = fileStorage.renameDirectory(GAMEDATA_DIR, backupFolderName)
                if (!renamed) {
                    println("Failed to backup gamedata folder")
                    return null
                }
                if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                println("Backed up gamedata to $backupFolderName")
                }
            }
            
            // Create new gamedata directory
            fileStorage.createDirectory(GAMEDATA_DIR)
            fileStorage.createDirectory("$GAMEDATA_DIR/maps")
            fileStorage.createDirectory("$GAMEDATA_DIR/levels")
            
            // Load and save repository files
            val success = RepositoryLoader.loadAndSaveRepositoryFiles(fileStorage)
            
            if (!success) {
                println("Failed to load repository files")
                // Try to restore backup if loading failed
                if (fileStorage.fileExists(backupFolderName)) {
                    fileStorage.deleteDirectory(GAMEDATA_DIR)
                    fileStorage.renameDirectory(backupFolderName, GAMEDATA_DIR)
                }
                return null
            }
            
            // Return the absolute path to the backup folder
            return fileStorage.getAbsolutePath(backupFolderName)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Error restoring from repository: ${e.message}")
            }
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
        while (fileStorage.fileExists("$GAMEDATA_BACKUP_PREFIX$counter")) {
            counter++
        }
        return "$GAMEDATA_BACKUP_PREFIX$counter"
    }
    
    /**
     * Check if repository files exist
     */
    suspend fun hasRepositoryFiles(): Boolean {
        return RepositoryLoader.hasRepositoryFiles()
    }
    
    /**
     * Data class to hold information about new repository files
     */
    data class NewRepositoryData(
        val newMaps: List<String>,
        val newLevels: List<String>,
        val hasNewSequence: Boolean,
        val hasNewWorldMap: Boolean,
        val worldMapData: WorldMapData? = null  // Cache the loaded worldmap data to avoid redundant loading
    )
    
    /**
     * Detect new map and level files in repository that are not in gamedata/official
     * @return NewRepositoryData with lists of new files, or null if no new files found
     */
    suspend fun detectNewRepositoryFiles(): NewRepositoryData? {
        try {
            // Check if repository has files
            if (!RepositoryLoader.hasRepositoryFiles()) {
                println("No repository files found")
                return null
            }
            
            // Check if official gamedata exists
            if (!fileStorage.fileExists("$GAMEDATA_DIR/official")) {
                println("Official gamedata directory doesn't exist - all repository files are new")
                // All files are new if official gamedata doesn't exist
                val sequence = RepositoryLoader.loadSequence()
                if (sequence == null) return null
                
                // Load all map IDs and level IDs from sequence
                val levelIds = sequence.sequence
                val mapIds = mutableSetOf<String>()
                for (levelId in levelIds) {
                    val level = RepositoryLoader.loadLevel(levelId)
                    if (level != null) {
                        mapIds.add(level.mapId)
                    }
                }
                
                return NewRepositoryData(
                    newMaps = mapIds.toList(),
                    newLevels = levelIds,
                    hasNewSequence = true,
                    hasNewWorldMap = true,
                    worldMapData = RepositoryLoader.loadWorldMapData()
                )
            }
            
            // Compare repository files with official gamedata files
            val existingMaps = fileStorage.listFiles("$GAMEDATA_DIR/official/maps")
                .filter { it.endsWith(".json") }
                .map { it.removeSuffix(".json") }
                .toSet()
            
            val existingLevels = fileStorage.listFiles("$GAMEDATA_DIR/official/levels")
                .filter { it.endsWith(".json") }
                .map { it.removeSuffix(".json") }
                .toSet()
            
            // Load repository sequence to get all level and map IDs
            val repoSequence = RepositoryLoader.loadSequence()
            if (repoSequence == null) {
                println("Could not load repository sequence")
                return null
            }
            
            // Track new maps and levels
            val newMaps = mutableSetOf<String>()
            val newLevels = mutableListOf<String>()
            
            // Check each level in repository sequence
            for (levelId in repoSequence.sequence) {
                // Check if level is new
                if (!existingLevels.contains(levelId)) {
                    newLevels.add(levelId)
                }
                
                // Load level to get map ID
                val level = RepositoryLoader.loadLevel(levelId)
                if (level != null && !existingMaps.contains(level.mapId)) {
                    newMaps.add(level.mapId)
                }
            }
            
            // Check if sequence is different
            val currentSequenceJson = fileStorage.readFile("$GAMEDATA_DIR/official/sequence.json")
            val currentSequence = currentSequenceJson?.let { EditorJsonSerializer.deserializeSequence(it) }
            val hasNewSequence = currentSequence == null || 
                currentSequence.sequence != repoSequence.sequence
            
            // Check if worldmap is different
            val currentWorldMapJson = fileStorage.readFile("$GAMEDATA_DIR/official/worldmap.json")
            val repoWorldMapData = RepositoryLoader.loadWorldMapData()
            val hasNewWorldMap = if (repoWorldMapData != null) {
                val currentWorldMapData = currentWorldMapJson?.let { EditorJsonSerializer.deserializeWorldMapData(it) }
                currentWorldMapData == null || currentWorldMapData != repoWorldMapData
            } else {
                false
            }
            
            // Return null if no new files found
            if (newMaps.isEmpty() && newLevels.isEmpty() && !hasNewSequence && !hasNewWorldMap) {
                println("No new repository files detected")
                return null
            }
            
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Detected new repository files: ${newMaps.size} maps, ${newLevels.size} levels, sequence changed: $hasNewSequence, worldmap changed: $hasNewWorldMap")
            }
            return NewRepositoryData(
                newMaps = newMaps.toList(),
                newLevels = newLevels,
                hasNewSequence = hasNewSequence,
                hasNewWorldMap = hasNewWorldMap,
                worldMapData = repoWorldMapData  // Cache the loaded worldmap data
            )
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Error detecting new repository files: ${e.message}")
            }
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Sync new files from repository to gamedata.
     * This will:
     * 1. Backup current sequence file to sequence-N.json (if it exists)
     * 2. Backup current worldmap file to worldmap-N.json (if it exists)
     * 3. Add new map and level files from repository
     * 4. Replace sequence file with repository version
     * 5. Replace worldmap file with repository version
     * 
     * @return true if sync was successful
     */
    suspend fun syncNewRepositoryFiles(): Boolean {
        try {
            // Detect what's new
            val newData = detectNewRepositoryFiles()
            if (newData == null) {
                println("No new files to sync")
                return false
            }
            
            // Ensure official gamedata directories exist
            fileStorage.createDirectory(GAMEDATA_DIR)
            fileStorage.createDirectory("$GAMEDATA_DIR/official")
            fileStorage.createDirectory("$GAMEDATA_DIR/official/maps")
            fileStorage.createDirectory("$GAMEDATA_DIR/official/levels")
            
            // No backup needed - we always overwrite official data
            // User data in gamedata/user/ is never touched
            
            // Load and save new/updated maps to official directory
            for (mapId in newData.newMaps) {
                val map = RepositoryLoader.loadMap(mapId)
                if (map != null) {
                    val officialMap = map.copy(isOfficial = true)
                    val mapJson = EditorJsonSerializer.serializeMap(officialMap)
                    fileStorage.writeFile("$GAMEDATA_DIR/official/maps/$mapId.json", mapJson)
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Added/updated official map: $mapId")
                    }
                }
            }
            
            // Load and save new/updated levels to official directory
            for (levelId in newData.newLevels) {
                val level = RepositoryLoader.loadLevel(levelId)
                if (level != null) {
                    val officialLevel = level.copy(isOfficial = true)
                    val levelJson = EditorJsonSerializer.serializeLevel(officialLevel)
                    fileStorage.writeFile("$GAMEDATA_DIR/official/levels/$levelId.json", levelJson)
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Added/updated official level: $levelId")
                    }
                }
            }
            
            // Replace official sequence file with repository version
            if (newData.hasNewSequence) {
                val sequence = RepositoryLoader.loadSequence()
                if (sequence != null) {
                    val sequenceJson = EditorJsonSerializer.serializeSequence(sequence)
                    fileStorage.writeFile("$GAMEDATA_DIR/official/sequence.json", sequenceJson)
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Updated official sequence file")
                    }
                }
            }
            
            // Replace official worldmap file with repository version
            if (newData.hasNewWorldMap) {
                // Use cached worldmap data if available, otherwise load it
                val worldMapData = newData.worldMapData
                if (worldMapData != null) {
                    val worldMapJson = EditorJsonSerializer.serializeWorldMapData(worldMapData)
                    fileStorage.writeFile("$GAMEDATA_DIR/official/worldmap.json", worldMapJson)
                    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                    println("Updated official worldmap file")
                    }
                } else {
                    // Load from repository if not cached
                    val loadedWorldMapData = RepositoryLoader.loadWorldMapData()
                    if (loadedWorldMapData != null) {
                        val worldMapJson = EditorJsonSerializer.serializeWorldMapData(loadedWorldMapData)
                        fileStorage.writeFile("$GAMEDATA_DIR/official/worldmap.json", worldMapJson)
                        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Updated official worldmap file (loaded from repository)")
                        }
                    } else {
                        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
                        println("Error: Could not load worldmap from repository")
                        }
                    }
                }
            }
            
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Successfully synced ${newData.newMaps.size} maps and ${newData.newLevels.size} levels to official directory")
            }
            return true
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Error syncing new repository files: ${e.message}")
            }
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Sync new files from repository synchronously.
     */
    fun syncNewRepositoryFilesBlocking(): Boolean {
        return runBlockingCompat {
            syncNewRepositoryFiles()
        }
    }
    
    /**
     * Find the next available sequence backup name (sequence-1.json, sequence-2.json, etc.)
     * @return The backup filename, or null if maximum backups reached
     */
    private fun findNextSequenceBackupName(): String? {
        var counter = 1
        val maxBackups = 999  // Safety limit to prevent infinite loop
        while (counter <= maxBackups && fileStorage.fileExists("$GAMEDATA_DIR/sequence-$counter.json")) {
            counter++
        }
        return if (counter <= maxBackups) {
            "sequence-$counter.json"
        } else {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Warning: Maximum number of sequence backups ($maxBackups) reached")
            }
            null
        }
    }
    
    /**
     * Find the next available worldmap backup name (worldmap-1.json, worldmap-2.json, etc.)
     * @return The backup filename, or null if maximum backups reached
     */
    private fun findNextWorldmapBackupName(): String? {
        var counter = 1
        val maxBackups = 999  // Safety limit to prevent infinite loop
        while (counter <= maxBackups && fileStorage.fileExists("$GAMEDATA_DIR/worldmap-$counter.json")) {
            counter++
        }
        return if (counter <= maxBackups) {
            "worldmap-$counter.json"
        } else {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Warning: Maximum number of worldmap backups ($maxBackups) reached")
            }
            null
        }
    }
    
    /**
     * Detect new repository files synchronously.
     */
    fun detectNewRepositoryFilesBlocking(): NewRepositoryData? {
        return runBlockingCompat {
            detectNewRepositoryFiles()
        }
    }
}
