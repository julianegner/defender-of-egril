package de.egril.defender.editor

import de.egril.defender.config.LogConfig

fun EditorStorage.getLevelSequence(): LevelSequence {
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
fun EditorStorage.getUserLevelSequence(): LevelSequence {
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

fun EditorStorage.updateLevelSequence(sequence: LevelSequence) {
    // Save to OFFICIAL_SEQUENCE_FILE
    levelSequenceCache = sequence
    val json = EditorJsonSerializer.serializeSequence(sequence)
    fileStorage.writeFile(OFFICIAL_SEQUENCE_FILE, json)
}

/**
 * Update the user level sequence.
 */
fun EditorStorage.updateUserLevelSequence(sequence: LevelSequence) {
    userLevelSequenceCache = sequence
    val json = EditorJsonSerializer.serializeSequence(sequence)
    fileStorage.writeFile(USER_SEQUENCE_FILE, json)
}

fun EditorStorage.moveLevelUp(levelId: String) {
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

fun EditorStorage.moveLevelDown(levelId: String) {
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
fun EditorStorage.addLevelToSequence(
    levelId: String,
    atIndex: Int? = null,
) {
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
fun EditorStorage.removeLevelFromSequence(levelId: String) {
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
fun EditorStorage.moveLevelToPosition(
    levelId: String,
    toIndex: Int,
) {
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
fun EditorStorage.getWorldMapData(): WorldMapData {
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
fun EditorStorage.saveWorldMapData(data: WorldMapData) {
    worldMapDataCache = data
    val json = EditorJsonSerializer.serializeWorldMapData(data)
    fileStorage.writeFile(OFFICIAL_WORLDMAP_FILE, json)
}

/**
 * Add or update a location in the world map data.
 */
fun EditorStorage.saveWorldMapLocation(location: WorldMapLocationData) {
    val currentData = getWorldMapData()
    val existingIndex = currentData.locations.indexOfFirst { it.id == location.id }

    val updatedLocations =
        if (existingIndex >= 0) {
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
fun EditorStorage.deleteWorldMapLocation(locationId: String) {
    val currentData = getWorldMapData()
    val updatedLocations = currentData.locations.filter { it.id != locationId }
    val updatedPaths =
        currentData.paths.filter {
            it.fromLocationId != locationId && it.toLocationId != locationId
        }
    saveWorldMapData(currentData.copy(locations = updatedLocations, paths = updatedPaths))
}

/**
 * Add or update a path in the world map data.
 */
fun EditorStorage.saveWorldMapPath(path: WorldMapPathData) {
    val currentData = getWorldMapData()
    val existingIndex =
        currentData.paths.indexOfFirst {
            it.fromLocationId == path.fromLocationId && it.toLocationId == path.toLocationId
        }

    val updatedPaths =
        if (existingIndex >= 0) {
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
fun EditorStorage.deleteWorldMapPath(
    fromLocationId: String,
    toLocationId: String,
) {
    val currentData = getWorldMapData()
    val updatedPaths =
        currentData.paths.filter {
            !(it.fromLocationId == fromLocationId && it.toLocationId == toLocationId)
        }
    saveWorldMapData(currentData.copy(paths = updatedPaths))
}

/**
 * Check if a level is ready to play by its ID.
 */
fun EditorStorage.isLevelReadyToPlay(levelId: String): Boolean {
    val level = getLevel(levelId) ?: return false
    return isLevelReadyToPlay(level)
}

/**
 * Checks if a level is ready to play.
 * A level is ready if:
 * - It has at least one available tower
 * - It has at least one enemy spawn configured, unless it is a sandbox level
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
fun EditorStorage.isLevelReadyToPlay(level: EditorLevel): Boolean {
    if (!level.isReadyToPlay()) {
        return false
    }

    // Also check if the map is ready to use
    val map = getMap(level.mapId)
    if (map == null) {
        return false
    }

    if (!level.isSandbox && level.spawnGroups?.let { spawnGroupIssues(it, map, level.events.stoppedSpawnLoopIds()).isNotEmpty() } == true) {
        return false
    }

    // Validate that the map has a structurally valid path.
    // Always include rivers as walkable here (matching the editor's default), so that
    // community maps whose path crosses river tiles are not incorrectly rejected.
    // Whether enemies can actually cross rivers during gameplay is determined by the
    // pathfinding system at runtime, not at level-load time.
    if (!map.validateReadyToUse(includeRiversAsWalkable = true)) {
        return false
    }

    // When the map allows no direct path, the level's portals must bridge the gap.
    if (map.allowNoDirectPath) {
        val portals = level.getEffectiveInitialData().portals
        if (!map.validateReadyToUseWithPortals(portals, includeRiversAsWalkable = true)) {
            return false
        }
    }

    if (map.allowNoBuildableTiles && !map.hasBuildablePlacementTiles() && !level.hasNoBuildableTileFallback()) {
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
fun EditorStorage.validateAllPrerequisites(): PrerequisiteValidationResult {
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

    fun detectCycle(
        levelId: String,
        currentPath: MutableSet<String>,
    ): Boolean {
        if (levelId in currentPath) {
            return true // Cycle detected
        }
        if (levelId in visited) {
            return false // Already processed without cycle
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

    val isValid =
        missingLevelIds.isEmpty() &&
            circularDependencies.isEmpty() &&
            !disconnectedFromFinal

    return PrerequisiteValidationResult(
        isValid = isValid,
        missingLevelIds = missingLevelIds,
        circularDependencies = circularDependencies,
        unreachableLevels = unreachableLevels,
        disconnectedFromFinal = disconnectedFromFinal,
    )
}

/**
 * Check if a specific level is unlocked based on prerequisites and won levels.
 * @param levelId The level to check
 * @param wonLevelIds Set of level IDs that have been won
 * @return true if the level is unlocked
 */
fun EditorStorage.isLevelUnlocked(
    levelId: String,
    wonLevelIds: Set<String>,
): Boolean {
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
fun EditorStorage.getUnlockedLevelIds(wonLevelIds: Set<String>): Set<String> =
    getAllLevels()
        .filter { isLevelUnlocked(it.id, wonLevelIds) }
        .map { it.id }
        .toSet()
