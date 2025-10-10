package com.defenderofegril.model

data class Level(
    val id: Int,
    val name: String,
    val gridWidth: Int = 10,
    val gridHeight: Int = 8,
    val startPosition: Position = Position(0, gridHeight / 2),
    val targetPosition: Position = Position(gridWidth - 1, gridHeight / 2),
    val attackerWaves: List<AttackerWave>,
    val initialCoins: Int = 100,
    val healthPoints: Int = 10
)

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
