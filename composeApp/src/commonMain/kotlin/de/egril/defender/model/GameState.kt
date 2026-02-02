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

data class DamageEffect(
    val position: Position,
    val damageAmount: Int,
    val turnNumber: Int  // Track which turn this damage occurred for display timing
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
    val damageEffects: SnapshotStateList<DamageEffect> = mutableStateListOf(), // Track barricade damage effects
    val traps: SnapshotStateList<Trap> = mutableStateListOf(),  // Track active traps
    val barricades: SnapshotStateList<Barricade> = mutableStateListOf(),  // Track active barricades
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
     * Check if there are defenders with unused action points and enemies in range
     * Used to show end turn confirmation dialog
     */
    fun hasDefendersWithUnusedActions(): Boolean {
        // Get active attackers (not defeated, not building bridges)
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        
        return defenders.any { defender ->
            if (!defender.isReady || 
                defender.actionsRemaining.value <= 0 || 
                defender.isDisabled.value) {
                return@any false
            }
            
            // Special handling for different tower types
            when (defender.type) {
                DefenderType.DWARVEN_MINE -> {
                    // Mines always count as having unused actions (digging)
                    true
                }
                else -> {
                    // Only count attack towers if they have AttackType and enemies in range
                    if (defender.type.attackType == AttackType.NONE) {
                        false
                    } else {
                        // Check if there are any enemies in range
                        activeAttackers.any { attacker -> defender.canAttack(attacker) }
                    }
                }
            }
        }
    }
    
    /**
     * Check if there are defenders that can perform auto-attacks.
     * Returns true if there are defenders with actions that can be automated (regular attacks).
     * Excludes special actions like mines, traps, and alchemy towers.
     */
    fun hasDefendersForAutoAttack(): Boolean {
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        if (activeAttackers.isEmpty()) return false
        
        return defenders.any { defender ->
            if (!defender.isReady || 
                defender.actionsRemaining.value <= 0 || 
                defender.isDisabled.value) {
                return@any false
            }
            
            // Only count towers that can do regular auto-attacks
            // Exclude mines (no attack) and wizard towers level 10+ (have trap ability that needs manual placement)
            // Alchemy towers CAN auto-attack (they check acid immunity like wizard towers check fireball immunity)
            when {
                defender.type == DefenderType.DWARVEN_MINE -> false
                defender.type == DefenderType.WIZARD_TOWER && defender.level.value >= 10 -> false
                defender.type.attackType == AttackType.NONE -> false
                else -> {
                    // Check if there are any enemies in range
                    activeAttackers.any { attacker -> defender.canAttack(attacker) }
                }
            }
        }
    }
    
    /**
     * Check if there are defenders with special actions that cannot be automated effectively.
     * Returns a list of defender types that have remaining special actions.
     */
    fun getDefenderTypesWithSpecialActions(): List<DefenderType> {
        val typesWithActions = mutableSetOf<DefenderType>()
        val activeAttackers = attackers.filter { !it.isDefeated.value && !it.isBuildingBridge.value }
        
        defenders.forEach { defender ->
            if (!defender.isReady || defender.actionsRemaining.value <= 0 || defender.isDisabled.value) {
                return@forEach
            }
            
            when {
                // Dwarven mines with digging actions
                defender.type == DefenderType.DWARVEN_MINE -> {
                    typesWithActions.add(DefenderType.DWARVEN_MINE)
                }
                // Alchemy towers with lasting attacks only when no enemies in range
                // (if enemies are in range, they will auto-attack like normal towers)
                defender.type == DefenderType.ALCHEMY_TOWER -> {
                    val hasEnemiesInRange = activeAttackers.any { attacker -> defender.canAttack(attacker) }
                    if (!hasEnemiesInRange) {
                        typesWithActions.add(DefenderType.ALCHEMY_TOWER)
                    }
                }
                // Wizard towers (level 10+) with magical trap available
                defender.type == DefenderType.WIZARD_TOWER && defender.level.value >= 10 -> {
                    if (defender.trapCooldownRemaining.value == 0) {
                        typesWithActions.add(DefenderType.WIZARD_TOWER)
                    }
                }
            }
        }
        
        return typesWithActions.toList()
    }
    
    /**
     * Initialize pre-placed defenders, attackers, traps, and barricades from level configuration.
     * This should be called right after GameState creation to set up the initial level state.
     */
    fun initializePrePlacedElements() {
        // Place initial defenders
        for (initialDefender in level.initialDefenders) {
            val defender = Defender(
                id = nextDefenderId.value,
                type = initialDefender.type,
                position = mutableStateOf(initialDefender.position),
                placedOnTurn = 0,  // Placed before the game starts
                dragonName = initialDefender.dragonName
            )
            defender.level.value = initialDefender.level
            defender.buildTimeRemaining.value = 0  // Already built
            defender.actionsRemaining.value = 0  // No actions in initial phase
            defenders.add(defender)
            nextDefenderId.value++
        }
        
        // Place initial attackers
        for (initialAttacker in level.initialAttackers) {
            val attacker = Attacker(
                id = nextAttackerId.value,
                type = initialAttacker.type,
                position = mutableStateOf(initialAttacker.position),
                level = mutableStateOf(initialAttacker.level),
                dragonName = initialAttacker.dragonName
            )
            // Set custom health if specified, otherwise use default for level
            val health = initialAttacker.currentHealth ?: (initialAttacker.type.health * initialAttacker.level)
            attacker.currentHealth.value = health
            attacker.isDefeated.value = false
            attackers.add(attacker)
            nextAttackerId.value++
        }
        
        // Place initial traps
        for (initialTrap in level.initialTraps) {
            val trapType = try {
                TrapType.valueOf(initialTrap.type)
            } catch (e: Exception) {
                TrapType.DWARVEN
            }
            // We need a defender ID for the trap, but there may not be one
            // Use defenderId = 0 to indicate it's a pre-placed trap
            val trap = Trap(
                position = initialTrap.position,
                damage = initialTrap.damage,
                defenderId = 0,  // Pre-placed traps don't belong to any specific defender
                type = trapType
            )
            traps.add(trap)
        }
        
        // Place initial barricades
        for (initialBarricade in level.initialBarricades) {
            val barricade = Barricade(
                position = initialBarricade.position,
                healthPoints = mutableStateOf(initialBarricade.healthPoints),
                defenderId = 0  // Pre-placed barricades don't belong to any specific defender
            )
            barricades.add(barricade)
        }
    }
}
