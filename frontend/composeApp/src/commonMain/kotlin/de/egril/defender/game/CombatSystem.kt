package de.egril.defender.game

import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*

/**
 * Result of a combat action
 */
data class CombatResult(
    val killsThisAttack: Int = 0,
    val killedEnemyTypes: List<AttackerType> = emptyList()
)

/**
 * Handles combat mechanics including single-target, area, and lasting attacks.
 */
class CombatSystem(
    private val state: GameState,
    private val bridgeSystem: BridgeSystem,
    private val getEffectiveLevel: (Defender) -> Int = { it.level.value },
    private val getEffectiveRange: (Defender) -> Int = { it.range }
) {
    
    // Track kills this turn for achievements
    private var killsThisTurn = 0
    private var killedTypesThisTurn = mutableListOf<AttackerType>()
    
    // Callback for combat results (for achievements)
    var onCombatResult: ((CombatResult) -> Unit)? = null
    
    /**
     * Reset turn counters at start of turn
     */
    fun startTurn() {
        killsThisTurn = 0
        killedTypesThisTurn.clear()
    }
    
    companion object {
        // LASTING damage is applied at half the initial damage per turn
        private const val LASTING_DAMAGE_DIVISOR = 2
    }
    
    /**
     * Calculate effective damage for a defender, accounting for level buffs
     */
    private fun getEffectiveDamage(defender: Defender): Int {
        val effectiveLevel = getEffectiveLevel(defender)
        return defender.type.baseDamage + (effectiveLevel - 1) * 5
    }

    /**
     * Returns true if the given position is a valid area-attack target tile:
     * on the enemy path, a bridge, or a spawn point.
     */
    private fun isValidAreaTargetPosition(position: Position): Boolean {
        return state.level.isEnemyTraversable(position) || state.isBridgeAt(position)
    }

    fun defenderAttack(defenderId: Int, targetId: Int, processDefeated: () -> Unit): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        val target = state.attackers.find { it.id == targetId && !it.isDefeated.value } ?: return false
        
        if (!defender.canAttack(target)) return false
        
        // Mark defender as used
        defender.hasBeenUsed.value = true
        
        // Play attack sound based on attack type
        val soundEvent = when (defender.type.attackType) {
            AttackType.MELEE -> SoundEvent.ATTACK_MELEE
            AttackType.RANGED -> {
                // Use different sound for ballista
                if (defender.type == DefenderType.BALLISTA_TOWER) {
                    SoundEvent.ATTACK_BALLISTA
                } else {
                    SoundEvent.ATTACK_RANGED
                }
            }
            AttackType.AREA -> SoundEvent.ATTACK_AREA
            AttackType.LASTING -> SoundEvent.ATTACK_LASTING
            AttackType.NONE -> null
        }
        soundEvent?.let { GlobalSoundManager.playSound(it) }
        
        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> singleTargetAttack(defender, target)
            AttackType.AREA -> areaAttack(defender, target.position.value)
            AttackType.LASTING -> lastingAttack(defender, target.position.value)
            AttackType.NONE -> return false  // Mines and special structures can't attack
        }

        // Record attack impact visual effect at the target position (deduplicated per tile per turn)
        if (state.towerAttackEffects.none { it.targetPosition == target.position.value }) {
            state.towerAttackEffects.add(
                TowerAttackEffect(
                    targetPosition = target.position.value,
                    turnNumber = state.turnNumber.value
                )
            )
        }

        // Record arrow/bolt visual on the tower tile for ranged attacks (Bow, Spear)
        // Ballista uses a separate overlay animation (BallistaAttackEffect)
        if (defender.type.attackType == AttackType.RANGED) {
            if (defender.type == DefenderType.BALLISTA_TOWER) {
                if (state.ballistaAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.ballistaAttackEffects.add(
                        BallistaAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = target.position.value,
                            turnNumber = state.turnNumber.value
                        )
                    )
                }
            } else {
                if (state.arrowAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.arrowAttackEffects.add(
                        ArrowAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = target.position.value,
                            turnNumber = state.turnNumber.value
                        )
                    )
                }
            }
        }

        defender.actionsRemaining.value--

        // Process defeated attackers immediately to give coins
        processDefeated()

        return true
    }

    fun defenderAttackPosition(defenderId: Int, targetPosition: Position, processDefeated: () -> Unit): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false

        // Check if defender can reach the target position
        val distance = defender.position.value.distanceTo(targetPosition)
        if (distance < defender.type.minRange || distance > getEffectiveRange(defender)) return false
        if (!defender.isReady || defender.actionsRemaining.value <= 0) return false
        
        // Mark defender as used
        defender.hasBeenUsed.value = true

        // For AOE and DOT attacks, target position must be on the path, a river tile, or a spawn point
        if (defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) {
            if (!state.level.isEnemyOccupiable(targetPosition)) return false
        } else {
            // For single-target attacks, prioritize enemy over bridge at the same position
            val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }
            val bridge = state.getBridgeAt(targetPosition)
            if (target == null && (bridge == null || !bridge.isActive)) return false
        }

        // Play attack sound based on attack type
        val soundEvent = when (defender.type.attackType) {
            AttackType.MELEE -> SoundEvent.ATTACK_MELEE
            AttackType.RANGED -> {
                // Use different sound for ballista
                if (defender.type == DefenderType.BALLISTA_TOWER) {
                    SoundEvent.ATTACK_BALLISTA
                } else {
                    SoundEvent.ATTACK_RANGED
                }
            }
            AttackType.AREA -> SoundEvent.ATTACK_AREA
            AttackType.LASTING -> SoundEvent.ATTACK_LASTING
            AttackType.NONE -> null
        }
        soundEvent?.let { GlobalSoundManager.playSound(it) }

        // Perform attack based on type
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> {
                // Single target attack - prioritize enemy over bridge
                val target = state.attackers.find { it.position.value == targetPosition && !it.isDefeated.value }
                
                if (target != null) {
                    // Attack enemy (takes priority)
                    singleTargetAttack(defender, target)
                } else {
                    // No enemy, attack bridge if present
                    val bridge = state.getBridgeAt(targetPosition)
                    if (bridge != null && bridge.isActive) {
                        bridgeSystem.damageBridge(targetPosition, getEffectiveDamage(defender))
                    } else {
                        return false
                    }
                }
            }
            AttackType.AREA -> {
                // Area attack affects both enemies AND bridges in range
                areaAttack(defender, targetPosition)
                // Also damage bridge at target position if present
                val bridge = state.getBridgeAt(targetPosition)
                if (bridge != null && bridge.isActive) {
                    bridgeSystem.damageBridge(targetPosition, getEffectiveDamage(defender))
                }
            }
            AttackType.LASTING -> lastingAttack(defender, targetPosition)
            AttackType.NONE -> return false  // Mines and special structures can't attack
        }

        // Record attack impact visual effect at the target position (deduplicated per tile per turn)
        if (state.towerAttackEffects.none { it.targetPosition == targetPosition }) {
            state.towerAttackEffects.add(
                TowerAttackEffect(
                    targetPosition = targetPosition,
                    turnNumber = state.turnNumber.value
                )
            )
        }

        // Record arrow/bolt visual on the tower tile for ranged attacks (Bow, Spear)
        // Ballista uses a separate overlay animation (BallistaAttackEffect)
        if (defender.type.attackType == AttackType.RANGED) {
            if (defender.type == DefenderType.BALLISTA_TOWER) {
                if (state.ballistaAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.ballistaAttackEffects.add(
                        BallistaAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = targetPosition,
                            turnNumber = state.turnNumber.value
                        )
                    )
                }
            } else {
                if (state.arrowAttackEffects.none { it.sourcePosition == defender.position.value }) {
                    state.arrowAttackEffects.add(
                        ArrowAttackEffect(
                            sourcePosition = defender.position.value,
                            targetPosition = targetPosition,
                            turnNumber = state.turnNumber.value
                        )
                    )
                }
            }
        }

        defender.actionsRemaining.value--

        // Process defeated attackers immediately to give coins
        processDefeated()
        
        return true
    }
    
    private fun singleTargetAttack(defender: Defender, target: Attacker) {
        target.currentHealth.value -= getEffectiveDamage(defender)
        if (target.currentHealth.value <= 0) {
            target.isDefeated.value = true
        }
        
        // Apply spike barbs effect (level 10+ with Construction level 1+)
        if (defender.type == DefenderType.SPIKE_TOWER && 
            defender.level.value >= 10 && 
            state.constructionLevel >= PlayerAbilities.CONSTRUCTION_LEVEL_1) {
            target.movementPenalty.value += 1
        }
    }
    
    private fun areaAttack(defender: Defender, targetPosition: Position) {
        // Calculate affected positions - target and neighbors within area effect radius
        // At level 20+, radius increases from 1 to 2 tiles
        val affectedPositions = mutableSetOf(targetPosition)
        val radius = defender.areaEffectRadius
        
        if (radius == 1) {
            // Use standard hex neighbors for radius 1
            affectedPositions.addAll(
                targetPosition.getHexNeighbors().filter { neighbor ->
                    neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
                    neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
                    isValidAreaTargetPosition(neighbor)
                }
            )
        } else {
            // Use extended radius for level 20+
            affectedPositions.addAll(
                targetPosition.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                    .filter { isValidAreaTargetPosition(it) }
            )
        }

        // Only include target position if it's on the path, a bridge, or a spawn point
        if (!isValidAreaTargetPosition(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }

        // Damage all enemies in affected positions (except those immune to fireballs)
        val targets = state.attackers.filter { 
            !it.isDefeated.value && affectedPositions.contains(it.position.value)
        }
        
        for (target in targets) {
            // Check immunity to fireball (Red Demons)
            if (target.canBeDamagedByFireball()) {
                target.currentHealth.value -= getEffectiveDamage(defender)
                if (target.currentHealth.value <= 0) {
                    target.isDefeated.value = true
                }
            }
        }

        // Clear existing fireball effects from this defender
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.FIREBALL && it.defenderId == defender.id
        }

        // Remove acid effects from affected positions (fire burns away the acid)
        state.fieldEffects.removeAll {
            it.type == FieldEffectType.ACID && it.position in affectedPositions
        }
        
        // Damage all bridges in affected positions
        affectedPositions.forEach { pos ->
            val bridge = state.getBridgeAt(pos)
            if (bridge != null && bridge.isActive) {
                bridgeSystem.damageBridge(pos, getEffectiveDamage(defender))
            }
        }

        // Add new fireball effects (visual only, last for 1 turn to show affected area)
        for (pos in affectedPositions) {
            state.fieldEffects.add(
                FieldEffect(
                    position = pos,
                    type = FieldEffectType.FIREBALL,
                    damage = getEffectiveDamage(defender),
                    turnsRemaining = 1,  // Visual effect lasts 1 turn
                    defenderId = defender.id
                )
            )
        }
    }
    
    private fun lastingAttack(defender: Defender, targetPosition: Position) {
        // Calculate affected positions - target and neighbors within area effect radius
        // At level 20+, radius increases from 1 to 2 tiles
        val affectedPositions = mutableSetOf(targetPosition)
        val radius = defender.areaEffectRadius
        
        if (radius == 1) {
            // Use standard hex neighbors for radius 1
            affectedPositions.addAll(
                targetPosition.getHexNeighbors().filter { neighbor ->
                    neighbor.x >= 0 && neighbor.x < state.level.gridWidth &&
                    neighbor.y >= 0 && neighbor.y < state.level.gridHeight &&
                    state.level.isEnemyTraversable(neighbor)
                }
            )
        } else {
            // Use extended radius for level 20+
            affectedPositions.addAll(
                targetPosition.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                    .filter { state.level.isEnemyTraversable(it) }
            )
        }

        // Remove target position only if it's neither on path, on a bridge, nor a spawn point
        if (!isValidAreaTargetPosition(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }
        
        // Apply initial damage and DOT to all enemies in affected positions
        val targets = state.attackers.filter {
            !it.isDefeated.value && affectedPositions.contains(it.position.value)
        }

        for (target in targets) {
            // Check immunity to acid (Blue Demons)
            if (target.canBeDamagedByAcid()) {
                // Initial damage is same as DOT tick damage (not full damage)
                target.currentHealth.value -= getEffectiveDamage(defender) / LASTING_DAMAGE_DIVISOR
                // Mark for additional rounds of DOT based on tower level
                defender.dotRoundsRemaining[target.id] = defender.dotDuration

                if (target.currentHealth.value <= 0) {
                    target.isDefeated.value = true
                }
            }
        }

        // Create field effects for acid DOT on all affected positions
        // Don't remove existing acid effects - they should persist until they expire
        
        // Get all positions with active fireball effects (fire burns away acid)
        val fireballPositions = state.fieldEffects
            .filter { it.type == FieldEffectType.FIREBALL }
            .mapTo(mutableSetOf()) { it.position }
        
        for (pos in affectedPositions) {
            // Skip this position if there's an active fireball
            if (pos in fireballPositions) continue
            
            // Find if there's an enemy at this position
            val enemyAtPos = targets.find { it.position.value == pos }

            // Check if there's already an acid effect at this position
            val existingEffect = state.fieldEffects.find {
                it.type == FieldEffectType.ACID && it.position == pos
            }

            val newDuration = defender.dotDuration

            if (existingEffect != null) {
                // If existing effect has more turns, keep it; otherwise replace it
                if (newDuration > existingEffect.turnsRemaining) {
                    state.fieldEffects.remove(existingEffect)
                    state.fieldEffects.add(
                        FieldEffect(
                            position = pos,
                            type = FieldEffectType.ACID,
                            damage = getEffectiveDamage(defender) / LASTING_DAMAGE_DIVISOR,
                            turnsRemaining = newDuration,
                            defenderId = defender.id,
                            attackerId = enemyAtPos?.id
                        )
                    )
                }
                // If existing has equal or more turns, do nothing (keep existing)
            } else {
                // No existing effect, add new one
                state.fieldEffects.add(
                    FieldEffect(
                        position = pos,
                        type = FieldEffectType.ACID,
                        damage = getEffectiveDamage(defender) / LASTING_DAMAGE_DIVISOR,
                        turnsRemaining = newDuration,
                        defenderId = defender.id,
                        attackerId = enemyAtPos?.id
                    )
                )
            }
        }
    }
    
    fun applyLastingEffects() {
        // Apply LASTING damage from acid puddles on the ground
        val acidEffects = state.fieldEffects.filter { it.type == FieldEffectType.ACID }

        for (effect in acidEffects) {
            // Find all enemies standing in the acid
            val enemiesInAcid = state.attackers.filter {
                !it.isDefeated.value && it.position.value == effect.position
            }

            for (attacker in enemiesInAcid) {
                // Check immunity to acid (Blue Demons)
                if (attacker.canBeDamagedByAcid()) {
                    attacker.currentHealth.value -= effect.damage
                    if (attacker.currentHealth.value <= 0) {
                        attacker.isDefeated.value = true
                    }
                }
            }
        }
    }
    
    fun processDefeatedAttackers() {
        val defeated = state.attackers.filter { it.isDefeated.value && !state.level.isTargetPosition(it.position.value) }
        
        // Track kills for this attack
        val killsThisAttack = defeated.size
        val killedTypes = defeated.map { it.type }
        
        // Update turn totals
        killsThisTurn += killsThisAttack
        killedTypesThisTurn.addAll(killedTypes)
        
        // Emit combat result for achievement tracking
        if (killsThisAttack > 0) {
            onCombatResult?.invoke(
                CombatResult(
                    killsThisAttack = killsThisAttack,
                    killedEnemyTypes = killedTypes
                )
            )
        }
        
        // Calculate XP and coins for defeated enemies
        for (attacker in defeated) {
            // Award coins (with income multiplier from player stats)
            val baseCoins = attacker.type.reward * attacker.level.value
            val modifiedCoins = (baseCoins * state.incomeMultiplier).toInt()
            state.coins.value += modifiedCoins
            
            // Record enemy death visual effect for animation
            state.defeatedEnemyEffects.add(
                EnemyDeathEffect(
                    position = attacker.position.value,
                    turnNumber = state.turnNumber.value,
                    attackerType = attacker.type,
                    attackerLevel = attacker.level.value
                )
            )
            
            // Record coin gain visual effect for animation (only when coins are actually awarded)
            if (modifiedCoins > 0) {
                state.coinGainEffects.add(
                    CoinGainEffect(
                        position = attacker.position.value,
                        amount = modifiedCoins,
                        turnNumber = state.turnNumber.value
                    )
                )
            }
            
            // Award XP (multiplied by level for non-dragons)
            val xpEarned = attacker.type.xp * attacker.level.value
            state.xpEarnedThisLevel.value += xpEarned
            
            // Play enemy destroyed sound only if not building a bridge
            if (!attacker.isBuildingBridge.value) {
                GlobalSoundManager.playSound(SoundEvent.ENEMY_DESTROYED)
            }
            
            // Queue Ewhad message (retreats unless it's the final stand level)
            if (attacker.type == AttackerType.EWHAD) {
                val isFinalStand = state.level.editorLevelId == "the_final_stand"
                val messageType = if (isFinalStand) {
                    GameMessageType.EWHAD_DEFEATED
                } else {
                    GameMessageType.EWHAD_RETREATS
                }
                state.pendingMessages.add(GameMessage(type = messageType))
            }
        }
        state.attackers.removeAll { it.isDefeated.value }
    }
    
    /**
     * Get total kills this turn (for achievements)
     */
    fun getKillsThisTurn(): Int = killsThisTurn
}
