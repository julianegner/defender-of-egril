package com.defenderofegril.game

import com.defenderofegril.model.*
import kotlin.math.min

class GameEngine(private val state: GameState) {
    
    fun placeDefender(type: DefenderType, position: Position): Boolean {
        if (!state.canPlaceDefender(type)) return false
        if (isPositionOccupied(position)) return false
        if (position == state.level.startPosition || position == state.level.targetPosition) return false
        
        val defender = Defender(
            id = state.nextDefenderId++,
            type = type,
            position = position
        )
        state.defenders.add(defender)
        state.coins -= type.baseCost
        return true
    }
    
    fun upgradeDefender(defenderId: Int): Boolean {
        val defender = state.defenders.find { it.id == defenderId } ?: return false
        if (!state.canUpgradeDefender(defender)) return false
        
        state.coins -= defender.upgradeCost
        defender.level++
        return true
    }
    
    fun startCombatPhase() {
        if (state.phase == GamePhase.COMBAT) return
        state.phase = GamePhase.COMBAT
        
        // Load first wave if not started
        if (state.currentWaveIndex == 0 && state.attackersToSpawn.isEmpty()) {
            loadNextWave()
        }
    }
    
    fun executeTurn() {
        if (state.phase != GamePhase.COMBAT) return
        
        state.turnNumber++
        
        // Spawn new attackers
        spawnAttackers()
        
        // Defenders attack
        defendersAttack()
        
        // Move attackers
        moveAttackers()
        
        // Apply damage over time effects
        applyDotEffects()
        
        // Remove defeated attackers and give rewards
        processDefeatedAttackers()
        
        // Check if we should load next wave
        if (state.attackersToSpawn.isEmpty() && state.attackers.isEmpty()) {
            loadNextWave()
        }
    }
    
    fun returnToPlanning() {
        state.phase = GamePhase.PLANNING
    }
    
    private fun isPositionOccupied(position: Position): Boolean {
        return state.defenders.any { it.position == position } ||
               state.attackers.any { it.position == position }
    }
    
    private fun loadNextWave() {
        if (state.currentWaveIndex >= state.level.attackerWaves.size) {
            return
        }
        
        val wave = state.level.attackerWaves[state.currentWaveIndex]
        state.attackersToSpawn.addAll(wave.attackers)
        state.currentWaveIndex++
        state.spawnCounter = 0
    }
    
    private fun spawnAttackers() {
        if (state.attackersToSpawn.isEmpty()) return
        
        state.spawnCounter++
        val wave = state.level.attackerWaves.getOrNull(state.currentWaveIndex - 1) ?: return
        
        if (state.spawnCounter >= wave.spawnDelay) {
            val type = state.attackersToSpawn.removeAt(0)
            val attacker = Attacker(
                id = state.nextAttackerId++,
                type = type,
                position = state.level.startPosition
            )
            state.attackers.add(attacker)
            state.spawnCounter = 0
        }
    }
    
    private fun defendersAttack() {
        for (defender in state.defenders) {
            when (defender.type.attackType) {
                AttackType.MELEE, AttackType.RANGED -> singleTargetAttack(defender)
                AttackType.AOE -> aoeAttack(defender)
                AttackType.DOT -> dotAttack(defender)
            }
        }
    }
    
    private fun singleTargetAttack(defender: Defender) {
        val target = state.attackers
            .filter { !it.isDefeated && defender.canAttack(it) }
            .minByOrNull { it.position.distanceTo(state.level.targetPosition) }
            ?: return
        
        target.currentHealth -= defender.damage
        if (target.currentHealth <= 0) {
            target.isDefeated = true
        }
    }
    
    private fun aoeAttack(defender: Defender) {
        // Find primary target
        val primaryTarget = state.attackers
            .filter { !it.isDefeated && defender.canAttack(it) }
            .minByOrNull { it.position.distanceTo(state.level.targetPosition) }
            ?: return
        
        // Damage primary target and nearby enemies
        val targets = state.attackers.filter { 
            !it.isDefeated && it.position.distanceTo(primaryTarget.position) <= 1 
        }
        
        for (target in targets) {
            target.currentHealth -= defender.damage
            if (target.currentHealth <= 0) {
                target.isDefeated = true
            }
        }
    }
    
    private fun dotAttack(defender: Defender) {
        val target = state.attackers
            .filter { !it.isDefeated && defender.canAttack(it) }
            .minByOrNull { it.position.distanceTo(state.level.targetPosition) }
            ?: return
        
        // Apply initial damage
        target.currentHealth -= defender.damage
        // Mark for 3 more rounds of DOT
        defender.dotRoundsRemaining[target.id] = 3
        
        if (target.currentHealth <= 0) {
            target.isDefeated = true
        }
    }
    
    private fun applyDotEffects() {
        for (defender in state.defenders) {
            if (defender.type.attackType != AttackType.DOT) continue
            
            val toRemove = mutableListOf<Int>()
            for ((attackerId, rounds) in defender.dotRoundsRemaining) {
                val attacker = state.attackers.find { it.id == attackerId }
                if (attacker != null && !attacker.isDefeated) {
                    attacker.currentHealth -= defender.damage / 2
                    if (attacker.currentHealth <= 0) {
                        attacker.isDefeated = true
                    }
                    
                    if (rounds <= 1) {
                        toRemove.add(attackerId)
                    } else {
                        defender.dotRoundsRemaining[attackerId] = rounds - 1
                    }
                } else {
                    toRemove.add(attackerId)
                }
            }
            
            toRemove.forEach { defender.dotRoundsRemaining.remove(it) }
        }
    }
    
    private fun moveAttackers() {
        for (attacker in state.attackers) {
            if (attacker.isDefeated) continue
            
            // Simple pathfinding: move towards target
            val current = attacker.position
            val target = state.level.targetPosition
            
            var remainingSpeed = attacker.type.speed
            while (remainingSpeed > 0 && current != target) {
                val newPos = moveTowards(current, target)
                attacker.position = newPos
                remainingSpeed--
                
                // Check if reached target
                if (attacker.position == target) {
                    state.healthPoints--
                    attacker.isDefeated = true
                    break
                }
            }
        }
    }
    
    private fun moveTowards(from: Position, to: Position): Position {
        val dx = to.x - from.x
        val dy = to.y - from.y
        
        return when {
            dx != 0 -> Position(from.x + dx.coerceIn(-1, 1), from.y)
            dy != 0 -> Position(from.x, from.y + dy.coerceIn(-1, 1))
            else -> from
        }
    }
    
    private fun processDefeatedAttackers() {
        val defeated = state.attackers.filter { it.isDefeated && it.position != state.level.targetPosition }
        for (attacker in defeated) {
            state.coins += attacker.type.reward
        }
        state.attackers.removeAll { it.isDefeated }
    }
}
