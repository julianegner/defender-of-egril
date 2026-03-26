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

/**
 * Spell targeting mode state
 */
data class SpellTargetingState(
    val activeSpell: SpellType,
    val validTargets: Set<Any> = emptySet()  // Can be Position, Attacker, or Defender depending on spell type
)

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

data class BombExplosionEffect(
    val center: Position,        // Center of the explosion
    val affectedPositions: List<Position>,  // All affected tile positions
    val turnNumber: Int          // Turn when this explosion occurred
)

/**
 * Types of in-game event messages that are shown to the player.
 */
enum class GameMessageType {
    TARGET_TAKEN,      // A SINGLE_HIT target was captured by an enemy
    GATE_DESTROYED,    // A named gate barricade was destroyed
    EWHAD_ENTERS,      // Ewhad has entered the battlefield
    EWHAD_RETREATS,    // Ewhad has retreated (health reached 0, not final stand)
    EWHAD_DEFEATED     // Ewhad is defeated (health reached 0, final stand level)
}

/**
 * An in-game event message queued for display to the player.
 * @param type   The kind of event.
 * @param name   Optional name (target name or gate name).
 */
data class GameMessage(
    val type: GameMessageType,
    val name: String? = null
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
    val nextBarricadeId: MutableState<Int> = mutableStateOf(1),
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
    val bombExplosionEffects: SnapshotStateList<BombExplosionEffect> = mutableStateListOf(),  // Track bomb explosion visual effects
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
    val mineWarnings: SnapshotStateList<Int> = mutableStateListOf(),  // Mine IDs with active warnings (dragon about to destroy)
    val xpEarnedThisLevel: MutableState<Int> = mutableStateOf(0),  // XP earned during this level (awarded on win)
    val currentMana: MutableState<Int> = mutableStateOf(0),  // Current mana (for spellcasting)
    val maxMana: MutableState<Int> = mutableStateOf(0),  // Maximum mana (based on player stats)
    val activeSpellEffects: SnapshotStateList<ActiveSpellEffect> = mutableStateListOf(),  // Active spell effects
    val incomeMultiplier: Double = 1.0,  // Income multiplier from player stats (default 1.0, e.g. 1.2 for 20% bonus)
    val constructionLevel: Int = 0,  // Construction level from player stats (0-3+, gates tower abilities)
    val spellTargeting: MutableState<SpellTargetingState?> = mutableStateOf(null),  // Active spell targeting state (null when not targeting)
    val instantTowerSpellActive: MutableState<Boolean> = mutableStateOf(false),  // True when Instant Tower spell is active (waiting for next tower placement)
    // SINGLE_HIT target tracking
    val takenTargets: SnapshotStateList<Position> = mutableStateListOf(),  // Positions of taken SINGLE_HIT targets
    val pendingMessages: SnapshotStateList<GameMessage> = mutableStateListOf()  // Messages queued for display
) {
    fun isLevelWon(): Boolean {
        // Check if all planned spawns have occurred and all enemies are defeated
        val allSpawned = spawnPlan.all { it.spawnTurn <= turnNumber.value }
        return allSpawned && attackers.all { it.isDefeated.value }
    }
    
    fun isLevelLost(): Boolean {
        if (healthPoints.value <= 0) return true
        // Level is also lost when all SINGLE_HIT targets have been taken
        val singleHitTargets = level.targetInfoMap.filter { it.value.type == TargetType.SINGLE_HIT }.keys
        if (singleHitTargets.isNotEmpty() && takenTargets.containsAll(singleHitTargets)) return true
        return false
    }

    /**
     * Returns true if [position] is a target that can still be reached by enemies.
     * Taken SINGLE_HIT targets are excluded.
     */
    fun isActiveTargetPosition(position: Position): Boolean {
        return level.isTargetPosition(position) && !takenTargets.contains(position)
    }

    /**
     * Returns the active (non-taken) target positions.
     */
    fun getActiveTargetPositions(): List<Position> {
        return level.targetPositions.filter { !takenTargets.contains(it) }
    }

    /**
     * When a SINGLE_HIT target at [takenPosition] is taken, redirect all enemies
     * whose currentTarget points to that position towards the nearest remaining active target.
     */
    fun retargetEnemiesFromTakenTarget(takenPosition: Position) {
        val remaining = getActiveTargetPositions()
        if (remaining.isEmpty()) return  // No active targets left – level will be lost
        for (enemy in attackers) {
            if (enemy.isDefeated.value) continue
            if (enemy.currentTarget?.value == takenPosition) {
                val newTarget = remaining.minByOrNull { enemy.position.value.distanceTo(it) } ?: remaining.first()
                enemy.currentTarget.value = newTarget
                println("Enemy ${enemy.id} (${enemy.type}) retargeted from $takenPosition to $newTarget")
            }
        }
    }

    /**
     * Returns the effective next waypoint target, redirecting to the nearest active target
     * if the waypoint's next target is a taken SINGLE_HIT target.
     */
    fun resolveWaypointNextTarget(waypointNextTarget: Position, from: Position): Position {
        return if (takenTargets.contains(waypointNextTarget)) {
            getActiveTargetPositions().minByOrNull { from.distanceTo(it) } ?: waypointNextTarget
        } else {
            waypointNextTarget
        }
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
        // Get initial data using the helper method that handles both old and new formats
        val initialData = level.getEffectiveInitialData()
        
        // Place initial barricades FIRST (before defenders so we can link them)
        for (initialBarricade in initialData.barricades) {
            val barricade = Barricade(
                id = nextBarricadeId.value++,
                position = initialBarricade.position,
                healthPoints = mutableStateOf(initialBarricade.healthPoints),
                defenderId = 0,  // Pre-placed barricades don't belong to any specific defender
                isGate = initialBarricade.isGate,
                name = initialBarricade.name
            )
            barricades.add(barricade)
        }
        
        // Place initial defenders
        for (initialDefender in initialData.defenders) {
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
            
            // If this defender should be on a tower base, find the barricade at the same position
            if (initialDefender.onTowerBase) {
                val barricadeAtPosition = barricades.find { it.position == initialDefender.position }
                if (barricadeAtPosition != null && barricadeAtPosition.canSupportTower()) {
                    defender.towerBaseBarricadeId.value = barricadeAtPosition.id
                    barricadeAtPosition.supportedTowerId.value = defender.id
                }
            }
            
            defenders.add(defender)
            nextDefenderId.value++
        }
        
        // Place initial attackers
        for (initialAttacker in initialData.attackers) {
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
        for (initialTrap in initialData.traps) {
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
    }
}
