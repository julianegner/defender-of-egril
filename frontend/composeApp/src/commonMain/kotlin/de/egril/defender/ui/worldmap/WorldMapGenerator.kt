package de.egril.defender.ui.worldmap

import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.EditorStorage
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.Position
import de.egril.defender.model.WorldLevel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs

/**
 * Generator for creating hexagonal world maps from level prerequisites
 */
object WorldMapGenerator {
    
    // Map dimensions - wider than tall for horizontal spread
    private const val MAP_WIDTH = 50
    private const val MAP_HEIGHT = 20
    
    // Minimum path tiles between levels
    private const val MIN_PATH_TILES = 3
    
    // Level positions are calculated based on their depth in the prerequisite tree
    // Tutorial/entry levels at bottom left, final level at top right
    
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
        
        // Generate curved path tiles between connected levels
        for ((from, to) in pathConnections) {
            generateCurvedPathBetweenLevels(from, to, tiles)
        }
        
        // Add entry paths from the bottom of the map to entry levels (levels with no prerequisites)
        addEntryPathsFromBottom(worldLevels, levelPositions, editorLevels, tiles)
        
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
     * - Spread levels across the width from left to right based on depth
     * - Stagger vertically for visual variety
     * - Tutorial at bottom left, final level at top right
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
        
        // Horizontal layout: spread from left to right based on depth
        // Each depth gets a range of X positions
        val margin = 4
        val usableWidth = MAP_WIDTH - 2 * margin
        val depthWidth = if (maxDepth > 0) usableWidth / (maxDepth + 1) else usableWidth
        
        // Vertical center line with staggered offsets
        val centerY = MAP_HEIGHT / 2
        
        for ((depth, levelIds) in levelsByDepth.entries.sortedBy { it.key }) {
            // Calculate X range for this depth (left to right progression)
            val depthCenterX = margin + (depth * depthWidth) + depthWidth / 2
            
            // Vertical staggering pattern - alternate up/down from center
            val verticalSpacing = MIN_PATH_TILES + 2
            val totalHeight = (levelIds.size - 1) * verticalSpacing
            val startY = centerY - totalHeight / 2
            
            for ((index, levelId) in levelIds.withIndex()) {
                // Stagger horizontally within the depth zone
                val horizontalOffset = if (index % 2 == 0) -2 else 2
                var x = depthCenterX + horizontalOffset
                
                // Calculate Y with zigzag pattern
                var y = startY + index * verticalSpacing
                
                // Add some wave/zigzag to Y based on depth
                val zigzagOffset = if (depth % 2 == 0) 1 else -1
                y += zigzagOffset
                
                // Special positioning for final level (top right corner)
                if (levelId == "the_final_stand") {
                    x = MAP_WIDTH - margin - 2
                    y = margin
                }
                
                // Check for tutorial level (position at bottom left)
                val editorLevel = editorLevels[levelId]
                if (editorLevel?.prerequisites?.isEmpty() == true && 
                    worldLevels.any { it.level.editorLevelId == levelId && it.level.name.contains("Tutorial", ignoreCase = true) }) {
                    x = margin + 2
                    y = MAP_HEIGHT - margin - 2
                }
                
                positions[levelId] = Position(
                    x.coerceIn(margin, MAP_WIDTH - margin - 1), 
                    y.coerceIn(margin, MAP_HEIGHT - margin - 1)
                )
            }
        }
        
        return positions
    }
    
    /**
     * Generate curved path tiles between two level positions
     * Creates a more natural winding path instead of straight lines
     */
    private fun generateCurvedPathBetweenLevels(
        from: Position,
        to: Position,
        tiles: MutableMap<Position, WorldMapTile>
    ) {
        val path = calculateCurvedHexPath(from, to)
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
     * Calculate a curved path between two hex positions
     * Uses a more natural winding pattern instead of straight lines
     */
    private fun calculateCurvedHexPath(from: Position, to: Position): List<Position> {
        val path = mutableListOf<Position>()
        
        val dx = to.x - from.x
        val dy = to.y - from.y
        
        // For longer distances, create a curved path
        val totalDistance = abs(dx) + abs(dy)
        
        if (totalDistance <= 2) {
            // Short path - just go direct
            return calculateDirectPath(from, to)
        }
        
        // Create a curved path by going through a midpoint offset from the direct line
        // Alternate curve direction based on position for variety
        val curveDirection = if ((from.x + from.y) % 2 == 0) 1 else -1
        
        // Calculate midpoint with curve offset
        val midX = from.x + dx / 2
        val midY = from.y + dy / 2
        
        // Offset the midpoint perpendicular to the path direction
        val offsetAmount = min(3, totalDistance / 3)
        val curveMidpoint = if (abs(dx) > abs(dy)) {
            // Mostly horizontal - curve vertically
            Position(midX, midY + curveDirection * offsetAmount)
        } else {
            // Mostly vertical - curve horizontally
            Position(midX + curveDirection * offsetAmount, midY)
        }
        
        // Create path: from -> curveMidpoint -> to
        path.addAll(calculateDirectPath(from, curveMidpoint))
        path.add(curveMidpoint)
        path.addAll(calculateDirectPath(curveMidpoint, to))
        
        return path.filter { it != from && it != to }
    }
    
    /**
     * Calculate a direct path between two hex positions
     */
    private fun calculateDirectPath(from: Position, to: Position): List<Position> {
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
            
            // Alternate between horizontal and vertical movement for variety
            current = if (abs(dx) >= abs(dy)) {
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
     * Add entry paths from the bottom of the map to entry levels (levels with no prerequisites)
     * These represent the roads leading into the game world
     */
    private fun addEntryPathsFromBottom(
        worldLevels: List<WorldLevel>,
        levelPositions: Map<String, Position>,
        editorLevels: Map<String, EditorLevel>,
        tiles: MutableMap<Position, WorldMapTile>
    ) {
        // Find entry levels (levels with no prerequisites)
        val entryLevelIds = worldLevels.filter { worldLevel ->
            val editorLevel = editorLevels[worldLevel.level.editorLevelId]
            editorLevel?.prerequisites?.isEmpty() == true
        }.mapNotNull { it.level.editorLevelId }
        
        // For each entry level, create a path from the bottom of the map
        for (entryLevelId in entryLevelIds) {
            val levelPosition = levelPositions[entryLevelId] ?: continue
            
            // Create a starting point at the bottom of the map, roughly below the level
            val bottomY = MAP_HEIGHT - 2
            val startX = levelPosition.x.coerceIn(2, MAP_WIDTH - 3)
            val startPosition = Position(startX, bottomY)
            
            // Generate a curved path from the bottom to the entry level
            val path = calculateCurvedHexPath(startPosition, levelPosition)
            
            // Add path tiles
            for (pos in path) {
                // Don't overwrite level tiles
                if (tiles[pos]?.type == WorldMapTileType.LEVEL) continue
                tiles[pos] = WorldMapTile(
                    position = pos,
                    type = WorldMapTileType.PATH
                )
            }
            
            // Also add the starting position as a path tile
            if (!tiles.containsKey(startPosition)) {
                tiles[startPosition] = WorldMapTile(
                    position = startPosition,
                    type = WorldMapTileType.PATH
                )
            }
        }
    }
    
    /**
     * Add decorative landscape tiles to empty areas
     */
    private fun addLandscapeTiles(tiles: MutableMap<Position, WorldMapTile>) {
        // Add mountains at top edge
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
        
        // Add scattered forest along the edges
        val forestPositions = mutableListOf<Position>()
        for (y in 3 until MAP_HEIGHT - 2 step 3) {
            forestPositions.add(Position(0, y))
            forestPositions.add(Position(1, y + 1))
            forestPositions.add(Position(MAP_WIDTH - 1, y))
            forestPositions.add(Position(MAP_WIDTH - 2, y + 1))
        }
        for (pos in forestPositions) {
            if (!tiles.containsKey(pos) && pos.x in 0 until MAP_WIDTH && pos.y in 0 until MAP_HEIGHT) {
                tiles[pos] = WorldMapTile(
                    position = pos,
                    type = WorldMapTileType.FOREST
                )
            }
        }
        
        // Add a river/lake at bottom edge
        for (x in 0 until MAP_WIDTH) {
            val pos = Position(x, MAP_HEIGHT - 1)
            if (!tiles.containsKey(pos)) {
                tiles[pos] = WorldMapTile(
                    position = pos,
                    type = if (x % 4 == 0) WorldMapTileType.LAKE else WorldMapTileType.RIVER
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
