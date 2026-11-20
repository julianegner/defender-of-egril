package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.config.GameLogBuffer
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.GameState
import de.egril.defender.model.Gargoyle
import de.egril.defender.model.GargoyleRules
import de.egril.defender.model.Position
import de.egril.defender.model.hexDistanceTo

/**
 * Gargoyle guardians: inactive gargoyles strike enemies in range at the end of the player turn,
 * take damage from adjacent enemies, and become temporary enemy attackers when an evil wizard
 * comes within [GargoyleRules.ACTIVATION_RANGE]. Active gargoyles are regular enemy attackers of
 * type [AttackerType.GARGOYLE] that walk the enemy path until their active turns run out.
 *
 * @param getInitialTarget target an enemy spawned at the given position would walk toward
 * @param getEnemyDamage damage an enemy unit deals to a gargoyle it is adjacent to
 */
class GargoyleLogic(
    private val state: GameState,
    private val getInitialTarget: (Position) -> Position,
    private val getEnemyDamage: (Attacker) -> Int,
) {
    /** Inactive gargoyles attack up to [GargoyleRules.ATTACKS_PER_TURN] enemies in range each player turn end. */
    fun performInactiveAttacks() {
        reconcileActiveGargoyles()
        for (gargoyle in state.gargoyles.toList()) {
            if (gargoyle.isActive.value) continue
            repeat(GargoyleRules.ATTACKS_PER_TURN) {
                val target = findNearestEnemyInRange(gargoyle.position.value, GargoyleRules.ATTACK_RANGE) ?: return@repeat
                val damage = gargoyle.attackDamage
                val actualDamage = minOf(target.currentHealth.value, damage)
                target.recordDamageTaken(actualDamage)
                target.currentHealth.value -= damage
                if (target.currentHealth.value <= 0) {
                    target.isDefeated.value = true
                }
            }
        }
    }

    /**
     * Runs at the end of an enemy turn, after movement and before defeated attackers are processed:
     * drops records of gargoyles whose active form was defeated or arrived at its target, applies
     * adjacent enemy damage, converts gargoyles near evil wizards, and counts down active turns.
     */
    fun processEnemyTurnEnd() {
        reconcileActiveGargoyles()
        applyAdjacentEnemyDamage()

        // Decide activations from the pre-countdown state so a gargoyle that just went
        // inactive is not reactivated in the same turn.
        val toActivate =
            state.gargoyles.filter { !it.isActive.value && isWizardInActivationRange(it.position.value) }

        for (gargoyle in state.gargoyles.toList()) {
            if (!gargoyle.isActive.value) continue
            gargoyle.activeTurnsRemaining.value--
            if (gargoyle.activeTurnsRemaining.value <= 0) {
                deactivate(gargoyle)
            }
        }

        for (gargoyle in toActivate) {
            activate(gargoyle)
        }
    }

    /** Removes records of active gargoyles whose enemy form is gone (defeated, arrived, or removed). */
    private fun reconcileActiveGargoyles() {
        for (gargoyle in state.gargoyles.toList()) {
            if (!gargoyle.isActive.value) continue
            val attacker = findActiveAttacker(gargoyle)
            if (attacker == null || attacker.isDefeated.value) {
                state.gargoyles.remove(gargoyle)
            }
        }
    }

    private fun applyAdjacentEnemyDamage() {
        for (gargoyle in state.gargoyles.toList()) {
            if (gargoyle.isActive.value) continue
            val adjacentEnemies =
                state.attackers.filter {
                    !it.isDefeated.value && it.position.value.hexDistanceTo(gargoyle.position.value) == 1
                }
            val damage = adjacentEnemies.sumOf { getEnemyDamage(it) }
            if (damage <= 0) continue
            gargoyle.currentHealth.value -= damage
            if (gargoyle.currentHealth.value <= 0) {
                state.gargoyles.remove(gargoyle)
            }
        }
    }

    private fun isWizardInActivationRange(position: Position): Boolean =
        state.attackers.any {
            !it.isDefeated.value &&
                it.type == AttackerType.EVIL_WIZARD &&
                it.position.value.hexDistanceTo(position) <= GargoyleRules.ACTIVATION_RANGE
        }

    private fun activate(gargoyle: Gargoyle) {
        val position = gargoyle.position.value
        val attacker =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.GARGOYLE,
                position = mutableStateOf(position),
                level = mutableStateOf(gargoyle.level.value),
                currentHealth = mutableStateOf(gargoyle.currentHealth.value),
                currentTarget = mutableStateOf(getInitialTarget(position)),
            )
        state.attackers.add(attacker)
        gargoyle.isActive.value = true
        gargoyle.activeTurnsRemaining.value = GargoyleRules.ACTIVE_TURNS
        gargoyle.activeAttackerId.value = attacker.id
        GameLogBuffer.log("COMBAT", "Gargoyle at $position awakens")
    }

    /** Returns the gargoyle to its inactive form where its enemy form currently stands, keeping its health. */
    private fun deactivate(gargoyle: Gargoyle) {
        val attacker = findActiveAttacker(gargoyle)
        if (attacker != null) {
            gargoyle.position.value = attacker.position.value
            gargoyle.currentHealth.value = attacker.currentHealth.value
            state.attackers.remove(attacker)
        }
        gargoyle.isActive.value = false
        gargoyle.activeTurnsRemaining.value = 0
        gargoyle.activeAttackerId.value = null
    }

    private fun findActiveAttacker(gargoyle: Gargoyle): Attacker? {
        val attackerId = gargoyle.activeAttackerId.value ?: return null
        return state.attackers.firstOrNull { it.id == attackerId }
    }

    private fun findNearestEnemyInRange(
        from: Position,
        range: Int,
    ): Attacker? =
        state.attackers
            .filter { !it.isDefeated.value && from.hexDistanceTo(it.position.value) <= range }
            .minWithOrNull(compareBy<Attacker> { from.hexDistanceTo(it.position.value) }.thenBy { it.id })
}
