package de.egril.defender.game

import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.model.*

/**
 * Handles combat mechanics including single-target, area, and lasting attacks.
 */
class CombatSystem(
    private val state: GameState,
    private val bridgeSystem: BridgeSystem
) {
    
    companion object {
        // LASTING damage is applied at half the initial damage per turn
        private const val LASTING_DAMAGE_DIVISOR = 2
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

        defender.actionsRemaining.value--

        // Process defeated attackers immediately to give coins
        processDefeated()

        return true
    }

    fun defenderAttackPosition(defenderId: Int, targetPosition: Position, processDefeated: () -> Unit): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false

        // Check if defender can reach the target position
        val distance = defender.position.value.distanceTo(targetPosition)
        if (distance < defender.type.minRange || distance > defender.range) return false
        if (!defender.isReady || defender.actionsRemaining.value <= 0) return false
        
        // Mark defender as used
        defender.hasBeenUsed.value = true

        // For AOE and DOT attacks, target position must be on the path OR a river tile
        if (defender.type.attackType == AttackType.AREA || defender.type.attackType == AttackType.LASTING) {
            val isOnPath = state.level.isOnPath(targetPosition)
            val isOnRiver = state.level.getRiverTile(targetPosition) != null
            if (!isOnPath && !isOnRiver) return false
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
                        bridgeSystem.damageBridge(targetPosition, defender.damage)
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
                    bridgeSystem.damageBridge(targetPosition, defender.damage)
                }
            }
            AttackType.LASTING -> lastingAttack(defender, targetPosition)
            AttackType.NONE -> return false  // Mines and special structures can't attack
        }
        
        defender.actionsRemaining.value--
        
        // Process defeated attackers immediately to give coins
        processDefeated()
        
        return true
    }
    
    private fun singleTargetAttack(defender: Defender, target: Attacker) {
        target.currentHealth.value -= defender.damage
        if (target.currentHealth.value <= 0) {
            target.isDefeated.value = true
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
                    (state.level.isOnPath(neighbor) || state.isBridgeAt(neighbor))
                }
            )
        } else {
            // Use extended radius for level 20+
            affectedPositions.addAll(
                targetPosition.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                    .filter { state.level.isOnPath(it) || state.isBridgeAt(it) }
            )
        }

        // Only include target position if it's on the path or a bridge
        if (!state.level.isOnPath(targetPosition) && !state.isBridgeAt(targetPosition)) {
            affectedPositions.remove(targetPosition)
        }

        // Damage all enemies in affected positions (except those immune to fireballs)
        val targets = state.attackers.filter { 
            !it.isDefeated.value && affectedPositions.contains(it.position.value)
        }
        
        for (target in targets) {
            // Check immunity to fireball (Red Demons)
            if (target.canBeDamagedByFireball()) {
                target.currentHealth.value -= defender.damage
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
                bridgeSystem.damageBridge(pos, defender.damage)
            }
        }

        // Add new fireball effects (visual only, last for 1 turn to show affected area)
        for (pos in affectedPositions) {
            state.fieldEffects.add(
                FieldEffect(
                    position = pos,
                    type = FieldEffectType.FIREBALL,
                    damage = defender.damage,
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
                    state.level.isOnPath(neighbor)
                }
            )
        } else {
            // Use extended radius for level 20+
            affectedPositions.addAll(
                targetPosition.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                    .filter { state.level.isOnPath(it) }
            )
        }

        // Remove target position only if it's neither on path nor on a bridge
        if (!state.level.isOnPath(targetPosition) && !state.isBridgeAt(targetPosition)) {
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
                target.currentHealth.value -= defender.damage / LASTING_DAMAGE_DIVISOR
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
                            damage = defender.damage / LASTING_DAMAGE_DIVISOR,
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
                        damage = defender.damage / LASTING_DAMAGE_DIVISOR,
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
        for (attacker in defeated) {
            state.coins.value += attacker.type.reward * attacker.level.value
            // Play enemy destroyed sound only if not building a bridge
            if (!attacker.isBuildingBridge.value) {
                GlobalSoundManager.playSound(SoundEvent.ENEMY_DESTROYED)
            }
        }
        state.attackers.removeAll { it.isDefeated.value }
    }
}
