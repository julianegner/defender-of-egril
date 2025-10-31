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
    val tiles: Map<String, TileType>  // "x,y" -> TileType
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
