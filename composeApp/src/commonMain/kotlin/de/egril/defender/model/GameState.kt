package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

enum class GamePhase {
    INITIAL_BUILDING,  // Initial building phase - towers build instantly
    PLAYER_TURN,       // Player can place/upgrade towers and attack
    ENEMY_TURN         // Enemies move
}

enum class FieldEffectType {
    FIREBALL,      // Visual effect for wizard fireball area
    ACID           // Visual effect for alchemy acid with duration
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
    val phase: MutableState<GamePhase> = mutableStateOf(GamePhase.INITIAL_BUILDING),
    val coins: MutableState<Int> = mutableStateOf(level.initialCoins),
    val healthPoints: MutableState<Int> = mutableStateOf(level.healthPoints),
    val defenders: SnapshotStateList<Defender> = mutableStateListOf(),
    val attackers: SnapshotStateList<Attacker> = mutableStateListOf(),
    val nextDefenderId: MutableState<Int> = mutableStateOf(1),
    val nextAttackerId: MutableState<Int> = mutableStateOf(1),
    val currentWaveIndex: MutableState<Int> = mutableStateOf(0),
    val spawnCounter: MutableState<Int> = mutableStateOf(0),
    val attackersToSpawn: SnapshotStateList<AttackerType> = mutableStateListOf(),
    val turnNumber: MutableState<Int> = mutableStateOf(0),
    val actionsRemainingThisTurn: MutableState<Int> = mutableStateOf(0),
    val spawnPlan: List<PlannedEnemySpawn> = level.directSpawnPlan ?: generateSpawnPlan(level.attackerWaves),
    val fieldEffects: SnapshotStateList<FieldEffect> = mutableStateListOf(), // Track active field effects
    val traps: SnapshotStateList<Trap> = mutableStateListOf(),  // Track active traps
    val tutorialState: MutableState<TutorialState> = mutableStateOf(
        // Enable tutorial only for the tutorial level (id=1, title contains "Welcome")
        if (level.id == 1 && level.name.contains("Welcome", ignoreCase = true)) {
            TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
        } else {
            TutorialState(isActive = false, currentStep = TutorialStep.NONE)
        }
    ),
    val hasSeenDragonInfo: MutableState<Boolean> = mutableStateOf(false)  // Track if dragon tutorial has been shown
) {
    fun isLevelWon(): Boolean {
        // Check if all planned spawns have occurred and all enemies are defeated
        val allSpawned = spawnPlan.all { it.spawnTurn <= turnNumber.value }
        return allSpawned && attackers.all { it.isDefeated.value }
    }
    
    fun isLevelLost(): Boolean {
        return healthPoints.value <= 0
    }
    
    fun canPlaceDefender(type: DefenderType): Boolean {
        return coins.value >= type.baseCost && level.availableTowers.contains(type)
    }
    
    fun canUpgradeDefender(defender: Defender): Boolean {
        return coins.value >= defender.upgradeCost
    }
    
    fun hasActionsRemaining(): Boolean {
        return actionsRemainingThisTurn.value > 0
    }

    fun getRemainingEnemyCount(): Int {
        val totalSpawned = this.nextAttackerId.value - 1
        val plannedSpawns = this.spawnPlan.drop(totalSpawned)
        return plannedSpawns.size
    }

    fun getActiveEnemyCount(): Int {
        return this.attackers.count { !it.isDefeated.value }
    }
}
