package de.egril.defender.game

import de.egril.defender.model.*

/**
 * Villain auras that buff or support nearby friendly units each round.
 *
 * Coven effects are delegated back to the facade, which owns the enhanced witch state.
 */
internal class VillainAuraAbilities(
    private val state: GameState,
    private val applyCovenHealBoost: (Attacker, VillainAbility) -> Unit,
    private val applyCovenDisableBoost: (Attacker, VillainAbility) -> Unit,
) {
    /**
     * Applies the aura of every villain that is currently on the battlefield to all friendly units
     * within the ability's range while it is on the battlefield. War Cry style abilities activate on
     * a cooldown; while below 50% health the villain also permanently benefits from its own aura.
     */
    fun processVillainAuras(attackers: List<Attacker>) {
        for (villain in attackers) {
            if (villain.isDefeated.value) continue
            val ability = villain.type.villainAbility ?: continue

            // Decrement the villain's ability cooldown each round.
            if (villain.villainCooldown.value > 0) {
                villain.villainCooldown.value--
            }

            val isBelowHalfHealth = villain.currentHealth.value * 2 <= villain.maxHealth
            val activatesThisRound = villain.villainCooldown.value == 0
            if (activatesThisRound) {
                villain.villainCooldown.value = ability.cooldown
            }

            // Nothing to do this round unless the ability triggers or the wounded villain keeps its
            // permanent self-buff.
            if (!activatesThisRound && !isBelowHalfHealth) continue

            when (ability.effect) {
                VillainAuraEffect.SPEED ->
                    applySpeedAura(
                        villain = villain,
                        ability = ability,
                        applyToAllies = activatesThisRound,
                        applyToSelf = isBelowHalfHealth,
                    )
                VillainAuraEffect.SOUL_CALL -> {
                    // Soul Call is recorded when nearby units die and resolved at the next round start.
                }
                VillainAuraEffect.COVEN_HEAL_BOOST -> {
                    if (activatesThisRound) applyCovenHealBoost(villain, ability)
                }
                VillainAuraEffect.COVEN_DISABLE_BOOST -> {
                    if (activatesThisRound) applyCovenDisableBoost(villain, ability)
                }
                VillainAuraEffect.COVEN_SYNERGY -> {
                    if (activatesThisRound) {
                        applyCovenHealBoost(villain, ability)
                        applyCovenDisableBoost(villain, ability)
                    }
                }
                VillainAuraEffect.RUNE_GLYPH_WARD -> Unit // Evaluated dynamically by RuneGlyphWard.
            }
        }
    }

    private fun applySpeedAura(
        villain: Attacker,
        ability: VillainAbility,
        applyToAllies: Boolean,
        applyToSelf: Boolean,
    ) {
        if (applyToSelf) {
            villain.speedBonus.value = maxOf(villain.speedBonus.value, ability.magnitude)
        }
        if (!applyToAllies || villain.type.faction == EnemyFaction.NONE) return
        for (ally in state.attackers) {
            if (ally.isDefeated.value || ally.id == villain.id) continue
            if (ally.type.faction != villain.type.faction) continue
            val inRange =
                ability.range == VillainAbility.FULL_BATTLEFIELD ||
                    villain.position.value.hexDistanceTo(ally.position.value) <= ability.range
            if (inRange) {
                ally.speedBonus.value = maxOf(ally.speedBonus.value, ability.magnitude)
            }
        }
    }
}
