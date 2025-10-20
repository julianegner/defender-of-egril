package com.defenderofegril.model

data class Level(
    val id: Int,
    val name: String,
    val gridWidth: Int = 30,
    val gridHeight: Int = 8,
    val startPositions: List<Position> = listOf(
        Position(0, gridHeight / 2 - 1),
        Position(0, gridHeight / 2),
        Position(0, gridHeight / 2 + 1)
    ),
    val targetPosition: Position = Position(gridWidth - 1, gridHeight / 2),
    val pathRows: Set<Int> = setOf(gridHeight / 2 - 1, gridHeight / 2, gridHeight / 2 + 1),
    val attackerWaves: List<AttackerWave>,
    val initialCoins: Int = 100,
    val healthPoints: Int = 10
) {
    fun isOnPath(position: Position): Boolean {
        return pathRows.contains(position.y)
    }
    
    fun isBuildArea(position: Position): Boolean {
        return !isOnPath(position) && position.x >= 0 && position.x < gridWidth && 
               position.y >= 0 && position.y < gridHeight
    }
    
    fun isSpawnPoint(position: Position): Boolean {
        return startPositions.contains(position)
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
