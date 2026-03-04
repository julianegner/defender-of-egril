package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.Level
import de.egril.defender.model.PlannedEnemySpawn
import de.egril.defender.model.Position
import de.egril.defender.model.Waypoint
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.utils.runBlockingCompat
/**
 * File-based storage for maps and levels
 * Stores data in ~/.defender-of-egril/gamedata/ directory on desktop
 */
object EditorStorage {
    private val fileStorage = getFileStorage()
    private val mapsCache = mutableMapOf<String, EditorMap>()
    private val levelsCache = mutableMapOf<String, EditorLevel>()
    private var levelSequenceCache: LevelSequence? = null
    private var userLevelSequenceCache: LevelSequence? = null
    
    // Official content directories (read from repository)
    private val OFFICIAL_MAPS_DIR = "gamedata/official/maps"
    private val OFFICIAL_LEVELS_DIR = "gamedata/official/levels"
    private val OFFICIAL_SEQUENCE_FILE = "gamedata/official/sequence.json"
    private val OFFICIAL_WORLDMAP_FILE = "gamedata/official/worldmap.json"
    
    // User content directories (created by users in editor)
    private val USER_MAPS_DIR = "gamedata/user/maps"
    private val USER_LEVELS_DIR = "gamedata/user/levels"
    private val USER_SEQUENCE_FILE = "gamedata/user/sequence.json"
    
    // Legacy directories (for backward compatibility)
    private val LEGACY_MAPS_DIR = "gamedata/maps"
    private val LEGACY_LEVELS_DIR = "gamedata/levels"
    private val LEGACY_SEQUENCE_FILE = "gamedata/sequence.json"
    private val LEGACY_WORLDMAP_FILE = "gamedata/worldmap.json"
    
    private val VERSION_FILE = "gamedata/version.txt"
    private val CURRENT_VERSION = "10" // Increment when level data format changes - v10: added metadata wrapper to all JSON files
    
    private var worldMapDataCache: WorldMapData? = null
    private var initialized = false

    /**
     * Initialize and validate repository data
     * Must be called before using EditorStorage in production code
     * Silently skips initialization if repository files aren't available (e.g., in test environments)
     */
    fun ensureInitialized() {
        if (initialized) return
        
        // Always try to load repository files first
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("Initializing EditorStorage - loading repository files...")
        }
        val repositoryLoaded = tryLoadRepositoryFiles()
        
        if (!repositoryLoaded) {
            println("Repository files could not be loaded - this may be a test environment")
            // Continue anyway - we'll try to load from other paths or use defaults
        }

        // Validate that we have all required data categories
        val missingCategories = validateRepositoryData()
        if (missingCategories.isNotEmpty()) {
            // In production builds, repository files are complete, so we should never reach here
            // If we do, we're in a test environment with incomplete data
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Missing data categories: $missingCategories - continuing anyway (test environment)")
            }
        }

        // Load sequence to populate cache
        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            println("Level sequence is empty - continuing anyway (test environment)")
        }

        initialized = true
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("EditorStorage initialized successfully. Repository loaded: $repositoryLoaded")
        }
    }

    /**
     * Validate that all required repository data exists and is not empty
     * @return List of missing or empty categories
     */
    private fun validateRepositoryData(): List<String> {
        val missing = mutableListOf<String>()

        // Check maps
        val maps = getAllMaps()
        if (maps.isEmpty()) {
            missing.add("maps")
        }

        // Check levels
        val levels = getAllLevels()
        if (levels.isEmpty()) {
            missing.add("levels")
        }

        // Check sequence
        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            missing.add("sequence")
        }

        // Check worldmap (optional but good to have)
        val worldMapData = getWorldMapData()
        if (worldMapData.locations.isEmpty() && worldMapData.paths.isEmpty()) {
            missing.add("worldmap")
        }

        return missing
    }
    
    /**
     * Save the map. Returns true if the map image was (re)generated, false if it was skipped
     * because the image already existed and no tile type changes were detected.
     *
     * @param map The map to save.
     * @param oldId If the map was renamed (ID changed), pass the old ID here so that the old
     *   JSON and PNG files are deleted after saving under the new name.
     */
    fun saveMap(map: EditorMap, oldId: String? = null): Boolean {
        // Validate and update readyToUse before saving
        val validatedMap = map.copy(readyToUse = map.validateReadyToUse())

        // Get existing map BEFORE updating cache (for image regeneration decision)
        val existingMap = mapsCache[validatedMap.id]

        mapsCache[validatedMap.id] = validatedMap
        val json = EditorJsonSerializer.serializeMap(validatedMap)

        // Save to appropriate directory based on isOfficial flag
        val targetDir = if (validatedMap.isOfficial) OFFICIAL_MAPS_DIR else USER_MAPS_DIR
        fileStorage.writeFile("$targetDir/${validatedMap.id}.json", json)

        // Only regenerate the map image if:
        // - the PNG does not exist yet, OR
        // - at least one tile's TileType has changed (river flow direction changes are ignored)
        val pngPath = "$targetDir/${validatedMap.id}.png"
        val pngExists = fileStorage.fileExists(pngPath)
        val tilesChanged = existingMap == null || existingMap.tiles != validatedMap.tiles
        val imageRegenerated = !pngExists || tilesChanged
        if (imageRegenerated) {
            generateAndSaveMapImage(validatedMap)
        } else {
            println("Skipping map image regeneration for ${validatedMap.id} (no tile type changes)")
        }

        // If the map was renamed (old ID differs from new ID), delete the old files
        if (oldId != null && oldId != validatedMap.id) {
            mapsCache.remove(oldId)
            fileStorage.deleteFile("$USER_MAPS_DIR/$oldId.json")
            fileStorage.deleteFile("$USER_MAPS_DIR/$oldId.png")
            println("Deleted old map files for renamed map: $oldId -> ${validatedMap.id}")
        }

        // Track changes to official data
        if (validatedMap.isOfficial) {
            OfficialDataChangeTracker.trackMapModified(validatedMap.id)
        }

        return imageRegenerated
    }

    /**
     * Copy a map, reusing the existing PNG image instead of regenerating it.
     * The copied map is always saved as a user map (isOfficial = false).
     */
    fun copyMap(sourceMap: EditorMap, copiedMap: EditorMap) {
        val validatedMap = copiedMap.copy(readyToUse = copiedMap.validateReadyToUse())
        mapsCache[validatedMap.id] = validatedMap
        val json = EditorJsonSerializer.serializeMap(validatedMap)
        fileStorage.writeFile("$USER_MAPS_DIR/${validatedMap.id}.json", json)

        // Copy the PNG from the source map rather than regenerating it
        val sourcePng = readMapImageBytes(sourceMap.id, sourceMap.isOfficial)
        if (sourcePng != null) {
            fileStorage.writeBinaryFile("$USER_MAPS_DIR/${validatedMap.id}.png", sourcePng)
            println("Copied map image from ${sourceMap.id} to ${validatedMap.id}")
        } else {
            println("No source image found for ${sourceMap.id}, generating new image")
            generateAndSaveMapImage(validatedMap)
        }
    }

    /**
     * Read the raw PNG bytes for a map image.
     * Checks the expected directory first (based on [isOfficial]) to avoid unnecessary lookups.
     */
    private fun readMapImageBytes(mapId: String, isOfficial: Boolean): ByteArray? {
        return if (isOfficial) {
            fileStorage.readBinaryFile("$OFFICIAL_MAPS_DIR/$mapId.png")
                ?: fileStorage.readBinaryFile("$USER_MAPS_DIR/$mapId.png")
        } else {
            fileStorage.readBinaryFile("$USER_MAPS_DIR/$mapId.png")
                ?: fileStorage.readBinaryFile("$OFFICIAL_MAPS_DIR/$mapId.png")
        } ?: fileStorage.readBinaryFile("$LEGACY_MAPS_DIR/$mapId.png")
    }

    private fun generateAndSaveMapImage(map: EditorMap) {
        try {
            val (pixels, width, height) = de.egril.defender.mapgen.MapImageGenerator.generatePixels(map)
            val pngBytes = de.egril.defender.mapgen.MapImageEncoder.encodeToPng(pixels, width, height)
            if (pngBytes != null) {
                val targetDir = if (map.isOfficial) OFFICIAL_MAPS_DIR else USER_MAPS_DIR
                fileStorage.writeBinaryFile("$targetDir/${map.id}.png", pngBytes)
                println("Generated map image: ${map.id}.png")
            }
        } catch (e: Exception) {
            println("Failed to generate map image for ${map.id}: ${e.message}")
        }
    }
    
    fun reloadMap(id: String): EditorMap? {
        // Force reload from file, bypassing cache
        mapsCache.remove(id)
        return getMap(id)
    }
    
    fun getMap(id: String): EditorMap? {
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("EditorStorage: Retrieving map with ID: $id")
        }

        // Check cache first
        if (mapsCache.containsKey(id)) {
            return mapsCache[id]
        }
        
        // Try to load from official directory first, then user, then legacy
        var json = fileStorage.readFile("$OFFICIAL_MAPS_DIR/$id.json")
        var isOfficial = true
        
        if (json == null) {
            json = fileStorage.readFile("$USER_MAPS_DIR/$id.json")
            isOfficial = false
        }
        
        if (json == null) {
            json = fileStorage.readFile("$LEGACY_MAPS_DIR/$id.json")
            // For legacy files, check if it's an official map
            isOfficial = OfficialContent.isOfficialMap(id)
        }
        
        if (json != null) {
            val map = EditorJsonSerializer.deserializeMap(json)
            if (map != null) {
                // Always recalculate readyToUse when loading from file
                // Set isOfficial flag based on which directory it was found in
                val validatedMap = map.copy(
                    readyToUse = map.validateReadyToUse(),
                    isOfficial = map.isOfficial || isOfficial
                )
                mapsCache[id] = validatedMap
                return validatedMap
            }
        }
        
        return null
    }
    
    fun getAllMaps(): List<EditorMap> {
        // Load all maps from both official and user directories
        fileStorage.createDirectory(OFFICIAL_MAPS_DIR)
        fileStorage.createDirectory(USER_MAPS_DIR)
        
        val officialFiles = fileStorage.listFiles(OFFICIAL_MAPS_DIR)
        val userFiles = fileStorage.listFiles(USER_MAPS_DIR)
        
        // Load official maps
        for (filename in officialFiles) {
            if (!filename.endsWith(".json")) continue
            val id = filename.removeSuffix(".json")
            if (!mapsCache.containsKey(id)) {
                getMap(id) // This will load and cache it
            }
        }
        
        // Load user maps
        for (filename in userFiles) {
            if (!filename.endsWith(".json")) continue
            val id = filename.removeSuffix(".json")
            if (!mapsCache.containsKey(id)) {
                getMap(id) // This will load and cache it
            }
        }
        
        return mapsCache.values.toList()
    }
    
    /**
     * Helper function to ensure all enemy spawns have spawn points assigned.
     * For enemies without spawn points, assigns them in round-robin fashion.
     */
    private fun ensureSpawnPoints(level: EditorLevel): EditorLevel {
        // Get the map to find available spawn points
        val map = getMap(level.mapId) ?: return level
        val spawnPoints = map.getSpawnPoints()
        
        if (spawnPoints.isEmpty()) {
            // No spawn points available, can't assign
            return level
        }
        
        // Check if any enemy needs a spawn point
        val needsUpdate = level.enemySpawns.any { it.spawnPoint == null }
        if (!needsUpdate) {
            return level
        }
        
        // Assign spawn points to enemies that don't have them
        val updatedSpawns = level.enemySpawns.mapIndexed { index, spawn ->
            if (spawn.spawnPoint == null) {
                // Assign in round-robin fashion
                spawn.copy(spawnPoint = spawnPoints[index % spawnPoints.size])
            } else {
                spawn
            }
        }
        
        return level.copy(enemySpawns = updatedSpawns)
    }
    
    fun saveLevel(level: EditorLevel) {
        // Ensure all enemy spawns have spawn points
        val levelWithSpawnPoints = ensureSpawnPoints(level)
        
        val initData = levelWithSpawnPoints.getEffectiveInitialData()
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("EditorStorage.saveLevel: Saving level ${levelWithSpawnPoints.id} with ${levelWithSpawnPoints.initialDefenders.size} defenders, ${levelWithSpawnPoints.initialAttackers.size} attackers, ${levelWithSpawnPoints.initialTraps.size} traps, ${levelWithSpawnPoints.initialBarricades.size} barricades")
        }

        levelsCache[levelWithSpawnPoints.id] = levelWithSpawnPoints
        val json = EditorJsonSerializer.serializeLevel(levelWithSpawnPoints)
        
        // Save to appropriate directory based on isOfficial flag
        val targetDir = if (levelWithSpawnPoints.isOfficial) OFFICIAL_LEVELS_DIR else USER_LEVELS_DIR
        fileStorage.writeFile("$targetDir/${levelWithSpawnPoints.id}.json", json)
        
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("EditorStorage.saveLevel: Saved to $targetDir/${levelWithSpawnPoints.id}.json")
        }

        // Track changes to official data
        if (levelWithSpawnPoints.isOfficial) {
            OfficialDataChangeTracker.trackLevelModified(levelWithSpawnPoints.id)
        }
        
        // Update appropriate sequence if this is a new level
        if (levelWithSpawnPoints.isOfficial) {
            val sequence = getLevelSequence()
            if (!sequence.sequence.contains(levelWithSpawnPoints.id)) {
                val newSequence = LevelSequence(sequence.sequence + levelWithSpawnPoints.id)
                updateLevelSequence(newSequence)
            }
        } else {
            val sequence = getUserLevelSequence()
            if (!sequence.sequence.contains(levelWithSpawnPoints.id)) {
                val newSequence = LevelSequence(sequence.sequence + levelWithSpawnPoints.id)
                updateUserLevelSequence(newSequence)
            }
        }
    }
    
    fun reloadLevel(id: String): EditorLevel? {
        // Force reload from file, bypassing cache
        levelsCache.remove(id)
        return getLevel(id)
    }
    
    fun getLevel(id: String): EditorLevel? {
        // Check cache first
        if (levelsCache.containsKey(id)) {
            return levelsCache[id]
        }
        
        // Try to load from official directory first, then user, then legacy
        var json = fileStorage.readFile("$OFFICIAL_LEVELS_DIR/$id.json")
        var isOfficial = true
        
        if (json == null) {
            json = fileStorage.readFile("$USER_LEVELS_DIR/$id.json")
            isOfficial = false
        }
        
        if (json == null) {
            json = fileStorage.readFile("$LEGACY_LEVELS_DIR/$id.json")
            // For legacy files, check if it's an official level
            isOfficial = OfficialContent.isOfficialLevel(id)
        }
        
        if (json != null) {
            val level = EditorJsonSerializer.deserializeLevel(json)
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("EditorStorage: Deserialized level $id: $level")
            }
            if (level != null) {
                // Set isOfficial flag based on which directory it was found in
                val levelWithFlag = level.copy(isOfficial = level.isOfficial || isOfficial)
                levelsCache[id] = levelWithFlag
                return levelWithFlag
            }
        }
        
        return null
    }
    
    fun getAllLevels(): List<EditorLevel> {
        // Load all levels from both official and user directories
        fileStorage.createDirectory(OFFICIAL_LEVELS_DIR)
        fileStorage.createDirectory(USER_LEVELS_DIR)
        
        val officialFiles = fileStorage.listFiles(OFFICIAL_LEVELS_DIR)
        val userFiles = fileStorage.listFiles(USER_LEVELS_DIR)
        
        // Load official levels
        for (filename in officialFiles) {
            if (!filename.endsWith(".json")) continue
            val id = filename.removeSuffix(".json")
            if (!levelsCache.containsKey(id)) {
                getLevel(id) // This will load and cache it
            }
        }
        
        // Load user levels
        for (filename in userFiles) {
            if (!filename.endsWith(".json")) continue
            val id = filename.removeSuffix(".json")
            if (!levelsCache.containsKey(id)) {
                getLevel(id) // This will load and cache it
            }
        }
        
        return levelsCache.values.toList()
    }
    
    fun getLevelSequence(): LevelSequence {
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("EditorStorage: Retrieving level sequence...")
        }
        if (levelSequenceCache != null) {
            return levelSequenceCache!!
        }
        
        // Try to load from official sequence file first
        var json = fileStorage.readFile(OFFICIAL_SEQUENCE_FILE)
        
        // Fall back to legacy sequence file for backward compatibility
        if (json == null) {
            json = fileStorage.readFile(LEGACY_SEQUENCE_FILE)
        }

        if (json != null) {
            val sequence = EditorJsonSerializer.deserializeSequence(json)
            if (sequence != null && sequence.sequence.isNotEmpty()) {
                levelSequenceCache = sequence
                return sequence
            }
        }
        
        // Return empty sequence if not found - let init handle creation
        return LevelSequence(emptyList())
    }
    
    /**
     * Get the user level sequence from USER_SEQUENCE_FILE.
     * Returns empty sequence if file doesn't exist.
     */
    fun getUserLevelSequence(): LevelSequence {
        if (userLevelSequenceCache != null) {
            return userLevelSequenceCache!!
        }
        
        // Try to load from user sequence file
        val json = fileStorage.readFile(USER_SEQUENCE_FILE)
        if (json != null) {
            val sequence = EditorJsonSerializer.deserializeSequence(json)
            if (sequence != null) {
                userLevelSequenceCache = sequence
                return sequence
            }
        }
        
        // Return empty sequence if not found
        return LevelSequence(emptyList())
    }
    
    fun updateLevelSequence(sequence: LevelSequence) {
        // Save to OFFICIAL_SEQUENCE_FILE
        levelSequenceCache = sequence
        val json = EditorJsonSerializer.serializeSequence(sequence)
        fileStorage.writeFile(OFFICIAL_SEQUENCE_FILE, json)
    }
    
    /**
     * Update the user level sequence.
     */
    fun updateUserLevelSequence(sequence: LevelSequence) {
        userLevelSequenceCache = sequence
        val json = EditorJsonSerializer.serializeSequence(sequence)
        fileStorage.writeFile(USER_SEQUENCE_FILE, json)
    }
    
    fun moveLevelUp(levelId: String) {
        // Determine if level is official or user
        val level = getLevel(levelId)
        
        if (level?.isOfficial == true) {
            // Move in official sequence
            val currentSequence = getLevelSequence().sequence.toMutableList()
            val index = currentSequence.indexOf(levelId)
            if (index > 0) {
                currentSequence.removeAt(index)
                currentSequence.add(index - 1, levelId)
                updateLevelSequence(LevelSequence(currentSequence))
            }
        } else {
            // Move in user sequence
            val currentSequence = getUserLevelSequence().sequence.toMutableList()
            val index = currentSequence.indexOf(levelId)
            if (index > 0) {
                currentSequence.removeAt(index)
                currentSequence.add(index - 1, levelId)
                updateUserLevelSequence(LevelSequence(currentSequence))
            }
        }
    }
    
    fun moveLevelDown(levelId: String) {
        // Determine if level is official or user
        val level = getLevel(levelId)
        
        if (level?.isOfficial == true) {
            // Move in official sequence
            val currentSequence = getLevelSequence().sequence.toMutableList()
            val index = currentSequence.indexOf(levelId)
            if (index >= 0 && index < currentSequence.size - 1) {
                currentSequence.removeAt(index)
                currentSequence.add(index + 1, levelId)
                updateLevelSequence(LevelSequence(currentSequence))
            }
        } else {
            // Move in user sequence
            val currentSequence = getUserLevelSequence().sequence.toMutableList()
            val index = currentSequence.indexOf(levelId)
            if (index >= 0 && index < currentSequence.size - 1) {
                currentSequence.removeAt(index)
                currentSequence.add(index + 1, levelId)
                updateUserLevelSequence(LevelSequence(currentSequence))
            }
        }
    }
    
    /**
     * Add a level to the level sequence.
     * If the level is already in the sequence, does nothing.
     * @param levelId The ID of the level to add
     * @param atIndex Optional index where to insert the level. If null, adds to the end.
     */
    fun addLevelToSequence(levelId: String, atIndex: Int? = null) {
        // Determine if level is official or user
        val level = getLevel(levelId)
        
        if (level?.isOfficial == true) {
            // Add to official sequence
            val currentSequence = getLevelSequence().sequence.toMutableList()
            if (!currentSequence.contains(levelId)) {
                if (atIndex != null && atIndex >= 0 && atIndex <= currentSequence.size) {
                    currentSequence.add(atIndex, levelId)
                } else {
                    currentSequence.add(levelId)
                }
                updateLevelSequence(LevelSequence(currentSequence))
            }
        } else {
            // Add to user sequence
            val currentSequence = getUserLevelSequence().sequence.toMutableList()
            if (!currentSequence.contains(levelId)) {
                if (atIndex != null && atIndex >= 0 && atIndex <= currentSequence.size) {
                    currentSequence.add(atIndex, levelId)
                } else {
                    currentSequence.add(levelId)
                }
                updateUserLevelSequence(LevelSequence(currentSequence))
            }
        }
    }
    
    /**
     * Remove a level from the level sequence.
     * The level file is not deleted and can be added back to the sequence later.
     * @param levelId The ID of the level to remove from the sequence
     */
    fun removeLevelFromSequence(levelId: String) {
        // Determine if level is official or user
        val level = getLevel(levelId)
        
        if (level?.isOfficial == true) {
            // Remove from official sequence
            val currentSequence = getLevelSequence().sequence.toMutableList()
            currentSequence.remove(levelId)
            updateLevelSequence(LevelSequence(currentSequence))
        } else {
            // Remove from user sequence
            val currentSequence = getUserLevelSequence().sequence.toMutableList()
            currentSequence.remove(levelId)
            updateUserLevelSequence(LevelSequence(currentSequence))
        }
    }
    
    /**
     * Move a level to a specific position in the sequence.
     * If the level is already in the sequence, it is moved to the new position.
     * If the level is not in the sequence, it is added at the specified position.
     * @param levelId The ID of the level to move
     * @param toIndex The target index (0-based) in the sequence
     */
    fun moveLevelToPosition(levelId: String, toIndex: Int) {
        // Determine if level is official or user
        val level = getLevel(levelId)
        
        if (level?.isOfficial == true) {
            // Move in official sequence
            val currentSequence = getLevelSequence().sequence.toMutableList()
            val fromIndex = currentSequence.indexOf(levelId)
            
            if (fromIndex >= 0) {
                // Level is already in sequence, move it
                currentSequence.removeAt(fromIndex)
                val adjustedIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex
                currentSequence.add(adjustedIndex.coerceIn(0, currentSequence.size), levelId)
            } else {
                // Level not in sequence, add it at the specified position
                currentSequence.add(toIndex.coerceIn(0, currentSequence.size), levelId)
            }
            
            updateLevelSequence(LevelSequence(currentSequence))
        } else {
            // Move in user sequence
            val currentSequence = getUserLevelSequence().sequence.toMutableList()
            val fromIndex = currentSequence.indexOf(levelId)
            
            if (fromIndex >= 0) {
                // Level is already in sequence, move it
                currentSequence.removeAt(fromIndex)
                val adjustedIndex = if (fromIndex < toIndex) toIndex - 1 else toIndex
                currentSequence.add(adjustedIndex.coerceIn(0, currentSequence.size), levelId)
            } else {
                // Level not in sequence, add it at the specified position
                currentSequence.add(toIndex.coerceIn(0, currentSequence.size), levelId)
            }
            
            updateUserLevelSequence(LevelSequence(currentSequence))
        }
    }
    
    // ==================== World Map Data ====================
    
    /**
     * Get the world map data containing locations and paths.
     * If no world map data exists, returns an empty WorldMapData.
     */
    fun getWorldMapData(): WorldMapData {
        if (worldMapDataCache != null) {
            return worldMapDataCache!!
        }
        
        // Try to load from official worldmap file first
        var json = fileStorage.readFile(OFFICIAL_WORLDMAP_FILE)
        
        // Fall back to legacy worldmap file for backward compatibility
        if (json == null) {
            json = fileStorage.readFile(LEGACY_WORLDMAP_FILE)
        }
        
        if (json != null) {
            val data = EditorJsonSerializer.deserializeWorldMapData(json)
            if (data != null) {
                worldMapDataCache = data
                return data
            }
        }
        
        return WorldMapData()
    }
    
    /**
     * Save the world map data to OFFICIAL_WORLDMAP_FILE.
     */
    fun saveWorldMapData(data: WorldMapData) {
        worldMapDataCache = data
        val json = EditorJsonSerializer.serializeWorldMapData(data)
        fileStorage.writeFile(OFFICIAL_WORLDMAP_FILE, json)
    }
    
    /**
     * Add or update a location in the world map data.
     */
    fun saveWorldMapLocation(location: WorldMapLocationData) {
        val currentData = getWorldMapData()
        val existingIndex = currentData.locations.indexOfFirst { it.id == location.id }
        
        val updatedLocations = if (existingIndex >= 0) {
            currentData.locations.toMutableList().apply {
                set(existingIndex, location)
            }
        } else {
            currentData.locations + location
        }
        
        saveWorldMapData(currentData.copy(locations = updatedLocations))
    }
    
    /**
     * Remove a location from the world map data.
     */
    fun deleteWorldMapLocation(locationId: String) {
        val currentData = getWorldMapData()
        val updatedLocations = currentData.locations.filter { it.id != locationId }
        val updatedPaths = currentData.paths.filter { 
            it.fromLocationId != locationId && it.toLocationId != locationId 
        }
        saveWorldMapData(currentData.copy(locations = updatedLocations, paths = updatedPaths))
    }
    
    /**
     * Add or update a path in the world map data.
     */
    fun saveWorldMapPath(path: WorldMapPathData) {
        val currentData = getWorldMapData()
        val existingIndex = currentData.paths.indexOfFirst { 
            it.fromLocationId == path.fromLocationId && it.toLocationId == path.toLocationId 
        }
        
        val updatedPaths = if (existingIndex >= 0) {
            currentData.paths.toMutableList().apply {
                set(existingIndex, path)
            }
        } else {
            currentData.paths + path
        }
        
        saveWorldMapData(currentData.copy(paths = updatedPaths))
    }
    
    /**
     * Remove a path from the world map data.
     */
    fun deleteWorldMapPath(fromLocationId: String, toLocationId: String) {
        val currentData = getWorldMapData()
        val updatedPaths = currentData.paths.filter { 
            !(it.fromLocationId == fromLocationId && it.toLocationId == toLocationId)
        }
        saveWorldMapData(currentData.copy(paths = updatedPaths))
    }
    
    /**
     * Check if a level is ready to play by its ID.
     */
    fun isLevelReadyToPlay(levelId: String): Boolean {
        val level = getLevel(levelId) ?: return false
        return isLevelReadyToPlay(level)
    }
    
    /**
     * Checks if a level is ready to play.
     * A level is ready if:
     * - It has at least one available tower
     * - It has at least one enemy spawn configured
     * - Start coins are greater than zero
     * - Start health points are greater than zero
     * - Its associated map is ready to use (has valid path from spawn to target)
     * 
     * For levels with ORK, EVIL_WIZARD, or EWHAD enemies, river tiles are considered
     * walkable during validation (they can build bridges). For other levels, rivers
     * must not be required for a valid path.
     * 
     * @param level The level to check
     * @return true if the level is ready to play, false otherwise
     */
    fun isLevelReadyToPlay(level: EditorLevel): Boolean {
        if (!level.isReadyToPlay()) {
            return false
        }
        
        // Also check if the map is ready to use
        val map = getMap(level.mapId)
        if (map == null) {
            return false
        }
        
        // Check if level has enemies that can build bridges (ORK, EVIL_WIZARD, or EWHAD)
        val hasBridgeBuildingEnemies = level.enemySpawns.any { spawn ->
            spawn.attackerType == AttackerType.ORK || 
            spawn.attackerType == AttackerType.EVIL_WIZARD || 
            spawn.attackerType == AttackerType.EWHAD
        }
        
        // Validate map with river consideration based on enemy types
        if (!map.validateReadyToUse(includeRiversAsWalkable = hasBridgeBuildingEnemies)) {
            return false
        }
        
        // check if the waypoints of the level are valid
        val targets = map.getTargets()
        if (targets.isEmpty()) {
            return false
        }
        
        val spawnPoints = map.getSpawnPoints()
        val waypointValidationResult = level.validateWaypointsDetailed(targetPositions = targets, spawnPoints = spawnPoints)
        return waypointValidationResult.isValid
    }
    
    /**
     * Validates the prerequisites configuration for all levels.
     * Checks for:
     * - Non-existent prerequisite level IDs
     * - Circular dependencies
     * - Path connectivity to "the_final_stand" level
     * @return PrerequisiteValidationResult with detailed validation information
     */
    fun validateAllPrerequisites(): PrerequisiteValidationResult {
        val allLevels = getAllLevels()
        val allLevelIds = allLevels.map { it.id }.toSet()
        
        val missingLevelIds = mutableSetOf<String>()
        val circularDependencies = mutableSetOf<String>()
        
        // Check for missing level IDs in prerequisites
        for (level in allLevels) {
            for (prereq in level.prerequisites) {
                if (prereq !in allLevelIds) {
                    missingLevelIds.add(prereq)
                }
            }
        }
        
        // Check for circular dependencies using DFS
        // Use a single visited set that persists across all calls
        val visited = mutableSetOf<String>()
        
        fun detectCycle(levelId: String, currentPath: MutableSet<String>): Boolean {
            if (levelId in currentPath) {
                return true  // Cycle detected
            }
            if (levelId in visited) {
                return false  // Already processed without cycle
            }
            
            currentPath.add(levelId)
            val level = allLevels.find { it.id == levelId }
            if (level != null) {
                for (prereq in level.prerequisites) {
                    if (prereq in allLevelIds && detectCycle(prereq, currentPath)) {
                        circularDependencies.add(levelId)
                        circularDependencies.add(prereq)
                        currentPath.remove(levelId)
                        return true
                    }
                }
            }
            currentPath.remove(levelId)
            visited.add(levelId)
            return false
        }
        
        for (level in allLevels) {
            if (level.id !in visited) {
                if (detectCycle(level.id, mutableSetOf())) {
                    circularDependencies.add(level.id)
                }
            }
        }
        
        // Find entry points (levels with no prerequisites)
        val entryPoints = allLevels.filter { it.prerequisites.isEmpty() }.map { it.id }.toSet()
        
        // Find "the_final_stand" level
        val finalStandId = "the_final_stand"
        val hasFinalStand = finalStandId in allLevelIds
        
        // Check if final stand is reachable from entry points (traverse in reverse)
        var disconnectedFromFinal = false
        if (hasFinalStand && entryPoints.isNotEmpty()) {
            // Build a reverse graph: which levels depend on this level
            val dependents = mutableMapOf<String, MutableSet<String>>()
            for (level in allLevels) {
                for (prereq in level.prerequisites) {
                    dependents.getOrPut(prereq) { mutableSetOf() }.add(level.id)
                }
            }
            
            // BFS from entry points to see if we can reach final stand
            val reachable = mutableSetOf<String>()
            val queue = ArrayDeque(entryPoints)
            
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (current in reachable) continue
                reachable.add(current)
                
                // Add all levels that depend on this level
                dependents[current]?.forEach { dependent ->
                    if (dependent !in reachable) {
                        queue.add(dependent)
                    }
                }
            }
            
            disconnectedFromFinal = finalStandId !in reachable
        } else if (hasFinalStand && entryPoints.isEmpty()) {
            // No entry points but have final stand - disconnected
            disconnectedFromFinal = true
        }
        
        // Find unreachable levels (levels that can't be reached from any entry point)
        val unreachableLevels = mutableSetOf<String>()
        if (entryPoints.isNotEmpty()) {
            val reachable = mutableSetOf<String>()
            val queue = ArrayDeque(entryPoints)
            
            // Build forward graph
            val dependents = mutableMapOf<String, MutableSet<String>>()
            for (level in allLevels) {
                for (prereq in level.prerequisites) {
                    dependents.getOrPut(prereq) { mutableSetOf() }.add(level.id)
                }
            }
            
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (current in reachable) continue
                reachable.add(current)
                
                dependents[current]?.forEach { dependent ->
                    if (dependent !in reachable) {
                        queue.add(dependent)
                    }
                }
            }
            
            unreachableLevels.addAll(allLevelIds - reachable)
        }
        
        val isValid = missingLevelIds.isEmpty() && 
                      circularDependencies.isEmpty() && 
                      !disconnectedFromFinal
        
        return PrerequisiteValidationResult(
            isValid = isValid,
            missingLevelIds = missingLevelIds,
            circularDependencies = circularDependencies,
            unreachableLevels = unreachableLevels,
            disconnectedFromFinal = disconnectedFromFinal
        )
    }
    
    /**
     * Check if a specific level is unlocked based on prerequisites and won levels.
     * @param levelId The level to check
     * @param wonLevelIds Set of level IDs that have been won
     * @return true if the level is unlocked
     */
    fun isLevelUnlocked(levelId: String, wonLevelIds: Set<String>): Boolean {
        val level = getLevel(levelId) ?: return false
        
        // No prerequisites means level is always unlocked
        if (level.prerequisites.isEmpty()) {
            return true
        }
        
        // Count how many prerequisites are fulfilled
        val fulfilledCount = level.prerequisites.count { it in wonLevelIds }
        val requiredCount = level.getEffectiveRequiredCount()
        
        return fulfilledCount >= requiredCount
    }
    
    /**
     * Get all levels that are currently unlocked based on won levels.
     * @param wonLevelIds Set of level IDs that have been won
     * @return List of unlocked level IDs
     */
    fun getUnlockedLevelIds(wonLevelIds: Set<String>): Set<String> {
        return getAllLevels()
            .filter { isLevelUnlocked(it.id, wonLevelIds) }
            .map { it.id }
            .toSet()
    }
    
    fun deleteMap(mapId: String): Boolean {
        // Get the map to check if it's official
        val map = getMap(mapId)
        if (map?.isOfficial == true) {
            println("Cannot delete official map: $mapId")
            return false
        }
        
        // Remove from cache
        mapsCache.remove(mapId)
        
        // Delete JSON and PNG files from user directory
        fileStorage.deleteFile("$USER_MAPS_DIR/$mapId.json")
        fileStorage.deleteFile("$USER_MAPS_DIR/$mapId.png")
        return true
    }
    
    fun deleteLevel(levelId: String): Boolean {
        // Get the level to check if it's official
        val level = getLevel(levelId)
        if (level?.isOfficial == true) {
            println("Cannot delete official level: $levelId")
            return false
        }
        
        // Remove from cache
        levelsCache.remove(levelId)
        
        // Delete file from user directory only
        fileStorage.deleteFile("$USER_LEVELS_DIR/$levelId.json")
        
        // Remove from user sequence
        val currentSequence = getUserLevelSequence().sequence.toMutableList()
        currentSequence.remove(levelId)
        updateUserLevelSequence(LevelSequence(currentSequence))
        
        return true
    }
    
    /**
     * Convert an EditorLevel to a Level for gameplay
     */
    fun convertToGameLevel(editorLevel: EditorLevel, numericId: Int): Level? {
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Converting EditorLevel ${editorLevel.id} to game Level with numeric ID $numericId")
        }
        // Force reload the map from disk to get latest changes
        val map = reloadMap(editorLevel.mapId) ?: getMap(editorLevel.mapId) ?: return null
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Using map: ${map.id} (${map.width}x${map.height})")
            // Convert enemy spawns directly to PlannedEnemySpawn
            println("-------------------------------")
            println("enemySpawns: ${editorLevel.enemySpawns}")
            println("-------------------------------")
        }

        val directSpawnPlan = editorLevel.enemySpawns.map { spawn ->
            PlannedEnemySpawn(
                attackerType = spawn.attackerType,
                spawnTurn = spawn.spawnTurn,
                level = spawn.level,
                spawnPoint = spawn.spawnPoint
            )
        }.sortedBy { it.spawnTurn }
        
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Created direct spawn plan with ${directSpawnPlan.size} spawns")
        }
        
        // Still create AttackerWaves for backward compatibility
        val spawnsByTurn = editorLevel.enemySpawns.groupBy { it.spawnTurn }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Enemy spawns grouped by turn: ${spawnsByTurn.keys.sorted()}")
        }
        val waves = spawnsByTurn.entries.sortedBy { it.key }.map { (_, spawns) ->
            AttackerWave(
                attackers = spawns.map { it.attackerType },
                spawnDelay = 1  // Fixed delay for now
            )
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Converted to ${waves.size} attacker waves for compatibility.")
        }

        // Get all target positions from the map
        val targets = map.getTargets()
        if (targets.isEmpty()) return null

        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("=== LEVEL CONVERSION DEBUG ===")
            println("Target positions from map: $targets")
        }
        
        // Convert editor waypoints to game waypoints
        val gameWaypoints = editorLevel.waypoints.map { editorWaypoint ->
            Waypoint(
                position = editorWaypoint.position,
                nextTarget = editorWaypoint.nextTargetPosition
            )
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Converted ${gameWaypoints.size} waypoints:")
            gameWaypoints.forEach { wp ->
                println("  Waypoint: ${wp.position} -> ${wp.nextTarget}")
            }
        }
        
        // Include waypoint positions in pathCells so enemies can walk on them
        val pathCellsWithWaypoints = map.getPathCells().toMutableSet()
        gameWaypoints.forEach { waypoint ->
            pathCellsWithWaypoints.add(waypoint.position)
        }
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Path cells: ${map.getPathCells().size}, with waypoints: ${pathCellsWithWaypoints.size}")
            println("Spawn points: ${map.getSpawnPoints()}")
            println("=== END LEVEL CONVERSION DEBUG ===")
        }
        
        val level = Level(
            id = numericId,
            name = editorLevel.title,
            subtitle = editorLevel.subtitle,
            titleKey = editorLevel.titleKey,  // Store translation key for localization
            subtitleKey = editorLevel.subtitleKey,  // Store translation key for localization
            gridWidth = map.width,
            gridHeight = map.height,
            startPositions = map.getSpawnPoints(),
            targetPositions = targets,
            pathCells = pathCellsWithWaypoints,
            buildAreas = map.getBuildAreas(),
            attackerWaves = waves,
            initialCoins = editorLevel.startCoins,
            healthPoints = editorLevel.startHealthPoints,
            directSpawnPlan = directSpawnPlan,
            availableTowers = editorLevel.availableTowers,
            waypoints = gameWaypoints,
            editorLevelId = editorLevel.id,  // Store editor level ID for minimap lookup
            mapId = editorLevel.mapId,  // Store map ID for save/load verification
            riverTiles = map.getRiverTilesMap(),  // Add river tiles with flow direction and speed
            allowAutoAttack = editorLevel.allowAutoAttack,  // Allow auto-attack option
            initialData = editorLevel.getEffectiveInitialData()  // Pre-placed elements using new structure
        )
        
        if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("=== CREATED LEVEL ===")
            println("Level: ${level.name} (ID: ${level.id})")
            println("Target positions: ${level.targetPositions}")
            println("Waypoints count: ${level.waypoints.size}")
            println("Start positions: ${level.startPositions}")
            println("=== END CREATED LEVEL ===")
        }
        
        return level
    }

    /**
     * Try to load maps and levels from repository resources.
     * Returns true if repository files were found and loaded successfully.
     * 
     * Note: Uses runBlocking because it's called from the init block which is synchronous.
     * This is acceptable since it only runs once during app initialization.
     */
    private fun tryLoadRepositoryFiles(): Boolean {
        return try {
            // Use runBlockingCompat to make this synchronous
            runBlockingCompat {
                RepositoryLoader.loadAndSaveRepositoryFiles(fileStorage)
            }
        } catch (e: Exception) {
            if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
            println("Could not load repository files: ${e.message}")
            }
            false
        }
    }
    
    /**
     * Check if the gamedata directory contains any existing user files
     * @return true if any user data files exist, false if directory is empty
     */
    private fun hasExistingGamedataFiles(): Boolean {
        // Check official sequence file
        if (fileStorage.fileExists(OFFICIAL_SEQUENCE_FILE)) {
            return true
        }
        
        // Check legacy sequence file (for backward compatibility)
        if (fileStorage.fileExists(LEGACY_SEQUENCE_FILE)) {
            return true
        }
        
        // Check if any official maps exist
        if (fileStorage.listFiles(OFFICIAL_MAPS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check if any user maps exist
        if (fileStorage.listFiles(USER_MAPS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check legacy maps directory
        if (fileStorage.listFiles(LEGACY_MAPS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check if any official levels exist
        if (fileStorage.listFiles(OFFICIAL_LEVELS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check if any user levels exist
        if (fileStorage.listFiles(USER_LEVELS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check legacy levels directory
        if (fileStorage.listFiles(LEGACY_LEVELS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check official worldmap file
        if (fileStorage.fileExists(OFFICIAL_WORLDMAP_FILE)) {
            return true
        }
        
        // Check legacy worldmap file (for backward compatibility)
        if (fileStorage.fileExists(LEGACY_WORLDMAP_FILE)) {
            return true
        }
        
        // No user data found
        return false
    }
}
