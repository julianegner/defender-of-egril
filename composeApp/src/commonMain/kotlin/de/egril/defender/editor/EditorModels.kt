package de.egril.defender.editor

import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.model.Position

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
    WAYPOINT        // Future: waypoints for path control
}

/**
 * Map data for the editor
 */
data class EditorMap(
    val id: String,
    val name: String = "",
    val width: Int,
    val height: Int,
    val tiles: Map<String, TileType>,  // "x,y" -> TileType
    val readyToUse: Boolean = false  // True if map has valid path from spawn to target
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
        return tiles.filter { it.value == TileType.TARGET }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
            .firstOrNull()
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
    
    fun getWaypoints(): List<Position> {
        return tiles.filter { it.value == TileType.WAYPOINT }
            .map { 
                val parts = it.key.split(",")
                Position(parts[0].toInt(), parts[1].toInt())
            }
    }
    
    /**
     * Validates if map is ready to use:
     * - Has at least one spawn point
     * - Has at least one target
     * - Has a continuous path from spawn to target
     */
    fun validateReadyToUse(): Boolean {
        val spawnPoints = getSpawnPoints()
        val target = getTarget()
        val pathCells = getPathCells()
        
        if (spawnPoints.isEmpty()) return false
        if (target == null) return false
        
        // Build set of traversable cells (spawn points + path cells + target + waypoints)
        val traversableCells = pathCells.toMutableSet()
        traversableCells.addAll(spawnPoints)
        traversableCells.add(target)
        traversableCells.addAll(getWaypoints())  // Waypoints are also traversable
        
        // Check if there's a path from any spawn point to target using BFS
        return spawnPoints.any { spawn ->
            hasPathBFS(spawn, target, traversableCells)
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
    val spawnTurn: Int
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
 * Level configuration for the editor
 */
data class EditorLevel(
    val id: String,
    val mapId: String,
    val title: String,
    val subtitle: String = "",
    val startCoins: Int,
    val startHealthPoints: Int = 10,
    val enemySpawns: List<EditorEnemySpawn>,
    val availableTowers: Set<DefenderType>,  // Which towers can be built
    val waypoints: List<EditorWaypoint> = emptyList()  // Waypoints for complex pathing
) {
    /**
     * Checks if this level is ready to play.
     * A level is ready if:
     * - It has at least one available tower
     * - It has at least one enemy spawn configured (each EditorEnemySpawn represents one enemy unit)
     * - Start coins are greater than zero
     * - Start health points are greater than zero
     * - All waypoints eventually lead to the final target (checked separately with map context)
     */
    fun isReadyToPlay(): Boolean {
        return availableTowers.isNotEmpty() && 
               enemySpawns.isNotEmpty() && 
               startCoins > 0 && 
               startHealthPoints > 0
    }
    
    /**
     * Validates that all waypoints form valid chains that eventually lead to the target.
     * This ensures enemies following waypoints will reach the target.
     * @param targetPosition The final target position from the map
     * @return true if waypoints are valid (or if there are no waypoints)
     */
    fun validateWaypoints(targetPosition: Position): Boolean {
        if (waypoints.isEmpty()) return true  // No waypoints is valid
        
        // Build a map of waypoint position to next target
        val waypointMap = waypoints.associateBy { it.position }
        val waypointPositions = waypointMap.keys
        
        // Check each waypoint can eventually reach the target
        return waypoints.all { waypoint ->
            val visited = mutableSetOf<Position>()
            var current = waypoint.nextTargetPosition
            
            // Follow the chain until we reach the target or detect a loop
            while (current != targetPosition) {
                if (current in visited) {
                    // Loop detected - this waypoint will never reach target
                    return@all false
                }
                visited.add(current)
                
                // If current is a waypoint, follow it
                val nextWaypoint = waypointMap[current]
                if (nextWaypoint != null) {
                    current = nextWaypoint.nextTargetPosition
                } else if (current == targetPosition) {
                    // Reached target
                    break
                } else {
                    // Current is not a waypoint and not the target - assume it's valid (enemies will path there)
                    // This allows waypoints to point to intermediate positions
                    break
                }
            }
            
            true
        }
    }
}

/**
 * Level sequence configuration
 */
data class LevelSequence(
    val sequence: List<String>  // List of level IDs in order
)
