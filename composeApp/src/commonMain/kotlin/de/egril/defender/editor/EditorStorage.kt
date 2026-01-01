package de.egril.defender.editor

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
    
    private val MAPS_DIR = "gamedata/maps"
    private val LEVELS_DIR = "gamedata/levels"
    private val SEQUENCE_FILE = "gamedata/sequence.json"
    private val WORLDMAP_FILE = "gamedata/worldmap.json"
    private val VERSION_FILE = "gamedata/version.txt"
    private val CURRENT_VERSION = "7" // Increment when level data format changes - v7: added world map locations file
    
    private var worldMapDataCache: WorldMapData? = null
    private var initialized = false
    
    /**
     * Initialize and validate repository data
     * Must be called before using EditorStorage in production code
     */
    fun ensureInitialized() {
        if (initialized) return
        
        // Check if gamedata directory has existing user data
        val hasUserData = hasExistingGamedataFiles()
        
        if (!hasUserData) {
            // No user data - try to load from repository
            println("No gamedata found - loading from repository...")
            if (!tryLoadRepositoryFiles()) {
                // Repository files are missing - this is a critical error
                throw MissingRepositoryDataException(listOf(
                    "maps", "levels", "sequence", "worldmap"
                ))
            }
        }
        
        // Validate that we have all required data categories
        val missingCategories = validateRepositoryData()
        if (missingCategories.isNotEmpty()) {
            throw MissingRepositoryDataException(missingCategories)
        }
        
        // Load sequence to populate cache
        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            throw MissingRepositoryDataException(listOf("sequence (empty)"))
        }
        
        initialized = true
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
    
    fun saveMap(map: EditorMap) {
        // Validate and update readyToUse before saving
        val validatedMap = map.copy(readyToUse = map.validateReadyToUse())
        mapsCache[validatedMap.id] = validatedMap
        val json = EditorJsonSerializer.serializeMap(validatedMap)
        fileStorage.writeFile("$MAPS_DIR/${validatedMap.id}.json", json)
    }
    
    fun reloadMap(id: String): EditorMap? {
        // Force reload from file, bypassing cache
        mapsCache.remove(id)
        return getMap(id)
    }
    
    fun getMap(id: String): EditorMap? {
        println("EditorStorage: Retrieving map with ID: $id")
        
        // Check cache first
        if (mapsCache.containsKey(id)) {
            return mapsCache[id]
        }
        
        // Try to load from file
        val json = fileStorage.readFile("$MAPS_DIR/$id.json")
        if (json != null) {
            val map = EditorJsonSerializer.deserializeMap(json)
            if (map != null) {
                // Always recalculate readyToUse when loading from file
                val validatedMap = map.copy(readyToUse = map.validateReadyToUse())
                mapsCache[id] = validatedMap
                return validatedMap
            }
        }
        
        return null
    }
    
    fun getAllMaps(): List<EditorMap> {
        // Load all maps from files
        fileStorage.createDirectory(MAPS_DIR)
        val mapFiles = fileStorage.listFiles(MAPS_DIR)
        
        for (filename in mapFiles) {
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
        
        levelsCache[levelWithSpawnPoints.id] = levelWithSpawnPoints
        val json = EditorJsonSerializer.serializeLevel(levelWithSpawnPoints)
        fileStorage.writeFile("$LEVELS_DIR/${levelWithSpawnPoints.id}.json", json)
        
        // Update sequence if this is a new level
        val sequence = getLevelSequence()
        if (!sequence.sequence.contains(levelWithSpawnPoints.id)) {
            val newSequence = LevelSequence(sequence.sequence + levelWithSpawnPoints.id)
            updateLevelSequence(newSequence)
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
        
        // Try to load from file
        val json = fileStorage.readFile("$LEVELS_DIR/$id.json")
        if (json != null) {
            val level = EditorJsonSerializer.deserializeLevel(json)
            println("EditorStorage: Deserialized level $id: $level")
            if (level != null) {
                levelsCache[id] = level
                return level
            }
        }
        
        return null
    }
    
    fun getAllLevels(): List<EditorLevel> {
        // Load all levels from files
        fileStorage.createDirectory(LEVELS_DIR)
        val levelFiles = fileStorage.listFiles(LEVELS_DIR)
        
        for (filename in levelFiles) {
            if (!filename.endsWith(".json")) continue
            val id = filename.removeSuffix(".json")
            if (!levelsCache.containsKey(id)) {
                getLevel(id) // This will load and cache it
            }
        }
        
        return levelsCache.values.toList()
    }
    
    fun getLevelSequence(): LevelSequence {
        println("EditorStorage: Retrieving level sequence...${levelSequenceCache}")
        if (levelSequenceCache != null) {
            return levelSequenceCache!!
        }
        
        // Try to load from file
        val json = fileStorage.readFile(SEQUENCE_FILE)

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
    
    fun updateLevelSequence(sequence: LevelSequence) {
        levelSequenceCache = sequence
        val json = EditorJsonSerializer.serializeSequence(sequence)
        fileStorage.writeFile(SEQUENCE_FILE, json)
    }
    
    fun moveLevelUp(levelId: String) {
        val currentSequence = getLevelSequence().sequence.toMutableList()
        val index = currentSequence.indexOf(levelId)
        if (index > 0) {
            currentSequence.removeAt(index)
            currentSequence.add(index - 1, levelId)
            updateLevelSequence(LevelSequence(currentSequence))
        }
    }
    
    fun moveLevelDown(levelId: String) {
        val currentSequence = getLevelSequence().sequence.toMutableList()
        val index = currentSequence.indexOf(levelId)
        if (index >= 0 && index < currentSequence.size - 1) {
            currentSequence.removeAt(index)
            currentSequence.add(index + 1, levelId)
            updateLevelSequence(LevelSequence(currentSequence))
        }
    }
    
    /**
     * Add a level to the level sequence.
     * If the level is already in the sequence, does nothing.
     * @param levelId The ID of the level to add
     * @param atIndex Optional index where to insert the level. If null, adds to the end.
     */
    fun addLevelToSequence(levelId: String, atIndex: Int? = null) {
        val currentSequence = getLevelSequence().sequence.toMutableList()
        if (!currentSequence.contains(levelId)) {
            if (atIndex != null && atIndex >= 0 && atIndex <= currentSequence.size) {
                currentSequence.add(atIndex, levelId)
            } else {
                currentSequence.add(levelId)
            }
            updateLevelSequence(LevelSequence(currentSequence))
        }
    }
    
    /**
     * Remove a level from the level sequence.
     * The level file is not deleted and can be added back to the sequence later.
     * @param levelId The ID of the level to remove from the sequence
     */
    fun removeLevelFromSequence(levelId: String) {
        val currentSequence = getLevelSequence().sequence.toMutableList()
        currentSequence.remove(levelId)
        updateLevelSequence(LevelSequence(currentSequence))
    }
    
    /**
     * Move a level to a specific position in the sequence.
     * If the level is already in the sequence, it is moved to the new position.
     * If the level is not in the sequence, it is added at the specified position.
     * @param levelId The ID of the level to move
     * @param toIndex The target index (0-based) in the sequence
     */
    fun moveLevelToPosition(levelId: String, toIndex: Int) {
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
        
        // Try to load from file
        val json = fileStorage.readFile(WORLDMAP_FILE)
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
     * Save the world map data.
     */
    fun saveWorldMapData(data: WorldMapData) {
        worldMapDataCache = data
        val json = EditorJsonSerializer.serializeWorldMapData(data)
        fileStorage.writeFile(WORLDMAP_FILE, json)
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
    
    fun deleteMap(mapId: String) {
        // Remove from cache
        mapsCache.remove(mapId)
        // Delete file
        fileStorage.deleteFile("$MAPS_DIR/$mapId.json")
    }
    
    fun deleteLevel(levelId: String) {
        // Remove from cache
        levelsCache.remove(levelId)
        // Delete file
        fileStorage.deleteFile("$LEVELS_DIR/$levelId.json")
        // Remove from sequence
        val currentSequence = getLevelSequence().sequence.toMutableList()
        currentSequence.remove(levelId)
        updateLevelSequence(LevelSequence(currentSequence))
    }
    
    /**
     * Convert an EditorLevel to a Level for gameplay
     */
    fun convertToGameLevel(editorLevel: EditorLevel, numericId: Int): Level? {
        println("Converting EditorLevel ${editorLevel.id} to game Level with numeric ID $numericId")
        // Force reload the map from disk to get latest changes
        val map = reloadMap(editorLevel.mapId) ?: getMap(editorLevel.mapId) ?: return null
        println("Using map: ${map.id} (${map.width}x${map.height})")
        
        // Convert enemy spawns directly to PlannedEnemySpawn
        println("-------------------------------")
        println("enemySpawns: ${editorLevel.enemySpawns}")
        println("-------------------------------")

        val directSpawnPlan = editorLevel.enemySpawns.map { spawn ->
            PlannedEnemySpawn(
                attackerType = spawn.attackerType,
                spawnTurn = spawn.spawnTurn,
                level = spawn.level,
                spawnPoint = spawn.spawnPoint
            )
        }.sortedBy { it.spawnTurn }
        
        println("Created direct spawn plan with ${directSpawnPlan.size} spawns")
        
        // Still create AttackerWaves for backward compatibility
        val spawnsByTurn = editorLevel.enemySpawns.groupBy { it.spawnTurn }
        println("Enemy spawns grouped by turn: ${spawnsByTurn.keys.sorted()}")
        val waves = spawnsByTurn.entries.sortedBy { it.key }.map { (_, spawns) ->
            AttackerWave(
                attackers = spawns.map { it.attackerType },
                spawnDelay = 1  // Fixed delay for now
            )
        }
        println("Converted to ${waves.size} attacker waves for compatibility.")

        // Get all target positions from the map
        val targets = map.getTargets()
        if (targets.isEmpty()) return null
        println("=== LEVEL CONVERSION DEBUG ===")
        println("Target positions from map: $targets")
        
        // Convert editor waypoints to game waypoints
        val gameWaypoints = editorLevel.waypoints.map { editorWaypoint ->
            Waypoint(
                position = editorWaypoint.position,
                nextTarget = editorWaypoint.nextTargetPosition
            )
        }
        println("Converted ${gameWaypoints.size} waypoints:")
        gameWaypoints.forEach { wp ->
            println("  Waypoint: ${wp.position} -> ${wp.nextTarget}")
        }
        
        // Include waypoint positions in pathCells so enemies can walk on them
        val pathCellsWithWaypoints = map.getPathCells().toMutableSet()
        gameWaypoints.forEach { waypoint ->
            pathCellsWithWaypoints.add(waypoint.position)
        }
        println("Path cells: ${map.getPathCells().size}, with waypoints: ${pathCellsWithWaypoints.size}")
        println("Spawn points: ${map.getSpawnPoints()}")
        println("=== END LEVEL CONVERSION DEBUG ===")
        
        val level = Level(
            id = numericId,
            name = editorLevel.title,
            subtitle = editorLevel.subtitle,
            gridWidth = map.width,
            gridHeight = map.height,
            startPositions = map.getSpawnPoints(),
            targetPositions = targets,
            pathCells = pathCellsWithWaypoints,
            buildIslands = map.getBuildIslands(),
            buildAreas = map.getBuildAreas(),
            attackerWaves = waves,
            initialCoins = editorLevel.startCoins,
            healthPoints = editorLevel.startHealthPoints,
            directSpawnPlan = directSpawnPlan,
            availableTowers = editorLevel.availableTowers,
            waypoints = gameWaypoints,
            editorLevelId = editorLevel.id,  // Store editor level ID for minimap lookup
            mapId = editorLevel.mapId,  // Store map ID for save/load verification
            riverTiles = map.getRiverTilesMap()  // Add river tiles with flow direction and speed
        )
        
        println("=== CREATED LEVEL ===")
        println("Level: ${level.name} (ID: ${level.id})")
        println("Target positions: ${level.targetPositions}")
        println("Waypoints count: ${level.waypoints.size}")
        println("Start positions: ${level.startPositions}")
        println("=== END CREATED LEVEL ===")
        
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
            println("Could not load repository files: ${e.message}")
            false
        }
    }
    
    /**
     * Check if the gamedata directory contains any existing user files
     * @return true if any user data files exist, false if directory is empty
     */
    private fun hasExistingGamedataFiles(): Boolean {
        // Check if the sequence file exists (most reliable indicator)
        if (fileStorage.fileExists(SEQUENCE_FILE)) {
            return true
        }
        
        // Check if any maps exist
        if (fileStorage.listFiles(MAPS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check if any levels exist
        if (fileStorage.listFiles(LEVELS_DIR).isNotEmpty()) {
            return true
        }
        
        // Check if world map file exists (user may have edited it)
        if (fileStorage.fileExists(WORLDMAP_FILE)) {
            return true
        }
        
        // No user data found
        return false
    }
}
