package com.defenderofegril.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

enum class GamePhase {
    INITIAL_BUILDING,  // Initial building phase - towers build instantly
    PLAYER_TURN,       // Player can place/upgrade towers and attack
    ENEMY_TURN         // Enemies move
}

data class GameState(
    val level: Level,
    val phase: MutableState<GamePhase> = mutableStateOf(GamePhase.INITIAL_BUILDING),
    val coins: MutableState<Int> = mutableStateOf(level.initialCoins),
    val healthPoints: MutableState<Int> = mutableStateOf(level.healthPoints),
    val defenders: MutableList<Defender> = mutableListOf(),
    val attackers: MutableList<Attacker> = mutableListOf(),
    val nextDefenderId: MutableState<Int> = mutableStateOf(1),
    val nextAttackerId: MutableState<Int> = mutableStateOf(1),
    val currentWaveIndex: MutableState<Int> = mutableStateOf(0),
    val spawnCounter: MutableState<Int> = mutableStateOf(0),
    val attackersToSpawn: MutableList<AttackerType> = mutableListOf(),
    val turnNumber: MutableState<Int> = mutableStateOf(0),
    val actionsRemainingThisTurn: MutableState<Int> = mutableStateOf(0),
    val spawnPlan: List<PlannedEnemySpawn> = generateSpawnPlan(level.attackerWaves)
) {
    fun isLevelWon(): Boolean {
        return currentWaveIndex.value >= level.attackerWaves.size &&
               attackersToSpawn.isEmpty() &&
               attackers.all { it.isDefeated }
    }
    
    fun isLevelLost(): Boolean {
        return healthPoints.value <= 0
    }
    
    fun canPlaceDefender(type: DefenderType): Boolean {
        return coins.value >= type.baseCost
    }
    
    fun canUpgradeDefender(defender: Defender): Boolean {
        return coins.value >= defender.upgradeCost
    }
    
    fun hasActionsRemaining(): Boolean {
        return actionsRemainingThisTurn.value > 0
    }
}
