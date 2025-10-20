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
    val pathCells: Set<Position> = generateCurvedPath(gridWidth, gridHeight),
    val attackerWaves: List<AttackerWave>,
    val initialCoins: Int = 100,
    val healthPoints: Int = 10
) {
    fun isOnPath(position: Position): Boolean {
        return pathCells.contains(position)
    }
    
    fun isBuildArea(position: Position): Boolean {
        // Build areas are cells that are on the path but not occupied by spawn/target
        // and also cells off the path
        return position.x >= 0 && position.x < gridWidth && 
               position.y >= 0 && position.y < gridHeight &&
               !isSpawnPoint(position) && position != targetPosition
    }
    
    fun isSpawnPoint(position: Position): Boolean {
        return startPositions.contains(position)
    }
    
    companion object {
        fun generateCurvedPath(width: Int, height: Int): Set<Position> {
            val path = mutableSetOf<Position>()
            
            // Create 3 curved paths from left to right
            // Top path (y = 0-2)
            for (x in 0 until width) {
                val yOffset = when {
                    x < width / 3 -> 0  // Straight at top
                    x < 2 * width / 3 -> 1  // Curve down
                    else -> 0  // Back to middle-top
                }
                path.add(Position(x, 1 + yOffset))
            }
            
            // Middle path (y = 3-4)
            for (x in 0 until width) {
                path.add(Position(x, 4))
            }
            
            // Bottom path (y = 5-7)
            for (x in 0 until width) {
                val yOffset = when {
                    x < width / 3 -> 0  // Straight at bottom
                    x < 2 * width / 3 -> -1  // Curve up
                    else -> 0  // Back to middle-bottom
                }
                path.add(Position(x, 7 + yOffset))
            }
            
            return path
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
