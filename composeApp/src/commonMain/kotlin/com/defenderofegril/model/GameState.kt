package com.defenderofegril.model

enum class GamePhase {
    INITIAL_BUILDING,  // Initial building phase - towers build instantly
    PLAYER_TURN,       // Player can place/upgrade towers and attack
    ENEMY_TURN         // Enemies move
}

enum class FieldEffectType {
    FIREBALL_AOE,      // Visual effect for wizard fireball area
    ACID_DOT           // Visual effect for alchemy acid with duration
}

data class FieldEffect(
    val position: Position,
    val type: FieldEffectType,
    val damage: Int,
    var turnsRemaining: Int,
    val defenderId: Int,  // Track which tower created this effect
    val attackerId: Int? = null  // For DOT effects, track which enemy has the effect
)

data class GameState(
    val level: Level,
    var phase: GamePhase = GamePhase.INITIAL_BUILDING,
    var coins: Int = level.initialCoins,
    var healthPoints: Int = level.healthPoints,
    val defenders: MutableList<Defender> = mutableListOf(),
    val attackers: MutableList<Attacker> = mutableListOf(),
    var nextDefenderId: Int = 1,
    var nextAttackerId: Int = 1,
    var currentWaveIndex: Int = 0,
    var spawnCounter: Int = 0,
    var attackersToSpawn: MutableList<AttackerType> = mutableListOf(),
    var turnNumber: Int = 0,
    var actionsRemainingThisTurn: Int = 0,
    val spawnPlan: List<PlannedEnemySpawn> = generateSpawnPlan(level.attackerWaves),
    val fieldEffects: MutableList<FieldEffect> = mutableListOf()  // Track active field effects
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
    
    fun hasActionsRemaining(): Boolean {
        return actionsRemainingThisTurn > 0
    }
}
