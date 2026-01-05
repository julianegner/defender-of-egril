package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position
import de.egril.defender.ui.common.LevelInfoEnemiesLevelData

/**
 * Represents tile types in the map editor
 */
enum class TileType {
    PATH,           // Path where enemies walk
    BUILD_AREA,     // Area where towers can be built (adjacent to path)
    ISLAND,         // Build islands
    NO_PLAY,        // Not playable area
    SPAWN_POINT,    // Enemy spawn points
    TARGET,         // Target position
    RIVER           // River tile (movable with bridges)
}

/**
 * Map data for the editor
 */
data class EditorMap(
    val id: String,
    val name: String = "",
    val nameKey: String? = null,  // Optional string resource key for translation (e.g., "map_spiral_challenge")
    val width: Int,
    val height: Int,
    val tiles: Map<String, TileType>,  // "x,y" -> TileType
    val readyToUse: Boolean = false,  // True if map has valid path from spawn to target
    val worldMapPosition: Position? = null,  // Position on world map (x,y as permille 0-1000, null = auto-calculate)
    val riverTiles: Map<String, de.egril.defender.model.RiverTile> = emptyMap()  // "x,y" -> RiverTile (for tiles with TileType.RIVER)
) {
    fun getTileType(x: Int, y: Int): TileType {
        return tiles["$x,$y"] ?: TileType.NO_PLAY
    }
    
    fun getSpawnPoints(): List<Position> {
        return tiles.filter { it.value == TileType.SPAWN_POINT }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
    }
    
    fun getTarget(): Position? {
        return getTargets().firstOrNull()
    }
    
    fun getTargets(): List<Position> {
        return tiles.filter { it.value == TileType.TARGET }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
    }
    
    fun getPathCells(): Set<Position> {
        return tiles.filter { it.value == TileType.PATH }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
            .toSet()
    }
    
    fun getBuildIslands(): Set<Position> {
        return tiles.filter { it.value == TileType.ISLAND }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
            .toSet()
    }
    
    fun getBuildAreas(): Set<Position> {
        return tiles.filter { it.value == TileType.BUILD_AREA }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
            .toSet()
    }
    
    fun getRiverCells(): Set<Position> {
        return tiles.filter { it.value == TileType.RIVER }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
            .toSet()
    }
    
    fun getRiverTile(x: Int, y: Int): de.egril.defender.model.RiverTile? {
        return riverTiles["$x,$y"]
    }
    
    /**
     * Get all river tiles as a map keyed by Position
     */
    fun getRiverTilesMap(): Map<Position, de.egril.defender.model.RiverTile> {
        return riverTiles.mapKeys { (key, _) ->
            val parts = key.split(",")
            Position(parts[0].toInt(), parts[1].toInt())
        }
    }
    
    /**
     * Validates if map is ready to use:
     * - Has at least one spawn point
     * - Has at least one target
     * - ALL spawn points have a continuous path at least one target
     * 
     * @param includeRiversAsWalkable If true, river cells are considered walkable for validation
     */
    fun validateReadyToUse(includeRiversAsWalkable: Boolean = true): Boolean {
        val spawnPoints = getSpawnPoints()
        val targets = getTargets()
        val pathCells = getPathCells()
        val riverCells = getRiverCells()
        
        if (spawnPoints.isEmpty()) return false
        if (targets.isEmpty()) return false
        
        // Build set of traversable cells (spawn points + path cells + all targets)
        val traversableCells = pathCells.toMutableSet()
        
        // Add river cells only if requested (for levels with bridge-building enemies)
        if (includeRiversAsWalkable) {
            traversableCells.addAll(riverCells)
        }
        
        traversableCells.addAll(spawnPoints)
        traversableCells.addAll(targets)
        
        // Check if there's a path from all spawn points to any target using BFS
        return spawnPoints.all{ spawn ->
            targets.any{ target ->
                hasPathBFS(spawn, target, traversableCells)
            }
        }
    }
    
    private fun hasPathBFS(start: Position, end: Position, validCells: Set<Position>): Boolean {
        if (start == end) return true
        
        val queue = mutableListOf(start)
        val visited = mutableSetOf(start)
        
        while (queue.isNotEmpty()) {
            val current = queue.removeAt(0)
            
            // Check neighbors (using hex neighbors)
            val neighbors = listOf(
                Position(current.x + 1, current.y),
                Position(current.x - 1, current.y),
                Position(current.x, current.y + 1),
                Position(current.x, current.y - 1),
                Position(current.x + if (current.y % 2 == 0) -1 else 1, current.y + 1),
                Position(current.x + if (current.y % 2 == 0) -1 else 1, current.y - 1)
            )
            
            for (neighbor in neighbors) {
                if (neighbor == end) return true
                
                if (neighbor !in visited && neighbor in validCells) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                }
            }
        }
        
        return false
    }
}

/**
 * Enemy spawn configuration
 */
data class EditorEnemySpawn(
    val attackerType: AttackerType,
    val level: Int = 1,
    val spawnTurn: Int,
    val spawnPoint: Position? = null  // Fixed spawn point for this enemy (null for backward compatibility)
) {
    val healthPoints: Int get() = attackerType.health * level
}

/**
 * Waypoint configuration for the editor
 * Stores waypoint position and the next target position (another waypoint or final target)
 */
data class EditorWaypoint(
    val position: Position,
    val nextTargetPosition: Position
)

/**
 * Result of waypoint validation containing detailed information
 */
data class WaypointValidationResult(
    val isValid: Boolean,
    val circularDependencies: Set<Position> = emptySet(),  // Positions involved in circular paths
    val unconnectedWaypoints: Set<Position> = emptySet(),  // Waypoints without connections
    val waypointChains: List<WaypointChain> = emptyList()  // All waypoint chains from spawn to target
)

/**
 * Represents a chain of waypoints from a spawn point to the target
 */
data class WaypointChain(
    val startPosition: Position,  // Spawn point or first waypoint
    val positions: List<Position>,  // Intermediate waypoints
    val endPosition: Position?,  // Target or null if incomplete
    val hasCircularDependency: Boolean = false
)

/**
 * Level configuration for the editor
 */
data class EditorLevel(
    val id: String,
    val mapId: String,
    val title: String,
    val titleKey: String? = null,  // Optional string resource key for title translation (e.g., "level_first_battle_title")
    val subtitle: String = "",
    val subtitleKey: String? = null,  // Optional string resource key for subtitle translation (e.g., "level_first_battle_subtitle")
    val startCoins: Int,
    val startHealthPoints: Int = 10,
    val enemySpawns: List<EditorEnemySpawn>,
    val availableTowers: Set<DefenderType>,  // Which towers can be built
    val waypoints: List<EditorWaypoint> = emptyList(),  // Waypoints for complex pathing
    val prerequisites: Set<String> = emptySet(),  // Level IDs that must be won to unlock this level
    val requiredPrerequisiteCount: Int? = null  // Number of prerequisites needed (null = all required)
) {
    /**
     * Get the effective required prerequisite count.
     * Returns the size of prerequisites if requiredPrerequisiteCount is null or larger than prerequisites size.
     */
    fun getEffectiveRequiredCount(): Int {
        return when {
            prerequisites.isEmpty() -> 0
            requiredPrerequisiteCount == null -> prerequisites.size
            requiredPrerequisiteCount >= prerequisites.size -> prerequisites.size
            else -> requiredPrerequisiteCount
        }
    }
    /**
     * Checks if this level is ready to play (level-specific checks only).
     * A level is ready if:
     * - It has at least one available tower
     * - It has at least one enemy spawn configured (each EditorEnemySpawn represents one enemy unit)
     * - Start coins are greater than zero
     * - Start health points are greater than zero
     * 
     * Note: This does NOT check if the associated map is ready.
     * Use EditorStorage.isLevelReadyToPlay() for a complete readiness check including map validation.
     * - All waypoints eventually lead to the final target (checked separately with map context)
     */
    fun isReadyToPlay(): Boolean {
        return availableTowers.isNotEmpty() && 
               enemySpawns.isNotEmpty() && 
               startCoins > 0 && 
               startHealthPoints > 0
    }
    
    /**
     * Validates that all waypoints form valid chains that eventually lead to a target.
     * This ensures enemies following waypoints will reach one of the targets.
     * @param targetPositions List of valid target positions from the map
     * @return true if waypoints are valid (or if there are no waypoints)
     */
    fun validateWaypoints(targetPositions: List<Position>): Boolean {
        if (waypoints.isEmpty()) return true  // No waypoints is valid
        if (targetPositions.isEmpty()) return false  // No targets is invalid
        
        // Build a map of waypoint position to next target
        val waypointMap = waypoints.associateBy { it.position }
        val waypointPositions = waypointMap.keys
        val targetSet = targetPositions.toSet()
        
        // Check each waypoint can eventually reach a target
        return waypoints.all { waypoint ->
            val visited = mutableSetOf<Position>()
            var current = waypoint.nextTargetPosition
            
            // Follow the chain until we reach a target or detect a loop
            while (current !in targetSet) {
                if (current in visited) {
                    // Loop detected - this waypoint will never reach a target
                    return@all false
                }
                visited.add(current)
                
                // If current is a waypoint, follow it
                val nextWaypoint = waypointMap[current]
                if (nextWaypoint != null) {
                    current = nextWaypoint.nextTargetPosition
                } else if (current in targetSet) {
                    // Reached a target
                    break
                } else {
                    // Current is not a waypoint and not a target - assume it's valid (enemies will path there)
                    // This allows waypoints to point to intermediate positions
                    break
                }
            }
            
            true
        }
    }
    
    /**
     * Performs detailed waypoint validation and returns comprehensive results.
     * @param targetPositions List of valid target positions from the map
     * @param spawnPoints List of spawn points from the map
     * @return WaypointValidationResult with detailed validation information
     */
    fun validateWaypointsDetailed(
        targetPositions: List<Position>,
        spawnPoints: List<Position>
    ): WaypointValidationResult {
        // If there are multiple targets, waypoints are required
        if (targetPositions.size > 1 && waypoints.isEmpty()) {
            return WaypointValidationResult(isValid = false)
        }
        
        if (waypoints.isEmpty()) {
            return WaypointValidationResult(isValid = true)
        }
        
        if (targetPositions.isEmpty()) {
            return WaypointValidationResult(isValid = false)
        }
        
        val waypointMap = waypoints.associateBy { it.position }
        val waypointPositions = waypointMap.keys
        val circularDeps = mutableSetOf<Position>()
        val chains = mutableListOf<WaypointChain>()
        val targetSet = targetPositions.toSet()
        
        // Find waypoints that are sources (have a next target) but not targets (no incoming)
        val targetsSet = waypoints.map { it.nextTargetPosition }.toSet()
        val sourcesSet = waypoints.map { it.position }.toSet()
        
        // Unconnected: waypoints that are either:
        // 1. Sources without being targets (no incoming connection)
        // 2. Targets without being sources (no outgoing connection)
        val unconnectedPositions = mutableSetOf<Position>()
        
        // Find waypoints without incoming connections (except if they're spawn points)
        sourcesSet.forEach { pos ->
            if (pos !in targetsSet && pos !in spawnPoints) {
                unconnectedPositions.add(pos)
            }
        }
        
        // Find waypoints without outgoing connections (except if they point to a target)
        targetsSet.forEach { pos ->
            if (pos !in sourcesSet && pos !in targetSet) {
                unconnectedPositions.add(pos)
            }
        }
        
        // Build chains starting from each spawn point and waypoint source
        val allStarts = (spawnPoints + sourcesSet).distinct()
        
        for (start in allStarts) {
            val chain = mutableListOf<Position>()
            val visited = mutableSetOf<Position>()
            var current = start
            var hasCircular = false
            var reachedTarget: Position? = null
            
            // Follow the chain
            while (true) {
                val waypoint = waypointMap[current]
                if (waypoint == null) {
                    // Not a waypoint - check if it's a target
                    if (current in targetSet) {
                        reachedTarget = current
                    }
                    break
                }
                
                if (current in visited) {
                    // Circular dependency detected
                    hasCircular = true
                    circularDeps.add(current)
                    // Add all positions in the cycle
                    val cycleStart = current
                    var pos = waypoint.nextTargetPosition
                    while (pos != cycleStart && pos in waypointMap) {
                        circularDeps.add(pos)
                        pos = waypointMap[pos]!!.nextTargetPosition
                    }
                    break
                }
                
                visited.add(current)
                chain.add(current)
                current = waypoint.nextTargetPosition
                
                if (current in targetSet) {
                    reachedTarget = current
                    break
                }
            }
            
            // Only add chains that start from spawn points or unconnected waypoints
            if (start in spawnPoints || start in unconnectedPositions) {
                chains.add(
                    WaypointChain(
                        startPosition = start,
                        positions = chain,
                        endPosition = reachedTarget ?: current,
                        hasCircularDependency = hasCircular
                    )
                )
            }
        }
        
        val isValid = circularDeps.isEmpty() && unconnectedPositions.isEmpty()
        
        return WaypointValidationResult(
            isValid = isValid,
            circularDependencies = circularDeps,
            unconnectedWaypoints = unconnectedPositions,
            waypointChains = chains
        )
    }

    fun toLevelInfoEnemiesLevelData(index: Int): LevelInfoEnemiesLevelData {

        val enemyCountMap: Map<AttackerType, Int> = mutableMapOf()

        enemySpawns
            .groupingBy { it.attackerType }.eachCount()
            .entries
            .forEach { (attackerType, count) ->
                (enemyCountMap as MutableMap)[attackerType] = count
            }

        return LevelInfoEnemiesLevelData(
            id = "" + index,
            name = this.title,
            subtitle = this.subtitle,
            initialCoins = startCoins,
            healthPoints = startHealthPoints,
            enemyTypeCounts = enemyCountMap
        )
    }
}

/**
 * Result of prerequisite validation containing detailed information
 */
data class PrerequisiteValidationResult(
    val isValid: Boolean,
    val missingLevelIds: Set<String> = emptySet(),  // Level IDs that don't exist
    val circularDependencies: Set<String> = emptySet(),  // Level IDs involved in circular dependencies
    val unreachableLevels: Set<String> = emptySet(),  // Levels that can't be reached from entry points
    val disconnectedFromFinal: Boolean = false  // True if "the_final_stand" is not connected to entry points
)

/**
 * Level sequence configuration
 * @deprecated Kept for backward compatibility - use level prerequisites instead
 */
data class LevelSequence(
    val sequence: List<String>  // List of level IDs in order
)

/**
 * Exception thrown when critical repository data files are missing or empty
 */
class MissingRepositoryDataException(
    val missingCategories: List<String>
) : Exception("Missing or empty repository data categories: ${missingCategories.joinToString(", ")}")

