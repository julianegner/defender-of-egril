package com.defenderofegril.model

enum class GamePhase {
    PLANNING,  // Player can place/upgrade towers
    COMBAT     // Attackers move and towers attack
}

data class GameState(
    val level: Level,
    var phase: GamePhase = GamePhase.PLANNING,
    var coins: Int = level.initialCoins,
    var healthPoints: Int = level.healthPoints,
    val defenders: MutableList<Defender> = mutableListOf(),
    val attackers: MutableList<Attacker> = mutableListOf(),
    var nextDefenderId: Int = 1,
    var nextAttackerId: Int = 1,
    var currentWaveIndex: Int = 0,
    var spawnCounter: Int = 0,
    var attackersToSpawn: MutableList<AttackerType> = mutableListOf(),
    var turnNumber: Int = 0
) {
    fun isLevelWon(): Boolean {
        return currentWaveIndex >= level.attackerWaves.size &&
               attackersToSpawn.isEmpty() &&
               attackers.all { it.isDefeated }
    }
    
    fun isLevelLost(): Boolean {
        return healthPoints <= 0
    }
    
    fun canPlaceDefender(type: DefenderType): Boolean {
        return coins >= type.baseCost
    }
    
    fun canUpgradeDefender(defender: Defender): Boolean {
        return coins >= defender.upgradeCost
    }
}
