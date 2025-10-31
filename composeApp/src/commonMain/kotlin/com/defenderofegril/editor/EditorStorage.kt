package com.defenderofegril.editor

import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.model.AttackerWave
import com.defenderofegril.model.Level
import com.defenderofegril.model.PlannedEnemySpawn
import com.defenderofegril.model.getHexNeighbors

/**
 * File-based storage for maps and levels
 * Stores data in ~/.defender-of-egril/ directory on desktop
 */
object EditorStorage {
    private val fileStorage = getFileStorage()
    private val mapsCache = mutableMapOf<String, EditorMap>()
    private val levelsCache = mutableMapOf<String, EditorLevel>()
    private var levelSequenceCache: LevelSequence? = null
    
    private val MAPS_DIR = "editor/maps"
    private val LEVELS_DIR = "editor/levels"
    private val SEQUENCE_FILE = "editor/sequence.json"
    
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
    
    fun saveLevel(level: EditorLevel) {
        levelsCache[level.id] = level
        val json = EditorJsonSerializer.serializeLevel(level)
        fileStorage.writeFile("$LEVELS_DIR/${level.id}.json", json)
        
        // Update sequence if this is a new level
        val sequence = getLevelSequence()
        if (!sequence.sequence.contains(level.id)) {
            val newSequence = LevelSequence(sequence.sequence + level.id)
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
                level = spawn.level
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

        val target = map.getTarget() ?: return null
        println("Target position: $target")
        
        return Level(
            id = numericId,
            name = editorLevel.title,
            gridWidth = map.width,
            gridHeight = map.height,
            startPositions = map.getSpawnPoints(),
            targetPosition = target,
            pathCells = map.getPathCells(),
            buildIslands = map.getBuildIslands(),
            buildAreas = map.getBuildAreas(),
            attackerWaves = waves,
            initialCoins = editorLevel.startCoins,
            healthPoints = editorLevel.startHealthPoints,
            directSpawnPlan = directSpawnPlan
        )
    }
    
    /**
     * Convert existing levels to editor format
     * Only initializes if files don't exist or are invalid
     */
    private fun initializeDefaultMapsAndLevels() {
        // Check if already initialized with valid data
        val sequenceJson = fileStorage.readFile(SEQUENCE_FILE)
        
        if (sequenceJson != null) {
            val sequence = EditorJsonSerializer.deserializeSequence(sequenceJson)
            
            if (sequence != null && sequence.sequence.isNotEmpty()) {
                // Valid sequence exists, check if levels exist
                val firstLevelId = sequence.sequence.firstOrNull()
                
                if (firstLevelId != null && firstLevelId.isNotBlank()) {
                    val fileExists = fileStorage.fileExists("$LEVELS_DIR/$firstLevelId.json")
                    
                    if (fileExists) {
                        // Also verify we can actually load the first level
                        val firstLevel = getLevel(firstLevelId)
                        
                        if (firstLevel != null) {
                            return  // Already initialized with valid data
                        }
                    }
                }
            }
        }
        
        // Create directories
        fileStorage.createDirectory(MAPS_DIR)
        fileStorage.createDirectory(LEVELS_DIR)
        
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
                    val pos = com.defenderofegril.model.Position(x, y)
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
        
        // Create levels based on existing LevelData
        // Level 1: The First Wave
        saveLevel(EditorLevel(
            id = "level_1",
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
        turn += 2
        List(15) { AttackerType.ORK }.forEach { type ->
            level2Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level2Spawns.filter { it.spawnTurn == turn }.size >= 6) turn++
        }
        
        saveLevel(EditorLevel(
            id = "level_2",
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
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        List(30) { AttackerType.ORK }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        List(20) { AttackerType.SKELETON }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        List(20) { AttackerType.ORK }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        List(5) { AttackerType.OGRE }.forEach { type ->
            level3Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level3Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        
        saveLevel(EditorLevel(
            id = "level_3",
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
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        (List(20) { AttackerType.ORK } + List(5) { AttackerType.WITCH }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        (List(5) { AttackerType.OGRE } + List(20) { AttackerType.SKELETON }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 3
        }
        turn += 2
        (List(10) { AttackerType.EVIL_WIZARD } + List(10) { AttackerType.WITCH }).forEach { type ->
            level4Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level4Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        
        saveLevel(EditorLevel(
            id = "level_4",
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
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        (List(30) { AttackerType.ORK } + List(5) { AttackerType.WITCH }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        (List(20) { AttackerType.OGRE } + List(30) { AttackerType.GOBLIN }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        turn += 2
        (List(20) { AttackerType.OGRE } + List(10) { AttackerType.EVIL_WIZARD } + 
         List(10) { AttackerType.WITCH } + List(1) { AttackerType.EWHAD }).forEach { type ->
            level5Spawns.add(EditorEnemySpawn(type, 1, turn))
            if (level5Spawns.filter { it.spawnTurn == turn }.size >= 6) turn += 2
        }
        
        saveLevel(EditorLevel(
            id = "level_5",
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
            id = "level_6",
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
        
        // Set initial level sequence
        updateLevelSequence(LevelSequence(listOf(
            "level_1", "level_2", "level_3", "level_4", "level_5", "level_6"
        )))
    }
}
