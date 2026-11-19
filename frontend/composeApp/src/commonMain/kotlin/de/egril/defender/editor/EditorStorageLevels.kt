package de.egril.defender.editor

import de.egril.defender.config.LogConfig
import de.egril.defender.model.AttackerWave
import de.egril.defender.model.Level
import de.egril.defender.model.PlannedEnemySpawn
import de.egril.defender.model.Position
import de.egril.defender.model.Waypoint

/**
 * Helper function to ensure all enemy spawns have spawn points assigned.
 * For enemies without spawn points, assigns them in round-robin fashion.
 */
private fun EditorStorage.ensureSpawnPoints(level: EditorLevel): EditorLevel {
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
    val updatedSpawns =
        level.enemySpawns.mapIndexed { index, spawn ->
            if (spawn.spawnPoint == null) {
                // Assign in round-robin fashion
                spawn.copy(spawnPoint = spawnPoints[index % spawnPoints.size])
            } else {
                spawn
            }
        }

    return level.copy(enemySpawns = updatedSpawns)
}

fun EditorStorage.saveLevel(level: EditorLevel) {
    // Ensure all enemy spawns have spawn points
    val levelWithSpawnPoints = ensureSpawnPoints(level)

    val initData = levelWithSpawnPoints.getEffectiveInitialData()
    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println(
            "EditorStorage.saveLevel: Saving level ${levelWithSpawnPoints.id} with ${initData.defenders.size} defenders, ${initData.attackers.size} attackers, ${initData.traps.size} traps, ${initData.barricades.size} barricades",
        )
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

fun EditorStorage.reloadLevel(id: String): EditorLevel? {
    // Force reload from file, bypassing cache
    levelsCache.remove(id)
    return getLevel(id)
}

fun EditorStorage.getLevel(id: String): EditorLevel? {
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
            val levelWithFlag = applyOfficialFlags(level, isOfficial)
            levelsCache[id] = levelWithFlag
            return levelWithFlag
        }
    }

    return null
}

private fun EditorStorage.applyOfficialFlags(
    level: EditorLevel,
    isOfficial: Boolean,
): EditorLevel = level.copy(isOfficial = level.isOfficial || isOfficial)

fun EditorStorage.getAllLevels(): List<EditorLevel> {
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

    val officialSequence = getLevelSequence().sequence
    val userSequence = getUserLevelSequence().sequence

    return levelsCache.values
        .toList()
        .sortedWith(
            compareBy<EditorLevel> { if (it.isOfficial) 0 else 1 }
                .thenBy { level ->
                    val index =
                        if (level.isOfficial) {
                            officialSequence.indexOf(level.id)
                        } else {
                            userSequence.indexOf(level.id)
                        }
                    if (index >= 0) index else Int.MAX_VALUE
                }.thenBy { it.title.lowercase() },
        )
}

// ---------------------------------------------------------------------------
// Community content (downloaded from backend)
// ---------------------------------------------------------------------------

/**
 * Store a community level downloaded from the backend.
 * @param level The level to store (will have isCommunity = true)
 */
fun EditorStorage.saveCommunityLevel(level: EditorLevel) {
    fileStorage.createDirectory(COMMUNITY_LEVELS_DIR)
    val communityLevel = level.copy(isCommunity = true)
    val json = EditorJsonSerializer.serializeLevel(communityLevel)
    fileStorage.writeFile("$COMMUNITY_LEVELS_DIR/${level.id}.json", json)
    communityLevelsCache[level.id] = communityLevel
}

/**
 * Store a community map downloaded from the backend.
 * @param map The map to store (will have isCommunity = true)
 * @param authorUsername The username of the map author
 * @param requestedId When provided (and different from [map.id]), the map is also stored
 *   under this ID so that [getMap] calls using the level's `mapId` reference can find it
 *   even if the map's internal JSON id differs from the fileId used to download it.
 */
fun EditorStorage.saveCommunityMap(
    map: EditorMap,
    authorUsername: String,
    requestedId: String? = null,
    storage: FileStorage = fileStorage,
) {
    storage.createDirectory(COMMUNITY_MAPS_DIR)
    val communityMap = map.copy(isCommunity = true, communityAuthorUsername = authorUsername)
    val json = EditorJsonSerializer.serializeMap(communityMap)
    storage.writeFile("$COMMUNITY_MAPS_DIR/${map.id}.json", json)
    communityMapsCache[map.id] = communityMap
    // If the caller knows the map was requested under a different ID (e.g. the level's mapId
    // differs from the map's internal id field), persist it under that alias too so that
    // getMap(level.mapId) can always find the map.
    if (requestedId != null && requestedId != map.id) {
        storage.writeFile("$COMMUNITY_MAPS_DIR/$requestedId.json", json)
        communityMapsCache[requestedId] = communityMap
    }
    // Generate PNG image so it is available during gameplay
    generateAndSaveCommunityMapImage(communityMap, storage)
    generateAndSaveTileZoneImages(communityMap, storage)
    if (requestedId != null && requestedId != map.id) {
        val aliasMap = communityMap.copy(id = requestedId)
        generateAndSaveCommunityMapImage(aliasMap, storage)
        generateAndSaveTileZoneImages(aliasMap, storage)
    }
}

/**
 * Get a community level by ID (from community cache/directory only).
 * Returns null if not found.
 */
fun EditorStorage.getCommunityLevel(id: String): EditorLevel? {
    if (communityLevelsCache.containsKey(id)) {
        return communityLevelsCache[id]
    }
    val json = fileStorage.readFile("$COMMUNITY_LEVELS_DIR/$id.json") ?: return null
    val level = EditorJsonSerializer.deserializeLevel(json) ?: return null
    val communityLevel = level.copy(isCommunity = true)
    communityLevelsCache[id] = communityLevel
    return communityLevel
}

/**
 * Get a community map by ID (from community cache/directory only).
 * Returns null if not found.
 */
fun EditorStorage.getCommunityMap(id: String): EditorMap? {
    if (communityMapsCache.containsKey(id)) {
        return communityMapsCache[id]
    }
    val json = fileStorage.readFile("$COMMUNITY_MAPS_DIR/$id.json") ?: return null
    val map = EditorJsonSerializer.deserializeMap(json) ?: return null
    val communityMap = map.copy(isCommunity = true)
    communityMapsCache[id] = communityMap
    return communityMap
}

/**
 * Get all locally stored community levels.
 *
 * Returns a snapshot list built from disk reads rather than the live cache reference.
 * This prevents a race condition on JVM where a concurrent [clearCommunityCache] call
 * (from another Dispatchers.Default coroutine) could empty the cache between the loading
 * loop and the return statement, resulting in an empty list.
 */
fun EditorStorage.getAllCommunityLevels(): List<EditorLevel> {
    fileStorage.createDirectory(COMMUNITY_LEVELS_DIR)
    val files = fileStorage.listFiles(COMMUNITY_LEVELS_DIR)
    val result = mutableListOf<EditorLevel>()
    for (filename in files) {
        if (!filename.endsWith(".json")) continue
        val id = filename.removeSuffix(".json")
        val level = communityLevelsCache[id] ?: getCommunityLevel(id)
        if (level != null) result.add(level)
    }
    return result
}

/**
 * Get all locally stored community maps.
 *
 * Returns a snapshot list built from disk reads rather than the live cache reference.
 * This prevents a race condition on JVM where a concurrent [clearCommunityCache] call
 * (from another Dispatchers.Default coroutine) could empty the cache between the loading
 * loop and the return statement, resulting in an empty list.
 */
fun EditorStorage.getAllCommunityMaps(): List<EditorMap> {
    fileStorage.createDirectory(COMMUNITY_MAPS_DIR)
    val files = fileStorage.listFiles(COMMUNITY_MAPS_DIR)
    val result = mutableListOf<EditorMap>()
    for (filename in files) {
        if (!filename.endsWith(".json")) continue
        val id = filename.removeSuffix(".json")
        val map = communityMapsCache[id] ?: getCommunityMap(id)
        if (map != null) result.add(map)
    }
    return result
}

/**
 * Get the locally stored community version of a level (for change detection).
 * Returns null if the level has never been uploaded to community.
 */
fun EditorStorage.getStoredCommunityLevelJson(levelId: String): String? = fileStorage.readFile("$COMMUNITY_LEVELS_DIR/$levelId.json")

/**
 * Get the locally stored community version of a map (for change detection).
 * Returns null if the map has never been uploaded to community.
 */
fun EditorStorage.getStoredCommunityMapJson(mapId: String): String? = fileStorage.readFile("$COMMUNITY_MAPS_DIR/$mapId.json")

/**
 * Clear the in-memory community caches (forces reload from disk on next access).
 */
fun EditorStorage.clearCommunityCache() {
    communityLevelsCache.clear()
    communityMapsCache.clear()
}

/**
 * Clear the in-memory official data caches (forces reload from disk on next access).
 * Call this after syncing official content from the repository so subsequent reads pick up
 * the newly written files.
 */
fun EditorStorage.clearOfficialDataCache() {
    levelSequenceCache = null
    userLevelSequenceCache = null
    worldMapDataCache = null
    mapsCache.clear()
    levelsCache.clear()
}

fun EditorStorage.deleteLevel(levelId: String): Boolean {
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
fun EditorStorage.convertToGameLevel(
    editorLevel: EditorLevel,
    numericId: Int,
    preloadedMap: EditorMap? = null,
): Level? {
    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("Converting EditorLevel ${editorLevel.id} to game Level with numeric ID $numericId")
    }
    // Use the caller-supplied map when given; otherwise force reload from disk to get latest changes
    val map = preloadedMap ?: reloadMap(editorLevel.mapId) ?: getMap(editorLevel.mapId) ?: return null
    if (LogConfig.ENABLE_LEVEL_LOADING_LOGGING) {
        println("Using map: ${map.id} (${map.width}x${map.height})")
        // Convert enemy spawns directly to PlannedEnemySpawn
        println("-------------------------------")
        println("enemySpawns: ${editorLevel.enemySpawns}")
        println("-------------------------------")
    }

    val directSpawnPlan =
        editorLevel.enemySpawns
            .map { spawn ->
                PlannedEnemySpawn(
                    attackerType = spawn.attackerType,
                    spawnTurn = spawn.spawnTurn,
                    level = spawn.level,
                    spawnPoint = spawn.spawnPoint,
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
    val waves =
        spawnsByTurn.entries.sortedBy { it.key }.map { (_, spawns) ->
            AttackerWave(
                attackers = spawns.map { it.attackerType },
                spawnDelay = 1, // Fixed delay for now
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
    val gameWaypoints =
        editorLevel.waypoints.map { editorWaypoint ->
            Waypoint(
                position = editorWaypoint.position,
                nextTarget = editorWaypoint.nextTargetPosition,
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

    // Convert editor target info map to model TargetInfo map
    val gameTargetInfoMap: Map<Position, de.egril.defender.model.TargetInfo> =
        map.targetInfoMap.entries
            .mapNotNull { (key, editorInfo) ->
                val parts = key.split(",")
                val pos = Position(parts[0].toInt(), parts[1].toInt())
                pos to
                    de.egril.defender.model.TargetInfo(
                        name = editorInfo.name,
                        type = editorInfo.type,
                    )
            }.toMap()

    // Convert editor spawn point info map to model SpawnPointType map
    val gameSpawnPointTypeMap: Map<Position, de.egril.defender.model.SpawnPointType> =
        map.spawnPointInfoMap.entries
            .mapNotNull { (key, spawnType) ->
                val parts = key.split(",")
                val pos = Position(parts[0].toInt(), parts[1].toInt())
                pos to spawnType
            }.toMap()

    val level =
        Level(
            id = numericId,
            name = editorLevel.title,
            subtitle = editorLevel.subtitle,
            titleKey = editorLevel.titleKey, // Store translation key for localization
            subtitleKey = editorLevel.subtitleKey, // Store translation key for localization
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
            spawnGroups = editorLevel.spawnGroups, // Spawn loops (issue #694), override the flat plan when present
            availableTowers = editorLevel.availableTowers,
            waypoints = gameWaypoints,
            editorLevelId = editorLevel.id, // Store editor level ID for minimap lookup
            mapId = editorLevel.mapId, // Store map ID for save/load verification
            isCommunity = editorLevel.isCommunity, // Propagate community flag so UI can filter correctly
            riverTiles = map.getRiverTilesMap(), // Add river tiles with flow direction and speed
            allowAutoAttack = editorLevel.allowAutoAttack, // Allow auto-attack option
            connectedToPreviousLevel = editorLevel.connectedToPreviousLevel, // Connected level flag
            autoTowerBase = editorLevel.autoTowerBase, // Towers may be built on PATH tiles with a tower base
            autoTowerBaseExtraCost = editorLevel.autoTowerBaseExtraCost, // Extra cost for PATH tower placement
            isSandbox = editorLevel.isSandbox, // Sandbox mode flag (free building, no win, no XP, no events)
            waaghEnabled = editorLevel.waaghEnabled, // Waaagh! horde mechanics flag
            targetInfoMap = gameTargetInfoMap, // Named / SINGLE_HIT target metadata
            singleHitTargetOrder = editorLevel.singleHitTargetOrder,
            spawnPointTypeMap = gameSpawnPointTypeMap, // LAND/WATER classification per spawn point
            supports = editorLevel.supports, // Player-usable supports (objects + spell tokens)
            events = editorLevel.events, // Scripted level events (conditions + actions + messages)
            tileZones = map.tileZones, // Alternative terrain states switched by scripted events
            initialData = editorLevel.getEffectiveInitialData(), // Pre-placed elements using new structure
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
