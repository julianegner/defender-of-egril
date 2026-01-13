package de.egril.defender.model

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import de.egril.defender.ui.settings.DifficultyLevel

enum class GamePhase {
    INITIAL_BUILDING,  // Initial building phase - towers build instantly
    PLAYER_TURN,       // Player can place/upgrade towers and attack
    ENEMY_TURN         // Enemies move
}

enum class FieldEffectType {
    FIREBALL,      // Visual effect for wizard fireball area
    ACID           // Visual effect for alchemy acid with duration
}

enum class HealingEffectType {
    GREEN_WITCH    // Visual effect for green witch healing
}

data class FieldEffect(
    val position: Position,
    val type: FieldEffectType,
    val damage: Int,
    var turnsRemaining: Int,
    val defenderId: Int,  // Track which tower created this effect
    val attackerId: Int? = null  // For DOT effects, track which enemy has the effect
)

data class HealingEffect(
    val position: Position,
    val type: HealingEffectType,
    val healAmount: Int,
    val turnNumber: Int  // Track which turn this healing occurred for display timing
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
    val nextRaftId: MutableState<Int> = mutableStateOf(1),
    val currentWaveIndex: MutableState<Int> = mutableStateOf(0),
    val spawnCounter: MutableState<Int> = mutableStateOf(0),
    val attackersToSpawn: SnapshotStateList<AttackerType> = mutableStateListOf(),
    val turnNumber: MutableState<Int> = mutableStateOf(0),
    val actionsRemainingThisTurn: MutableState<Int> = mutableStateOf(0),
    val spawnPlan: List<PlannedEnemySpawn> = level.directSpawnPlan ?: generateSpawnPlan(level.attackerWaves),
    val fieldEffects: SnapshotStateList<FieldEffect> = mutableStateListOf(), // Track active field effects
    val healingEffects: SnapshotStateList<HealingEffect> = mutableStateListOf(), // Track active healing effects
    val traps: SnapshotStateList<Trap> = mutableStateListOf(),  // Track active traps
    val bridges: SnapshotStateList<Bridge> = mutableStateListOf(),  // Track active bridges
    val rafts: SnapshotStateList<Raft> = mutableStateListOf(),  // Track active rafts (towers on rivers)
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,  // Track difficulty for this game session
    val tutorialState: MutableState<TutorialState> = mutableStateOf(
        // Enable tutorial only for the tutorial level (id=1, title contains "Welcome")
        if (level.id == 1 && level.name.contains("Welcome", ignoreCase = true)) {
            TutorialState(isActive = true, currentStep = TutorialStep.WELCOME)
        } else {
            TutorialState(isActive = false, currentStep = TutorialStep.NONE)
        }
    ),
    val infoState: MutableState<InfoState> = mutableStateOf(InfoState()),  // Single tutorial infos system
    val destroyedMinePositions: SnapshotStateList<Position> = mutableStateListOf(),  // Positions where mines have been destroyed
    val mineWarnings: SnapshotStateList<Int> = mutableStateListOf()  // Mine IDs with active warnings (dragon about to destroy)
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
        // Count only non-defeated enemies that are NOT building bridges
        return this.attackers.count { !it.isDefeated.value && !it.isBuildingBridge.value }
    }
    
    /**
     * Check if a position is covered by any active bridge
     */
    fun isBridgeAt(position: Position): Boolean {
        return bridges.any { bridge ->
            bridge.isActive && bridge.coversPosition(position)
        }
    }
    
    /**
     * Get the bridge at a position, if any
     */
    fun getBridgeAt(position: Position): Bridge? {
        return bridges.find { bridge ->
            bridge.isActive && bridge.coversPosition(position)
        }
    }
    
    /**
     * Check if a position has a raft
     */
    fun isRaftAt(position: Position): Boolean {
        return rafts.any { raft ->
            raft.isActive && raft.currentPosition.value == position
        }
    }
    
    /**
     * Get the raft at a position, if any
     */
    fun getRaftAt(position: Position): Raft? {
        return rafts.find { raft ->
            raft.isActive && raft.currentPosition.value == position
        }
    }
    
    /**
     * Check if there are defenders with unused action points
     * Used to show end turn confirmation dialog
     */
    fun hasDefendersWithUnusedActions(): Boolean {
        return defenders.any { defender ->
            defender.isReady && 
            defender.actionsRemaining.value > 0 && 
            !defender.isDisabled.value && 
            (defender.type.attackType != AttackType.NONE || defender.type == DefenderType.DWARVEN_MINE)
        }
    }
}
