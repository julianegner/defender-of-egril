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
    
    // Initialize with converted existing levels
    init {
        initializeDefaultMapsAndLevels()
        
        // Safety check: ensure we have a valid sequence after initialization
        val sequence = getLevelSequence()
        if (sequence.sequence.isEmpty()) {
            println("ERROR: After initialization, sequence is still empty! Forcing reinitialization...")
            // Clear everything and force reinit
            levelSequenceCache = null
            mapsCache.clear()
            levelsCache.clear()
            
            // Delete the sequence file to force reinit
            fileStorage.writeFile(SEQUENCE_FILE, "")
            
            // Try again
            initializeDefaultMapsAndLevels()
        }
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
     * Convert existing levels to editor format
     * Only initializes if the gamedata directory is completely empty
     */
    private fun initializeDefaultMapsAndLevels() {
        // First check if gamedata directory has any existing user data
        // If it has any content, assume user has data and skip initialization
        val hasUserData = hasExistingGamedataFiles()
        
        if (hasUserData) {
            println("Gamedata directory is not empty - preserving existing user data")
            // Try to load the sequence to populate cache
            val sequenceJson = fileStorage.readFile(SEQUENCE_FILE)
            if (sequenceJson != null) {
                val sequence = EditorJsonSerializer.deserializeSequence(sequenceJson)
                if (sequence != null && sequence.sequence.isNotEmpty()) {
                    levelSequenceCache = sequence
                }
            }
            return
        }
        
        println("Gamedata directory is empty - initializing from repository or generating defaults")
        
        // Create directories
        fileStorage.createDirectory(MAPS_DIR)
        fileStorage.createDirectory(LEVELS_DIR)
        
        // Try to load from repository first
        println("Checking for repository files...")
        if (tryLoadRepositoryFiles()) {
            println("Successfully loaded files from repository")
            return
        }
        println("No repository files found, generating default maps and levels...")
        
        // Create default maps based on the existing level generation
        for (size in listOf(
            Triple("map_30x8", 30, 8),
            Triple("map_35x9", 35, 9),
            Triple("map_40x10", 40, 10),
            Triple("map_45x11", 45, 11),
            Triple("map_50x12", 50, 12)
        )) {
            val (mapId, width, height) = size
            val pathAndIslands = Level.generateCurvedPathWithIslands(width, height)
            
            val tiles = mutableMapOf<String, TileType>()
            
            // Set spawn points (hardcoded from original)
            val spawnPoints = listOf(
                0 to 1,
                0 to 4,
                0 to 7
            ).filter { it.second < height }
            
            spawnPoints.forEach { (x, y) ->
                tiles["$x,$y"] = TileType.SPAWN_POINT
            }
            
            // Set target
            tiles["${width - 1},${height / 2}"] = TileType.TARGET
            
            // Set path cells
            pathAndIslands.pathCells.forEach { pos ->
                if (!tiles.containsKey("${pos.x},${pos.y}")) {
                    tiles["${pos.x},${pos.y}"] = TileType.PATH
                }
            }
            
            // Set island cells
            pathAndIslands.buildIslands.forEach { pos ->
                tiles["${pos.x},${pos.y}"] = TileType.ISLAND
            }
            
            // Calculate and set BUILD_AREA tiles (adjacent to PATH)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    val pos = de.egril.defender.model.Position(x, y)
                    val key = "$x,$y"
                    
                    // Skip if already has a tile type assigned
                    if (tiles.containsKey(key)) continue
                    
                    // Check if adjacent to a path
                    val neighbors = pos.getHexNeighbors()
                    val isAdjacentToPath = neighbors.any { neighbor ->
                        neighbor.x >= 0 && neighbor.x < width &&
                        neighbor.y >= 0 && neighbor.y < height &&
                        tiles["${neighbor.x},${neighbor.y}"] == TileType.PATH
                    }
                    
                    if (isAdjacentToPath) {
                        tiles[key] = TileType.BUILD_AREA
                    }
                }
            }
            
            val map = EditorMap(
                id = mapId,
                name = "Generated Map ${width}x${height}",
                width = width,
                height = height,
                tiles = tiles,
                readyToUse = false  // Will be validated and updated on save
            )
            
            // Validate and save with correct readyToUse flag
            val validatedMap = map.copy(readyToUse = map.validateReadyToUse())
            saveMap(validatedMap)
        }
        
        // Create tutorial map - small and simple (15x8)
        val tutorialTiles = mutableMapOf<String, TileType>()
        
        // Tutorial spawn points - just 1 spawn point for simplicity
        tutorialTiles["0,4"] = TileType.SPAWN_POINT
        
        // Tutorial target
        tutorialTiles["14,4"] = TileType.TARGET
        
        // Simple straight path with slight curve
        for (x in 0..14) {
            val y = when {
                x < 3 -> 4
                x < 6 -> 3 + (x - 3) / 2  // Move from 4 to 5
                x < 9 -> 5
                x < 12 -> 5 - (x - 9) / 2  // Move from 5 to 4
                else -> 4
            }
            tutorialTiles["$x,$y"] = TileType.PATH
            // Add width to path
            if (y > 0) tutorialTiles["$x,${y-1}"] = TileType.PATH
            if (y < 7) tutorialTiles["$x,${y+1}"] = TileType.PATH
        }
        
        // Add build areas adjacent to path
        for (x in 0..14) {
            for (y in 0..7) {
                val key = "$x,$y"
                if (tutorialTiles.containsKey(key)) continue
                
                val pos = de.egril.defender.model.Position(x, y)
                val neighbors = pos.getHexNeighbors()
                val isAdjacentToPath = neighbors.any { neighbor ->
                    neighbor.x >= 0 && neighbor.x < 15 &&
                    neighbor.y >= 0 && neighbor.y < 8 &&
                    tutorialTiles["${neighbor.x},${neighbor.y}"] == TileType.PATH
                }
                
                if (isAdjacentToPath) {
                    tutorialTiles[key] = TileType.BUILD_AREA
                }
            }
        }
        
        // Add a couple of islands for strategic placement
        for ((baseX, baseY) in listOf(5 to 1, 9 to 6)) {
            for (dx in 0..1) {
                for (dy in 0..1) {
                    tutorialTiles["${baseX + dx},${baseY + dy}"] = TileType.ISLAND
                }
            }
        }
        
        val tutorialMap = EditorMap(
            id = "map_tutorial",
            name = "Tutorial Map",
            width = 15,
            height = 8,
            tiles = tutorialTiles,
            readyToUse = false
        )
        val validatedTutorialMap = tutorialMap.copy(readyToUse = tutorialMap.validateReadyToUse())
        saveMap(validatedTutorialMap)
        
        // Tutorial Level: Welcome to Defender of Egril
        // 5 goblins + 1 ork, only 3 tower types available
        val tutorialSpawns = mutableListOf<EditorEnemySpawn>()
        // 5 goblins - spawn on turns 1, 2, 3, 4, 5
        for (i in 1..5) {
            tutorialSpawns.add(EditorEnemySpawn(AttackerType.GOBLIN, 1, i))
        }
        // 1 ork on turn 8 (give player time to see goblins)
        tutorialSpawns.add(EditorEnemySpawn(AttackerType.ORK, 1, 8))
        
        saveLevel(EditorLevel(
            id = "welcome_to_defender_of_egril",
            mapId = "map_tutorial",
            title = "Welcome to Defender of Egril",
            subtitle = "Tutorial",
            startCoins = 60,  // Enough for 6 spike towers or 4 spear towers or 3 bow towers
            startHealthPoints = 10,
            enemySpawns = tutorialSpawns,
            availableTowers = setOf(
                DefenderType.SPIKE_TOWER,
                DefenderType.SPEAR_TOWER,
                DefenderType.BOW_TOWER
            )
        ))
        
        // Create levels based on existing LevelData
        // Level 1: The First Wave
        saveLevel(EditorLevel(
            id = "the_first_wave",
            mapId = "map_30x8",
            title = "The First Wave",
            subtitle = "",
            startCoins = 100,
            startHealthPoints = 10,
            enemySpawns = List(30) { index ->
                EditorEnemySpawn(AttackerType.GOBLIN, 1, index / 6 + 1)
            },
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 2: Mixed Forces
        val level2Spawns = mutableListOf<EditorEnemySpawn>()
        var turn = 1
        List(30) { AttackerType.GOBLIN }.forEach { type ->
            level2Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level2Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        List(20) { AttackerType.SKELETON }.forEach { type ->
            level2Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level2Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        List(15) { AttackerType.ORK }.forEach { type ->
            level2Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level2Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        
        saveLevel(EditorLevel(
            id = "mixed_forces",
            mapId = "map_35x9",
            title = "Mixed Forces",
            subtitle = "",
            startCoins = 120,
            startHealthPoints = 10,
            enemySpawns = level2Spawns,
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 3: The Ork Invasion
        val level3Spawns = mutableListOf<EditorEnemySpawn>()
        turn = 1
        List(40) { AttackerType.GOBLIN }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        List(30) { AttackerType.ORK }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        List(20) { AttackerType.SKELETON }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        List(20) { AttackerType.ORK }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        List(5) { AttackerType.OGRE }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        
        saveLevel(EditorLevel(
            id = "the_ork_invasion",
            mapId = "map_40x10",
            title = "The Ork Invasion",
            subtitle = "",
            startCoins = 150,
            startHealthPoints = 8,
            enemySpawns = level3Spawns,
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 4: Dark Magic Rises
        val level4Spawns = mutableListOf<EditorEnemySpawn>()
        turn = 1
        (List(30) { AttackerType.GOBLIN } + List(5) { AttackerType.EVIL_WIZARD }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        (List(20) { AttackerType.ORK } + List(5) { AttackerType.GREEN_WITCH }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        (List(5) { AttackerType.OGRE } + List(20) { AttackerType.SKELETON }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        (List(10) { AttackerType.EVIL_WIZARD } + List(10) { AttackerType.RED_WITCH }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        
        saveLevel(EditorLevel(
            id = "dark_magic_rises",
            mapId = "map_45x11",
            title = "Dark Magic Rises",
            subtitle = "",
            startCoins = 180,
            startHealthPoints = 8,
            enemySpawns = level4Spawns,
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 5: The Final Stand
        val level5Spawns = mutableListOf<EditorEnemySpawn>()
        turn = 1
        (List(50) { AttackerType.SKELETON } + List(20) { AttackerType.EVIL_WIZARD }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        (List(30) { AttackerType.ORK } + List(5) { AttackerType.GREEN_WITCH }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        (List(20) { AttackerType.OGRE } + List(30) { AttackerType.GOBLIN }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        turn++  // Small gap between waves
        (List(20) { AttackerType.OGRE } + List(10) { AttackerType.EVIL_WIZARD } + 
         List(10) { AttackerType.RED_WITCH } + List(1) { AttackerType.EWHAD }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        
        saveLevel(EditorLevel(
            id = "the_final_stand",
            mapId = "map_50x12",
            title = "The Final Stand",
            subtitle = "",
            startCoins = 200,
            startHealthPoints = 6,
            enemySpawns = level5Spawns,
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 6: Ewhad's Challenge
        saveLevel(EditorLevel(
            id = "ewhads_challenge",
            mapId = "map_50x12",
            title = "Ewhad's Challenge",
            subtitle = "",
            startCoins = 300,
            startHealthPoints = 10,
            enemySpawns = listOf(
                EditorEnemySpawn(AttackerType.EWHAD, 1, 1)
            ),
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Create spiral map - square map with spiral path
        val spiralMap = MapGenerator.createSpiralMap()
        val validatedSpiralMap = spiralMap.copy(readyToUse = spiralMap.validateReadyToUse())
        saveMap(validatedSpiralMap)
        
        // Create plains map - simple map with 4 islands
        val plainsMap = MapGenerator.createPlainsMap()
        val validatedPlainsMap = plainsMap.copy(readyToUse = plainsMap.validateReadyToUse())
        saveMap(validatedPlainsMap)
        
        // Create dance map - circular dancing path with broken ring
        val danceMap = MapGenerator.createDanceMap()
        val validatedDanceMap = danceMap.copy(readyToUse = danceMap.validateReadyToUse())
        saveMap(validatedDanceMap)

        // Level 7: The Spiral Challenge
        saveLevel(EditorLevel(
            id = "the_spiral_challenge",
            mapId = "map_spiral",
            title = "The Spiral Challenge",
            subtitle = "Navigate the Spiral",
            startCoins = 250,
            startHealthPoints = 10,
            enemySpawns = List(50) { index ->
                // Mix of enemy types
                val enemyType = when (index % 5) {
                    0 -> AttackerType.GOBLIN
                    1 -> AttackerType.SKELETON
                    2 -> AttackerType.ORK
                    3 -> AttackerType.EVIL_WIZARD
                    else -> if (index % 2 == 0) AttackerType.GREEN_WITCH else AttackerType.RED_WITCH
                }
                EditorEnemySpawn(enemyType, 1, index / 6 + 1)
            },
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 8: The Plains
        saveLevel(EditorLevel(
            id = "the_plains",
            mapId = "map_plains",
            title = "The Plains",
            subtitle = "Open Field Battle",
            startCoins = 200,
            startHealthPoints = 10,
            enemySpawns = List(40) { index ->
                // Mix of basic enemy types
                val enemyType = when (index % 4) {
                    0 -> AttackerType.GOBLIN
                    1 -> AttackerType.SKELETON
                    2 -> AttackerType.ORK
                    else -> AttackerType.OGRE
                }
                EditorEnemySpawn(enemyType, 1, index / 6 + 1)
            },
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet()
        ))
        
        // Level 9: The Dance
        // Create waypoints for circular dancing pattern
        // Three circles: outer (radius 18), middle (radius 10), inner (radius 6) -> center
        val danceCenter = Position(20, 20)
        
        // Outermost ring waypoints at distance ~18
        val outerWaypoints = listOf(
            Position(38, 20),  // East at distance ~18
            Position(20, 2),   // North at distance ~18
            Position(2, 20),   // West at distance ~18
            Position(20, 38)   // South at distance ~18
        )
        // Middle ring waypoints at distance ~10
        val middleWaypoints = listOf(
            Position(30, 20),  // East at distance ~10
            Position(20, 10),  // North at distance ~10
            Position(10, 20),  // West at distance ~10
            Position(20, 30)   // South at distance ~10
        )
        // Inner ring waypoints at distance ~6
        val innerWaypoints = listOf(
            Position(26, 20),  // East at distance ~6
            Position(20, 14),  // North at distance ~6
            Position(14, 20),  // West at distance ~6
            Position(20, 26)   // South at distance ~6
        )
        
        // Create waypoint chain: outer ring (clockwise) -> middle ring (clockwise) -> inner ring (clockwise) -> target
        val danceWaypoints = listOf(
            // Outer ring - clockwise circle
            EditorWaypoint(outerWaypoints[0], outerWaypoints[1]),  // East -> North
            EditorWaypoint(outerWaypoints[1], outerWaypoints[2]),  // North -> West
            EditorWaypoint(outerWaypoints[2], outerWaypoints[3]),  // West -> South
            EditorWaypoint(outerWaypoints[3], middleWaypoints[0]), // South -> Middle East (transition)
            // Middle ring - clockwise circle
            EditorWaypoint(middleWaypoints[0], middleWaypoints[1]),  // Middle East -> Middle North
            EditorWaypoint(middleWaypoints[1], middleWaypoints[2]),  // Middle North -> Middle West
            EditorWaypoint(middleWaypoints[2], middleWaypoints[3]),  // Middle West -> Middle South
            EditorWaypoint(middleWaypoints[3], innerWaypoints[0]),   // Middle South -> Inner East (transition)
            // Inner ring - clockwise circle
            EditorWaypoint(innerWaypoints[0], innerWaypoints[1]),  // Inner East -> Inner North
            EditorWaypoint(innerWaypoints[1], innerWaypoints[2]),  // Inner North -> Inner West
            EditorWaypoint(innerWaypoints[2], innerWaypoints[3]),  // Inner West -> Inner South
            EditorWaypoint(innerWaypoints[3], danceCenter)         // Inner South -> Target
        )
        
        saveLevel(EditorLevel(
            id = "the_dance",
            mapId = "map_dance",
            title = "The Dance",
            subtitle = "Follow the Rhythm",
            startCoins = 220,
            startHealthPoints = 10,
            enemySpawns = List(45) { index ->
                // Mix of enemy types with emphasis on speed
                val enemyType = when (index % 6) {
                    0 -> AttackerType.GOBLIN
                    1 -> AttackerType.SKELETON
                    2 -> AttackerType.GOBLIN
                    3 -> AttackerType.SKELETON
                    4 -> AttackerType.ORK
                    else -> AttackerType.EVIL_WIZARD
                }
                EditorEnemySpawn(enemyType, 1, index / 6 + 1)
            },
            availableTowers = DefenderType.entries.filter { 
                it != DefenderType.DRAGONS_LAIR 
            }.toSet(),
            waypoints = danceWaypoints
        ))
        
        // Set initial level sequence (tutorial first, then spiral, plains, and dance before final stand!)
        updateLevelSequence(LevelSequence(listOf(
            "welcome_to_defender_of_egril", "the_first_wave", "mixed_forces", "the_ork_invasion", "dark_magic_rises", "the_spiral_challenge", "the_plains", "the_dance", "the_final_stand", "ewhads_challenge"
        )))
        
        // Save version file to indicate successful initialization
        fileStorage.writeFile(VERSION_FILE, CURRENT_VERSION)
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
