package com.defenderofegril.model

data class Level(
    val id: Int,
    val name: String,
    val gridWidth: Int = 30,
    val gridHeight: Int = 8,
    val startPositions: List<Position> = listOf(
        Position(0, 1),
        Position(0, 4),
        Position(0, 7)
    ),
    val targetPosition: Position = Position(gridWidth - 1, gridHeight / 2),
    val pathCells: Set<Position>,
    val buildIslands: Set<Position>,
    val attackerWaves: List<AttackerWave>,
    val initialCoins: Int = 100,
    val healthPoints: Int = 10
) {
    fun isOnPath(position: Position): Boolean {
        return pathCells.contains(position)
    }
    
    fun isBuildIsland(position: Position): Boolean {
        return buildIslands.contains(position)
    }
    
    fun isBuildArea(position: Position): Boolean {
        // Build areas are islands OR strips adjacent to paths
        // Cannot build on path itself, spawn points, or target
        if (isSpawnPoint(position) || position == targetPosition) return false
        if (isOnPath(position)) return false
        
        // Can build on islands
        if (isBuildIsland(position)) return true
        
        // Can also build on strips adjacent to paths
        return isAdjacentToPath(position)
    }
    
    private fun isAdjacentToPath(position: Position): Boolean {
        // Check if any adjacent cell is a path cell
        val adjacentPositions = listOf(
            Position(position.x - 1, position.y),
            Position(position.x + 1, position.y),
            Position(position.x, position.y - 1),
            Position(position.x, position.y + 1)
        )
        return adjacentPositions.any { pos ->
            pos.x in 0 until gridWidth && 
            pos.y in 0 until gridHeight && 
            isOnPath(pos)
        }
    }
    
    fun isSpawnPoint(position: Position): Boolean {
        return startPositions.contains(position)
    }
    
    companion object {
        data class PathAndIslands(
            val pathCells: Set<Position>,
            val buildIslands: Set<Position>
        )
        
        fun generateCurvedPathWithIslands(width: Int, height: Int): PathAndIslands {
            val islands = mutableSetOf<Position>()
            
            // Create build islands at strategic points
            // Islands are 2x2 blocks that the path will curve around
            val islandPositions = listOf(
                Pair(6, 1),   // Top path island
                Pair(10, 4),  // Middle path island  
                Pair(15, 2),  // Between top and middle
                Pair(18, 6),  // Bottom path island
                Pair(22, 4),  // Middle path island
                Pair(25, 1)   // Top path island
            )
            
            for ((x, y) in islandPositions) {
                // Create 2x2 island
                islands.add(Position(x, y))
                islands.add(Position(x + 1, y))
                islands.add(Position(x, y + 1))
                islands.add(Position(x + 1, y + 1))
            }
            
            // Generate path that curves around islands
            val path = mutableSetOf<Position>()
            
            // Create 3 curved paths from left to right that avoid islands
            // Top path (y = 1-2)
            for (x in 0 until width) {
                val baseY = 1
                var y = baseY
                
                // Check if we need to curve around an island
                val pos = Position(x, y)
                if (islands.contains(pos)) {
                    // Try going above or below
                    if (!islands.contains(Position(x, y - 1)) && y > 0) {
                        y = y - 1
                    } else if (!islands.contains(Position(x, y + 1)) && y < height - 1) {
                        y = y + 1
                    }
                }
                
                if (!islands.contains(Position(x, y))) {
                    path.add(Position(x, y))
                }
            }
            
            // Middle path (y = 4)
            for (x in 0 until width) {
                val baseY = 4
                var y = baseY
                
                val pos = Position(x, y)
                if (islands.contains(pos)) {
                    // Try going above or below
                    if (!islands.contains(Position(x, y - 1)) && y > 0) {
                        y = y - 1
                    } else if (!islands.contains(Position(x, y + 1)) && y < height - 1) {
                        y = y + 1
                    }
                }
                
                if (!islands.contains(Position(x, y))) {
                    path.add(Position(x, y))
                }
            }
            
            // Bottom path (y = 6-7)
            for (x in 0 until width) {
                val baseY = 7
                var y = baseY
                
                val pos = Position(x, y)
                if (islands.contains(pos)) {
                    // Try going above or below
                    if (!islands.contains(Position(x, y - 1)) && y > 0) {
                        y = y - 1
                    } else if (!islands.contains(Position(x, y + 1)) && y < height - 1) {
                        y = y + 1
                    }
                }
                
                if (!islands.contains(Position(x, y))) {
                    path.add(Position(x, y))
                }
            }
            
            return PathAndIslands(path, islands)
        }
    }
}

data class AttackerWave(
    val attackers: List<AttackerType>,
    val spawnDelay: Int = 2 // turns between spawns
)

enum class LevelStatus {
    LOCKED,
    UNLOCKED,
    WON
}

data class WorldLevel(
    val level: Level,
    var status: LevelStatus = LevelStatus.LOCKED
)
