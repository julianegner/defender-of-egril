package com.defenderofegril.editor

import com.defenderofegril.model.AttackerType
import com.defenderofegril.model.DefenderType
import com.defenderofegril.model.Position

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
        
        // Build set of traversable cells (spawn points + path cells + target)
        val traversableCells = pathCells.toMutableSet()
        traversableCells.addAll(spawnPoints)
        traversableCells.add(target)
        
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
    val availableTowers: Set<DefenderType>  // Which towers can be built
)

/**
 * Level sequence configuration
 */
data class LevelSequence(
    val sequence: List<String>  // List of level IDs in order
)
