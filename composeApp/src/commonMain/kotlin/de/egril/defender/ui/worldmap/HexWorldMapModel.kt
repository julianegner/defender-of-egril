package de.egril.defender.ui.worldmap

import de.egril.defender.model.Position
import de.egril.defender.model.LevelStatus

/**
 * Tile types for the world map hexagonal grid
 */
enum class WorldMapTileType {
    LEVEL,           // A playable level tile
    PATH,            // A path connecting levels
    MOUNTAIN,        // Decorative mountain landscape
    RIVER,           // Decorative river landscape
    LAKE,            // Decorative lake landscape  
    FOREST,          // Decorative forest landscape
    EMPTY            // Empty/background tile
}

/**
 * Represents a tile in the hexagonal world map
 */
data class WorldMapTile(
    val position: Position,
    val type: WorldMapTileType,
    val levelId: String? = null,         // For LEVEL tiles, the editor level ID
    val levelIndex: Int? = null,          // For LEVEL tiles, the sequential index (1-based)
    val isFinalLevel: Boolean = false,    // True if this is "the_final_stand" level
    val isTutorialLevel: Boolean = false  // True if this is the tutorial level
)

/**
 * Represents a level's display info on the world map
 */
data class WorldMapLevelInfo(
    val levelId: String,
    val levelIndex: Int,              // 1-based index for display
    val name: String,
    val subtitle: String,
    val status: LevelStatus,
    val position: Position,
    val isFinalLevel: Boolean,
    val isTutorialLevel: Boolean,
    val prerequisites: Set<String>
)

/**
 * Represents the complete hexagonal world map
 */
data class HexWorldMap(
    val width: Int,
    val height: Int,
    val tiles: Map<Position, WorldMapTile>,
    val levels: List<WorldMapLevelInfo>,
    val pathConnections: List<Pair<Position, Position>>  // Connections between levels for path drawing
)
