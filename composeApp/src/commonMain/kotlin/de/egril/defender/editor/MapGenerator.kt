package de.egril.defender.editor

import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.hexDistanceTo

/**
 * Map generation utilities for creating various map patterns
 */
object MapGenerator {
    
    /**
     * Create a spiral map with target in center and spawn points in corners
     */
    fun createSpiralMap(
        id: String = "map_spiral",
        name: String = "Spiral Challenge Map",
        size: Int = 40
    ): EditorMap {
        val center = size / 2
        val tiles = mutableMapOf<String, TileType>()
        
        // Set spawn points in corners
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["${size - 1},0"] = TileType.SPAWN_POINT
        tiles["0,${size - 1}"] = TileType.SPAWN_POINT
        tiles["${size - 1},${size - 1}"] = TileType.SPAWN_POINT
        
        // Set target at center
        tiles["$center,$center"] = TileType.TARGET
        
        // Generate spiral path from corners toward center
        val spiralPath = generateSpiralPath(size, center)
        spiralPath.forEach { pos ->
            val key = "${pos.x},${pos.y}"
            if (!tiles.containsKey(key)) {
                tiles[key] = TileType.PATH
            }
        }
        
        // Define circular region around center
        val innerRadius = size / 4  // Inner circular area
        val outerRadius = size / 3  // Outer edge of path area
        
        // Mark cells within the circular region (mostly NO_PLAY)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val key = "$x,$y"
                if (tiles.containsKey(key)) continue
                
                val pos = Position(x, y)
                val distanceFromCenter = pos.hexDistanceTo(Position(center, center))
                
                // Within the circular region - mostly NO_PLAY, except adjacent to path
                if (distanceFromCenter <= outerRadius) {
                    val neighbors = pos.getHexNeighbors()
                    val isAdjacentToPath = neighbors.any { neighbor ->
                        neighbor.x >= 0 && neighbor.x < size &&
                        neighbor.y >= 0 && neighbor.y < size &&
                        tiles["${neighbor.x},${neighbor.y}"] == TileType.PATH
                    }
                    
                    if (isAdjacentToPath) {
                        // Partly buildable - cells adjacent to path within circle
                        tiles[key] = TileType.BUILD_AREA
                    } else {
                        // Mostly non-buildable
                        tiles[key] = TileType.NO_PLAY
                    }
                } else {
                    // Outside the circular region - check if adjacent to path
                    val neighbors = pos.getHexNeighbors()
                    val isAdjacentToPath = neighbors.any { neighbor ->
                        neighbor.x >= 0 && neighbor.x < size &&
                        neighbor.y >= 0 && neighbor.y < size &&
                        tiles["${neighbor.x},${neighbor.y}"] == TileType.PATH
                    }
                    
                    if (isAdjacentToPath) {
                        tiles[key] = TileType.BUILD_AREA
                    }
                }
            }
        }
        
        return EditorMap(
            id = id,
            name = name,
            width = size,
            height = size,
            tiles = tiles,
            readyToUse = false
        )
    }
    
    /**
     * Create "The Plains" map with target in center, spawn points in corners,
     * and 4 2x2 islands positioned 3 tiles away from the center target
     */
    fun createPlainsMap(
        id: String = "map_plains",
        name: String = "The Plains",
        size: Int = 40
    ): EditorMap {
        val center = size / 2
        val tiles = mutableMapOf<String, TileType>()
        
        // Set spawn points in corners
        tiles["0,0"] = TileType.SPAWN_POINT
        tiles["${size - 1},0"] = TileType.SPAWN_POINT
        tiles["0,${size - 1}"] = TileType.SPAWN_POINT
        tiles["${size - 1},${size - 1}"] = TileType.SPAWN_POINT
        
        // Set target at center
        tiles["$center,$center"] = TileType.TARGET
        
        // Create 4 2x2 islands 3 tiles away from center in all directions
        // North, South, East, West
        val islandOffsets = listOf(
            Pair(0, -3),   // North
            Pair(0, 3),    // South
            Pair(3, 0),    // East
            Pair(-3, 0)    // West
        )
        
        for ((dx, dy) in islandOffsets) {
            val islandX = center + dx
            val islandY = center + dy
            
            // Create 2x2 island
            for (ix in 0..1) {
                for (iy in 0..1) {
                    val x = islandX + ix - 1  // Center the 2x2 block
                    val y = islandY + iy - 1
                    if (x >= 0 && x < size && y >= 0 && y < size) {
                        tiles["$x,$y"] = TileType.ISLAND
                    }
                }
            }
        }
        
        // All other tiles are PATH (except islands, spawn points, and target)
        for (x in 0 until size) {
            for (y in 0 until size) {
                val key = "$x,$y"
                if (!tiles.containsKey(key)) {
                    tiles[key] = TileType.PATH
                }
            }
        }
        
        return EditorMap(
            id = id,
            name = name,
            width = size,
            height = size,
            tiles = tiles,
            readyToUse = false
        )
    }
    
    /**
     * Create "The Dance" map with circular paths that make enemies dance around
     * Target in center, spawn points at edge centers, broken ring of buildable tiles at distance 4
     */
    fun createDanceMap(
        id: String = "map_dance",
        name: String = "The Dance",
        size: Int = 40
    ): EditorMap {
        val center = size / 2
        val tiles = mutableMapOf<String, TileType>()
        
        // Set spawn points at centers of all 4 edges
        tiles["$center,0"] = TileType.SPAWN_POINT          // Top
        tiles["$center,${size - 1}"] = TileType.SPAWN_POINT  // Bottom
        tiles["0,$center"] = TileType.SPAWN_POINT          // Left
        tiles["${size - 1},$center"] = TileType.SPAWN_POINT  // Right
        
        // Set target at center
        tiles["$center,$center"] = TileType.TARGET
        
        // Create broken ring of buildable tiles at distance 4 from center
        // Pattern: 3 BUILD_AREA, 3 PATH, repeating
        val ringPositions = mutableListOf<Position>()
        for (x in 0 until size) {
            for (y in 0 until size) {
                val pos = Position(x, y)
                val dist = pos.hexDistanceTo(Position(center, center))
                if (dist == 4) {
                    ringPositions.add(pos)
                }
            }
        }
        
        // Sort ring positions by angle to create a coherent ring pattern
        ringPositions.sortBy { pos ->
            kotlin.math.atan2((pos.y - center).toDouble(), (pos.x - center).toDouble())
        }
        
        // Apply alternating pattern: 3 BUILD_AREA, 3 PATH
        ringPositions.forEachIndexed { index, pos ->
            val patternIndex = index % 6
            tiles["${pos.x},${pos.y}"] = if (patternIndex < 3) {
                TileType.BUILD_AREA
            } else {
                TileType.PATH
            }
        }
        
        // Create circular path that guides enemies in a dancing pattern
        // The path will create concentric circular movements
        val dancePath = generateDancePath(size, center)
        dancePath.forEach { pos ->
            val key = "${pos.x},${pos.y}"
            if (!tiles.containsKey(key)) {
                tiles[key] = TileType.PATH
            }
        }
        
        // Fill remaining tiles with NO_PLAY to create the dance floor aesthetic
        for (x in 0 until size) {
            for (y in 0 until size) {
                val key = "$x,$y"
                if (!tiles.containsKey(key)) {
                    tiles[key] = TileType.NO_PLAY
                }
            }
        }
        
        return EditorMap(
            id = id,
            name = name,
            width = size,
            height = size,
            tiles = tiles,
            readyToUse = false
        )
    }
    
    /**
     * Generate a dancing path pattern with circular movements
     * Creates paths from edge spawn points that circle around before reaching center
     */
    private fun generateDancePath(size: Int, center: Int): Set<Position> {
        val path = mutableSetOf<Position>()
        val centerPos = Position(center, center)
        
        // Define spawn points at edge centers
        val spawnPoints = listOf(
            Position(center, 0),           // Top
            Position(center, size - 1),    // Bottom
            Position(0, center),           // Left
            Position(size - 1, center)     // Right
        )
        
        // Create circular path layers that connect spawns to center
        // Outer circle (distance ~18 from center)
        val outerRadius = 18
        val middleRadius = 10
        val innerRadius = 6
        
        // Add circular paths at different radii with extra width for connectivity
        for (radius in listOf(outerRadius, middleRadius, innerRadius)) {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val pos = Position(x, y)
                    val dist = pos.hexDistanceTo(centerPos)
                    // Create circular bands with extra width to ensure connectivity
                    if (dist >= radius - 2 && dist <= radius + 2) {
                        path.add(pos)
                    }
                }
            }
        }
        
        // Add radial connecting paths between all circles
        // Use 8 radial spokes at different angles for better connectivity
        for (angle in listOf(0, 45, 90, 135, 180, 225, 270, 315)) {
            val radians = angle * kotlin.math.PI / 180
            for (radius in 0..outerRadius) {
                val x = center + (radius * kotlin.math.cos(radians)).toInt()
                val y = center + (radius * kotlin.math.sin(radians)).toInt()
                if (x >= 0 && x < size && y >= 0 && y < size) {
                    path.add(Position(x, y))
                    // Add adjacent cells for better connectivity
                    val pos = Position(x, y)
                    pos.getHexNeighbors().forEach { neighbor ->
                        if (neighbor.x >= 0 && neighbor.x < size && neighbor.y >= 0 && neighbor.y < size) {
                            path.add(neighbor)
                        }
                    }
                }
            }
        }
        
        // Connect spawn points directly to the path network
        spawnPoints.forEach { spawn ->
            // Create straight path from spawn toward center
            val dx = if (spawn.x < center) 1 else if (spawn.x > center) -1 else 0
            val dy = if (spawn.y < center) 1 else if (spawn.y > center) -1 else 0
            
            var current = spawn
            for (step in 0..10) {
                path.add(current)
                // Add neighbors for width
                current.getHexNeighbors().forEach { neighbor ->
                    if (neighbor.x >= 0 && neighbor.x < size && neighbor.y >= 0 && neighbor.y < size) {
                        path.add(neighbor)
                    }
                }
                current = Position(current.x + dx, current.y + dy)
                if (current.x < 0 || current.x >= size || current.y < 0 || current.y >= size) break
            }
        }
        
        // Ensure center is connected
        path.add(centerPos)
        centerPos.getHexNeighbors().forEach { neighbor ->
            if (neighbor.x >= 0 && neighbor.x < size && neighbor.y >= 0 && neighbor.y < size) {
                path.add(neighbor)
            }
        }
        
        return path
    }
    
    /**
     * Generate a spiral path from corners toward center
     * Creates multiple spiral arms starting from each corner
     */
    private fun generateSpiralPath(size: Int, center: Int): Set<Position> {
        val path = mutableSetOf<Position>()
        
        // Create spiral paths from each corner
        val corners = listOf(
            Position(0, 0),
            Position(size - 1, 0),
            Position(0, size - 1),
            Position(size - 1, size - 1)
        )
        
        val centerPos = Position(center, center)
        
        // First, create direct paths from each corner to the center
        // This ensures all spawn points are connected
        corners.forEach { corner ->
            path.addAll(createPathBetween(corner, centerPos, size))
        }
        
        // Generate spiral decoration points that connect to the main paths
        // These add visual interest and provide alternative paths, but are not required for connectivity
        val spiralPoints = mutableListOf<Position>()
        
        // Start from outer edge and spiral inward
        var currentRadius = (size / 2) - 2
        
        // Create spiral layers moving inward - these will intersect with the guaranteed corner-to-center paths
        while (currentRadius > 0) {
            // Get points at this radius distance from center
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val pos = Position(x, y)
                    val dist = pos.hexDistanceTo(centerPos)
                    
                    // Add points at current radius with some spiral pattern
                    if (dist == currentRadius) {
                        // Use modulo to create spiral gaps (not all points at radius)
                        val angle = kotlin.math.atan2((y - center).toDouble(), (x - center).toDouble())
                        val angleDegrees = (angle * 180 / kotlin.math.PI).toInt()
                        
                        // Create spiral effect by only including certain angles
                        // This creates a spiral pattern that will naturally intersect with the guaranteed paths
                        if ((angleDegrees + currentRadius * 30) % 120 < 60) {
                            spiralPoints.add(pos)
                        }
                    }
                }
            }
            currentRadius -= 2
        }
        
        // Add spiral points to the path
        path.addAll(spiralPoints)
        
        // Ensure center is connected
        path.add(centerPos)
        
        // Add some width to the path for better gameplay
        val widenedPath = mutableSetOf<Position>()
        widenedPath.addAll(path)
        path.forEach { pos ->
            val neighbors = pos.getHexNeighbors()
            neighbors.filter { it.x >= 0 && it.x < size && it.y >= 0 && it.y < size }
                .take(2)  // Add 2 neighbors to widen the path
                .forEach { widenedPath.add(it) }
        }
        
        return widenedPath
    }

    /**
     * Create a simple path between two positions
     */
    private fun createPathBetween(
        start: Position,
        end: Position,
        size: Int
    ): List<Position> {
        val path = mutableListOf<Position>()
        var current = start
        path.add(current)
        
        while (current != end && path.size < size * 2) {  // Limit path length
            val neighbors = current.getHexNeighbors()
                .filter { it.x >= 0 && it.x < size && it.y >= 0 && it.y < size }
                .sortedBy { it.hexDistanceTo(end) }
            
            if (neighbors.isEmpty()) break
            
            current = neighbors.first()
            if (!path.contains(current)) {
                path.add(current)
            }
        }
        
        return path
    }
}
