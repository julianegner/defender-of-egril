package de.egril.defender.ui.worldmap

import de.egril.defender.editor.EditorStorage
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPoint
import de.egril.defender.editor.getAllLevels
import de.egril.defender.editor.getAllMaps
import de.egril.defender.editor.getWorldMapData
import de.egril.defender.editor.isLevelReadyToPlay
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.settings.AppSettings

/**
 * Generate world map locations and road connections from the available levels.
 * First tries to use WorldMapData from the worldmap.json file, then falls back
 * to auto-generation based on level prerequisites.
 */
internal fun generateWorldMapLocationsAndRoads(worldLevels: List<WorldLevel>): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
    if (worldLevels.isEmpty()) return emptyList<WorldMapLocation>() to emptyList()

    val sandboxLevelIds = worldLevels.filter { it.level.isSandbox }.mapNotNull { it.level.editorLevelId }.toSet()
    val regularWorldLevels = worldLevels.filterNot { it.level.isSandbox }

    // Get all editor levels for prerequisite info
    val editorLevels = EditorStorage.getAllLevels().associateBy { it.id }
    val allMaps = EditorStorage.getAllMaps()

    // Try to use WorldMapData first
    val worldMapData = EditorStorage.getWorldMapData()

    val (locations, roads) =
        if (regularWorldLevels.isEmpty()) {
            emptyList<WorldMapLocation>() to emptyList()
        } else if (worldMapData.locations.isNotEmpty()) {
            // Use locations from worldmap.json
            generateFromWorldMapData(worldMapData, regularWorldLevels, editorLevels, allMaps)
        } else {
            // Fall back to auto-generation
            generateAutoLocationsAndRoads(regularWorldLevels, editorLevels, allMaps)
        }

    val regularLocations =
        locations.map { location ->
            location.copy(levelIds = location.levelIds.filterNot { it in sandboxLevelIds })
        }
    val sandboxLocation = createSandboxWorldMapLocation(worldLevels)

    return (regularLocations + listOfNotNull(sandboxLocation)) to roads
}

internal fun createSandboxWorldMapLocation(worldLevels: List<WorldLevel>): WorldMapLocation? {
    val levelIds = worldLevels.filter { it.level.isSandbox }.mapNotNull { it.level.editorLevelId }
    if (levelIds.isEmpty()) return null

    val locationData =
        WorldMapLocationData(
            id = "sandbox_levels",
            name = "Sandbox",
            nameKey = "sandbox",
            position = WorldMapPoint(930, 920),
            levelIds = levelIds,
            iconResourceName = "sandbox",
        )
    val (x, y) = locationData.position.toNormalized()
    return WorldMapLocation(
        id = locationData.id,
        x = x,
        y = y,
        levelIds = locationData.levelIds,
        name = locationData.name,
        locationData = locationData,
    )
}

/**
 * Generate locations and roads from the WorldMapData file.
 * Only shows locations that have at least one ready-to-play level.
 * Roads are always shown if they match level prerequisites.
 */
private fun generateFromWorldMapData(
    worldMapData: WorldMapData,
    worldLevels: List<WorldLevel>,
    editorLevels: Map<String, de.egril.defender.editor.EditorLevel>,
    allMaps: List<de.egril.defender.editor.EditorMap>,
): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
    val locations = mutableListOf<WorldMapLocation>()
    val locationById = mutableMapOf<String, WorldMapLocation>()

    // Convert WorldMapLocationData to WorldMapLocation
    for (locationData in worldMapData.locations) {
        // Check if at least one level at this location is ready to play
        // If showTestingLevels is false, also check that level is not testing-only
        val hasPlayableLevel =
            locationData.levelIds.any { levelId ->
                val level = editorLevels[levelId]
                val isReadyToPlay = EditorStorage.isLevelReadyToPlay(levelId)
                val isVisibleBasedOnTesting = AppSettings.showTestingLevels.value || level?.testingOnly != true
                isReadyToPlay && isVisibleBasedOnTesting
            }

        // Only add location if it has at least one playable level
        if (hasPlayableLevel) {
            val (x, y) = locationData.position.toNormalized()
            val location =
                WorldMapLocation(
                    id = locationData.id,
                    x = x.coerceIn(0.1f, 0.9f),
                    y = y.coerceIn(0.15f, 0.85f),
                    levelIds = locationData.levelIds,
                    name = locationData.name,
                    locationData = locationData,
                )
            locations.add(location)
            locationById[locationData.id] = location
        }
    }

    // Generate roads from WorldMapData paths
    // Roads are shown even if source/destination locations are hidden
    // All connections are shown on the world map, regardless of validation status
    val roads = mutableListOf<WorldMapRoad>()

    for (pathData in worldMapData.paths) {
        val fromLocation = locationById[pathData.fromLocationId]
        val toLocation = locationById[pathData.toLocationId]

        // If both locations are visible, use them directly
        if (fromLocation != null && toLocation != null) {
            val controlPoints = pathData.controlPoints.map { it.toNormalized() }
            roads.add(
                WorldMapRoad(
                    fromLocation = fromLocation,
                    toLocation = toLocation,
                    controlPoints = controlPoints,
                    type = pathData.type,
                    segmentTypes = pathData.segmentTypes,
                ),
            )
        } else {
            // If locations are hidden but path should still be visible, create temporary locations
            val fromLocationData = worldMapData.findLocation(pathData.fromLocationId)
            val toLocationData = worldMapData.findLocation(pathData.toLocationId)

            if (fromLocationData != null && toLocationData != null) {
                val (fromX, fromY) = fromLocationData.position.toNormalized()
                val (toX, toY) = toLocationData.position.toNormalized()

                val tempFromLocation =
                    fromLocation ?: WorldMapLocation(
                        id = fromLocationData.id,
                        x = fromX.coerceIn(0.1f, 0.9f),
                        y = fromY.coerceIn(0.15f, 0.85f),
                        levelIds = emptyList(),
                        name = fromLocationData.name,
                        locationData = fromLocationData,
                    )
                val tempToLocation =
                    toLocation ?: WorldMapLocation(
                        id = toLocationData.id,
                        x = toX.coerceIn(0.1f, 0.9f),
                        y = toY.coerceIn(0.15f, 0.85f),
                        levelIds = emptyList(),
                        name = toLocationData.name,
                        locationData = toLocationData,
                    )

                val controlPoints = pathData.controlPoints.map { it.toNormalized() }
                roads.add(
                    WorldMapRoad(
                        fromLocation = tempFromLocation,
                        toLocation = tempToLocation,
                        controlPoints = controlPoints,
                        type = pathData.type,
                        segmentTypes = pathData.segmentTypes,
                    ),
                )
            }
        }
    }

    return locations to roads
}

/**
 * Auto-generate locations and roads based on level prerequisites.
 * Used when no WorldMapData file exists.
 */
private fun generateAutoLocationsAndRoads(
    worldLevels: List<WorldLevel>,
    editorLevels: Map<String, de.egril.defender.editor.EditorLevel>,
    allMaps: List<de.egril.defender.editor.EditorMap>,
): Pair<List<WorldMapLocation>, List<WorldMapRoad>> {
    // Group levels by map ID
    val levelsByMap =
        worldLevels.groupBy {
            it.level.mapId ?: it.level.editorLevelId ?: "unknown"
        }

    // Calculate depth of each level based on prerequisites (with memoization)
    val levelDepths = mutableMapOf<String, Int>()
    val currentPath = mutableSetOf<String>()
    for (worldLevel in worldLevels) {
        val levelId = worldLevel.level.editorLevelId ?: continue
        levelDepths[levelId] = calculateLevelDepth(levelId, editorLevels, levelDepths, currentPath)
    }

    val maxDepth = levelDepths.values.maxOrNull() ?: 0
    val locations = mutableListOf<WorldMapLocation>()
    val levelIdToLocation = mutableMapOf<String, WorldMapLocation>()

    // Create locations for each map group
    var locationIndex = 0
    for ((mapId, levels) in levelsByMap) {
        // Try to get custom position from the map file
        val editorMap = allMaps.find { it.id == mapId }
        val customPosition = editorMap?.worldMapPosition

        // Use custom position if available, otherwise calculate based on depth
        val x =
            if (customPosition != null) {
                // Position x is stored as permille (0-1000), convert to 0.0-1.0
                customPosition.x / 1000f
            } else {
                // Calculate average depth for this map's levels
                val avgDepth =
                    levels
                        .mapNotNull {
                            levelDepths[it.level.editorLevelId]
                        }.average()
                        .takeIf { !it.isNaN() } ?: 0.0

                // Position based on depth (left to right)
                if (maxDepth > 0) {
                    0.15f + ((avgDepth / maxDepth.toDouble()) * 0.7).toFloat()
                } else {
                    0.5f
                }
            }

        val y =
            if (customPosition != null) {
                // Position y is stored as permille (0-1000), convert to 0.0-1.0
                customPosition.y / 1000f
            } else {
                // Distribute locations vertically with some variation
                0.25f + (locationIndex % 3) * 0.25f + ((locationIndex / 3) % 2) * 0.1f
            }

        // Get location name from map or first level
        val mapName =
            editorMap?.name?.takeIf { it.isNotEmpty() }
                ?: levels.firstOrNull()?.level?.name
                ?: "Location"

        val location =
            WorldMapLocation(
                id = mapId,
                x = x.coerceIn(0.1f, 0.9f),
                y = y.coerceIn(0.15f, 0.85f),
                levelIds = levels.mapNotNull { it.level.editorLevelId },
                name = mapName,
            )

        locations.add(location)

        // Map each level ID to its location
        for (levelId in location.levelIds) {
            levelIdToLocation[levelId] = location
        }

        locationIndex++
    }

    // Generate road connections based on prerequisites
    val roads = mutableListOf<WorldMapRoad>()
    val addedRoads = mutableSetOf<Pair<String, String>>()

    for (level in editorLevels.values) {
        val levelLocation = levelIdToLocation[level.id] ?: continue

        for (prereqId in level.prerequisites) {
            val prereqLocation = levelIdToLocation[prereqId] ?: continue

            // Avoid duplicate roads (in either direction)
            val roadPair =
                if (prereqLocation.id < levelLocation.id) {
                    prereqLocation.id to levelLocation.id
                } else {
                    levelLocation.id to prereqLocation.id
                }

            if (roadPair !in addedRoads && prereqLocation != levelLocation) {
                addedRoads.add(roadPair)
                roads.add(
                    WorldMapRoad(
                        fromLocation = prereqLocation,
                        toLocation = levelLocation,
                    ),
                )
            }
        }
    }

    return locations to roads
}

/**
 * Calculate the depth of a level in the prerequisite tree using memoization.
 * This is optimized to avoid redundant calculations and handle cycles.
 */
private fun calculateLevelDepth(
    levelId: String,
    editorLevels: Map<String, de.egril.defender.editor.EditorLevel>,
    depthCache: MutableMap<String, Int>,
    currentPath: MutableSet<String>,
): Int {
    // Return cached depth if available
    if (levelId in depthCache) return depthCache[levelId]!!

    // Avoid infinite loops - if we've seen this level in the current path, treat as cycle
    if (levelId in currentPath) return 0
    currentPath.add(levelId)

    val level = editorLevels[levelId]
    if (level == null || level.prerequisites.isEmpty()) {
        depthCache[levelId] = 0
        currentPath.remove(levelId)
        return 0
    }

    val maxPrereqDepth =
        level.prerequisites.maxOfOrNull { prereqId ->
            calculateLevelDepth(prereqId, editorLevels, depthCache, currentPath)
        } ?: 0

    val depth = maxPrereqDepth + 1
    depthCache[levelId] = depth
    currentPath.remove(levelId)

    return depth
}
