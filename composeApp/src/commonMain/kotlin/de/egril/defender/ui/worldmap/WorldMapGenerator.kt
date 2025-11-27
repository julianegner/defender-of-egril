package de.egril.defender.ui.worldmap

import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorStorage
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.Position
import de.egril.defender.model.WorldLevel
import kotlin.math.max
import kotlin.math.min

/**
 * Generator for creating hexagonal world maps from level prerequisites
 */
object WorldMapGenerator {
    
    // Map dimensions
    private const val MAP_WIDTH = 20
    private const val MAP_HEIGHT = 16
    
    // Level positions are calculated based on their depth in the prerequisite tree
    // Tutorial/entry levels at bottom, final level at top right
    
    /**
     * Generate a hexagonal world map from the current level sequence and statuses
     */
    fun generateWorldMap(worldLevels: List<WorldLevel>): HexWorldMap {
        if (worldLevels.isEmpty()) {
            return HexWorldMap(
                width = MAP_WIDTH,
                height = MAP_HEIGHT,
                tiles = emptyMap(),
                levels = emptyList(),
                pathConnections = emptyList()
            )
        }
        
        // Get all editor levels for prerequisite info
        val editorLevels = EditorStorage.getAllLevels().associateBy { it.id }
        
        // Build the level graph and calculate depths
        val levelDepths = calculateLevelDepths(worldLevels, editorLevels)
        
        // Calculate positions for each level based on depth
        val levelPositions = calculateLevelPositions(worldLevels, levelDepths, editorLevels)
        
        // Generate tiles
        val tiles = mutableMapOf<Position, WorldMapTile>()
        val pathConnections = mutableListOf<Pair<Position, Position>>()
        
        // Create level info list
        val levels = mutableListOf<WorldMapLevelInfo>()
        
        for ((index, worldLevel) in worldLevels.withIndex()) {
            val position = levelPositions[worldLevel.level.editorLevelId] ?: continue
            val editorLevel = editorLevels[worldLevel.level.editorLevelId]
            
            val isFinalLevel = worldLevel.level.editorLevelId == "the_final_stand"
            val isTutorialLevel = editorLevel?.prerequisites?.isEmpty() == true && 
                                  worldLevel.level.name.contains("Tutorial", ignoreCase = true)
            
            // Create level tile
            tiles[position] = WorldMapTile(
                position = position,
                type = WorldMapTileType.LEVEL,
                levelId = worldLevel.level.editorLevelId,
                levelIndex = index + 1,
                isFinalLevel = isFinalLevel,
                isTutorialLevel = isTutorialLevel
            )
            
            // Create level info
            levels.add(WorldMapLevelInfo(
                levelId = worldLevel.level.editorLevelId ?: "",
                levelIndex = index + 1,
                name = worldLevel.level.name,
                subtitle = worldLevel.level.subtitle,
                status = worldLevel.status,
                position = position,
                isFinalLevel = isFinalLevel,
                isTutorialLevel = isTutorialLevel,
                prerequisites = editorLevel?.prerequisites ?: emptySet()
            ))
            
            // Add path connections from prerequisites
            editorLevel?.prerequisites?.forEach { prereqId ->
                val prereqPosition = levelPositions[prereqId]
                if (prereqPosition != null) {
                    pathConnections.add(Pair(prereqPosition, position))
                }
            }
        }
        
        // Generate path tiles between connected levels
        for ((from, to) in pathConnections) {
            generatePathBetweenLevels(from, to, tiles)
        }
        
        // Add decorative landscape tiles
        addLandscapeTiles(tiles)
        
        return HexWorldMap(
            width = MAP_WIDTH,
            height = MAP_HEIGHT,
            tiles = tiles,
            levels = levels,
            pathConnections = pathConnections
        )
    }
    
    /**
     * Calculate the depth of each level in the prerequisite tree
     * Entry levels (no prerequisites) have depth 0
     * Final level should have the highest depth
     */
    private fun calculateLevelDepths(
        worldLevels: List<WorldLevel>,
        editorLevels: Map<String, EditorLevel>
    ): Map<String, Int> {
        val depths = mutableMapOf<String, Int>()
        
        // Initialize all levels with depth -1 (unprocessed)
        worldLevels.forEach { worldLevel ->
            worldLevel.level.editorLevelId?.let { depths[it] = -1 }
        }
        
        // Process levels in order of dependencies
        var changed = true
        while (changed) {
            changed = false
            for (worldLevel in worldLevels) {
                val levelId = worldLevel.level.editorLevelId ?: continue
                val editorLevel = editorLevels[levelId]
                
                if (depths[levelId] != -1) continue  // Already processed
                
                val prereqs = editorLevel?.prerequisites ?: emptySet()
                
                if (prereqs.isEmpty()) {
                    // Entry level
                    depths[levelId] = 0
                    changed = true
                } else {
                    // Check if all prerequisites have been processed
                    val prereqDepths = prereqs.mapNotNull { depths[it] }.filter { it >= 0 }
                    if (prereqDepths.size == prereqs.size) {
                        // All prerequisites processed, this level is max(prereq depths) + 1
                        val maxPrereqDepth = prereqDepths.maxOrNull() ?: 0
                        depths[levelId] = maxPrereqDepth + 1
                        changed = true
                    }
                }
            }
        }
        
        // Handle any remaining unprocessed levels (circular deps or missing prereqs)
        val maxDepth = depths.values.maxOrNull() ?: 0
        depths.forEach { (id, depth) ->
            if (depth == -1) {
                depths[id] = maxDepth + 1
            }
        }
        
        return depths
    }
    
    /**
     * Calculate positions for each level on the hex grid
     * - Entry/tutorial levels at bottom
     * - Final level at top right
     * - Levels are spread horizontally based on their position in the sequence
     */
    private fun calculateLevelPositions(
        worldLevels: List<WorldLevel>,
        levelDepths: Map<String, Int>,
        editorLevels: Map<String, EditorLevel>
    ): Map<String, Position> {
        val positions = mutableMapOf<String, Position>()
        
        // Group levels by depth
        val levelsByDepth = mutableMapOf<Int, MutableList<String>>()
        for (worldLevel in worldLevels) {
            val levelId = worldLevel.level.editorLevelId ?: continue
            val depth = levelDepths[levelId] ?: 0
            levelsByDepth.getOrPut(depth) { mutableListOf() }.add(levelId)
        }
        
        val maxDepth = levelsByDepth.keys.maxOrNull() ?: 0
        
        // Calculate Y positions based on depth (depth 0 at bottom, max depth at top)
        // Reserve space at edges for landscape
        val usableHeight = MAP_HEIGHT - 4  // Leave 2 tiles margin at top and bottom
        val startY = MAP_HEIGHT - 3  // Start from bottom
        
        for ((depth, levelIds) in levelsByDepth.entries.sortedBy { it.key }) {
            // Calculate Y position for this depth level
            val depthProgress = if (maxDepth > 0) depth.toFloat() / maxDepth else 0f
            val y = (startY - (depthProgress * usableHeight)).toInt().coerceIn(2, MAP_HEIGHT - 3)
            
            // Spread levels horizontally
            val usableWidth = MAP_WIDTH - 4  // Leave margin on sides
            val startX = 2
            val spacing = if (levelIds.size > 1) usableWidth / (levelIds.size) else usableWidth / 2
            
            for ((index, levelId) in levelIds.withIndex()) {
                var x = startX + spacing * index + spacing / 2
                
                // Special positioning for final level (top right)
                if (levelId == "the_final_stand") {
                    x = MAP_WIDTH - 4
                }
                
                // Check for tutorial level (position at bottom center-left)
                val editorLevel = editorLevels[levelId]
                if (editorLevel?.prerequisites?.isEmpty() == true && 
                    worldLevels.any { it.level.editorLevelId == levelId && it.level.name.contains("Tutorial", ignoreCase = true) }) {
                    x = 4
                }
                
                positions[levelId] = Position(x.coerceIn(2, MAP_WIDTH - 3), y)
            }
        }
        
        return positions
    }
    
    /**
     * Generate path tiles between two level positions
     */
    private fun generatePathBetweenLevels(
        from: Position,
        to: Position,
        tiles: MutableMap<Position, WorldMapTile>
    ) {
        // Simple straight-line path using Bresenham-like algorithm for hexagons
        val path = calculateHexPath(from, to)
        for (pos in path) {
            // Don't overwrite level tiles
            if (tiles[pos]?.type == WorldMapTileType.LEVEL) continue
            tiles[pos] = WorldMapTile(
                position = pos,
                type = WorldMapTileType.PATH
            )
        }
    }
    
    /**
     * Calculate a path between two hex positions
     */
    private fun calculateHexPath(from: Position, to: Position): List<Position> {
        val path = mutableListOf<Position>()
        
        var current = from
        while (current != to) {
            val dx = to.x - current.x
            val dy = to.y - current.y
            
            // Move in the direction with the larger difference
            val nextX = when {
                dx > 0 -> current.x + 1
                dx < 0 -> current.x - 1
                else -> current.x
            }
            val nextY = when {
                dy > 0 -> current.y + 1
                dy < 0 -> current.y - 1
                else -> current.y
            }
            
            // Alternate between horizontal and vertical movement
            current = if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) {
                Position(nextX, current.y)
            } else {
                Position(current.x, nextY)
            }
            
            if (current != to) {
                path.add(current)
            }
        }
        
        return path
    }
    
    /**
     * Add decorative landscape tiles to empty areas
     */
    private fun addLandscapeTiles(tiles: MutableMap<Position, WorldMapTile>) {
        // Add mountains at top edges
        for (x in 0 until MAP_WIDTH) {
            for (y in 0..1) {
                val pos = Position(x, y)
                if (!tiles.containsKey(pos)) {
                    tiles[pos] = WorldMapTile(
                        position = pos,
                        type = WorldMapTileType.MOUNTAIN
                    )
                }
            }
        }
        
        // Add forest scattered around
        val forestPositions = listOf(
            Position(0, 4), Position(1, 5), Position(0, 8), Position(1, 9),
            Position(MAP_WIDTH - 1, 6), Position(MAP_WIDTH - 2, 7),
            Position(MAP_WIDTH - 1, 10), Position(MAP_WIDTH - 2, 11)
        )
        for (pos in forestPositions) {
            if (!tiles.containsKey(pos) && pos.x in 0 until MAP_WIDTH && pos.y in 0 until MAP_HEIGHT) {
                tiles[pos] = WorldMapTile(
                    position = pos,
                    type = WorldMapTileType.FOREST
                )
            }
        }
        
        // Add a river/lake at bottom edges
        for (x in 0 until MAP_WIDTH) {
            val pos = Position(x, MAP_HEIGHT - 1)
            if (!tiles.containsKey(pos)) {
                tiles[pos] = WorldMapTile(
                    position = pos,
                    type = if (x % 3 == 0) WorldMapTileType.LAKE else WorldMapTileType.RIVER
                )
            }
        }
        
        // Fill remaining tiles as empty
        for (x in 0 until MAP_WIDTH) {
            for (y in 0 until MAP_HEIGHT) {
                val pos = Position(x, y)
                if (!tiles.containsKey(pos)) {
                    tiles[pos] = WorldMapTile(
                        position = pos,
                        type = WorldMapTileType.EMPTY
                    )
                }
            }
        }
    }
}
