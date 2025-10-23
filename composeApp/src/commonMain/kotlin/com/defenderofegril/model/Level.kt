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
        // Check if any adjacent cell (using hexagonal neighbors) is a path cell
        val adjacentPositions = position.getHexNeighbors()
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
                Pair(8, 3),   // Left middle island
                Pair(12, 2),  // Upper middle island  
                Pair(16, 5),  // Lower middle island
                Pair(20, 3),  // Right middle island
                Pair(24, 4)   // Far right island
            )
            
            for ((x, y) in islandPositions) {
                // Create 2x2 island
                islands.add(Position(x, y))
                islands.add(Position(x + 1, y))
                islands.add(Position(x, y + 1))
                islands.add(Position(x + 1, y + 1))
            }
            
            // Generate SINGLE unified path that all enemies use
            // Path starts wide at spawn points and converges to single lane
            val path = mutableSetOf<Position>()
            
            for (x in 0 until width) {
                // Starting area: 3 lanes (y=1, 4, 7) for the 3 spawn points
                if (x < 5) {
                    // Wide starting area
                    for (y in listOf(1, 2, 3, 4, 5, 6, 7)) {
                        if (!islands.contains(Position(x, y))) {
                            path.add(Position(x, y))
                        }
                    }
                }
                // Converging area: gradually narrow to single lane
                else if (x < 10) {
                    // Narrowing from 3 lanes to 2
                    for (y in listOf(2, 3, 4, 5, 6)) {
                        if (!islands.contains(Position(x, y))) {
                            path.add(Position(x, y))
                        }
                    }
                }
                // Single lane with curves around islands
                else {
                    // Main single path (y=4 as base) that curves around islands
                    var y = 4
                    
                    // Check if island blocks the path
                    if (islands.contains(Position(x, y))) {
                        // Try alternative routes
                        if (!islands.contains(Position(x, y - 1))) {
                            y = y - 1
                        } else if (!islands.contains(Position(x, y + 1))) {
                            y = y + 1
                        } else if (!islands.contains(Position(x, y - 2))) {
                            y = y - 2
                        } else if (!islands.contains(Position(x, y + 2))) {
                            y = y + 2
                        }
                    }
                    
                    // Add path cell and adjacent cells for wider passage
                    if (!islands.contains(Position(x, y))) {
                        path.add(Position(x, y))
                        // Add some width to the path
                        if (y > 0 && !islands.contains(Position(x, y - 1))) {
                            path.add(Position(x, y - 1))
                        }
                        if (y < height - 1 && !islands.contains(Position(x, y + 1))) {
                            path.add(Position(x, y + 1))
                        }
                    }
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

/**
 * Represents a planned enemy spawn with the turn it will spawn
 */
data class PlannedEnemySpawn(
    val attackerType: AttackerType,
    val spawnTurn: Int
)

/**
 * Generate a spawn plan for all waves in a level
 * This calculates when each enemy will spawn based on wave delays
 */
fun generateSpawnPlan(waves: List<AttackerWave>): List<PlannedEnemySpawn> {
    val plan = mutableListOf<PlannedEnemySpawn>()
    var currentTurn = 1  // First enemies spawn at turn 1
    
    for (wave in waves) {
        for ((index, attackerType) in wave.attackers.withIndex()) {
            // First 3 enemies of first wave spawn immediately at turn 1
            val spawnTurn = if (currentTurn == 1 && index < 3) {
                1
            } else {
                currentTurn + (if (currentTurn == 1 && index < 3) 0 else wave.spawnDelay * (index - if (currentTurn == 1) 3 else 0))
            }
            plan.add(PlannedEnemySpawn(attackerType, spawnTurn))
        }
        // Move to next wave - add delay after last enemy of current wave
        if (wave.attackers.isNotEmpty()) {
            val lastIndex = wave.attackers.size - 1
            val lastEnemyTurn = if (currentTurn == 1 && lastIndex < 3) 1 else currentTurn + wave.spawnDelay * (lastIndex - if (currentTurn == 1) 3 else 0)
            currentTurn = lastEnemyTurn + wave.spawnDelay + 2  // Gap between waves
        }
    }
    
    return plan
}

enum class LevelStatus {
    LOCKED,
    UNLOCKED,
    WON
}

data class WorldLevel(
    val level: Level,
    var status: LevelStatus = LevelStatus.LOCKED
)
