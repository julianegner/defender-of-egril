package de.egril.defender.game

import de.egril.defender.model.*

/**
 * Runemaster arcane blast against nearby barricades and towers. Extracted from [EnemyAbilitySystem].
 */
internal class RunemasterAbilities(
    private val state: GameState,
    private val barricadeSystem: BarricadeSystem,
) {
    fun handleRunemasterArcaneBlast(runemaster: Attacker) {
        val cooldown = runemaster.type.towerDisableCooldown ?: return
        if (runemaster.villainCooldown.value > 0) {
            runemaster.villainCooldown.value--
            return
        }

        val range = runemaster.type.towerDisableRangeBase ?: return
        val nearestBarricade =
            state.barricades
                .filter { !it.isDestroyed() }
                .filter { runemaster.position.value.hexDistanceTo(it.position) <= range }
                .minByOrNull { runemaster.position.value.hexDistanceTo(it.position) }
        val nearestTower =
            state.defenders
                .filter { it.isReady && !it.isDisabled.value && it.level.value <= runemaster.effectiveLevel }
                .filter { runemaster.position.value.hexDistanceTo(it.position.value) <= range }
                .minByOrNull { runemaster.position.value.hexDistanceTo(it.position.value) }

        if (nearestBarricade == null && nearestTower == null) return

        val attackBarricade =
            nearestBarricade != null &&
                (
                    nearestTower == null ||
                        runemaster.position.value.hexDistanceTo(nearestBarricade.position) <=
                        runemaster.position.value.hexDistanceTo(nearestTower.position.value)
                )
        if (attackBarricade) {
            barricadeSystem.handleEnemyAttackBarricade(runemaster, nearestBarricade, runemaster.effectiveLevel)
        } else if (nearestTower != null) {
            nearestTower.isDisabled.value = true
            nearestTower.disabledTurnsRemaining.value = (runemaster.type.towerDisableDurationTurns ?: 1) + 1
            state.damageEffects.add(
                DamageEffect(
                    position = nearestTower.position.value,
                    damageAmount = runemaster.effectiveLevel,
                    turnNumber = state.turnNumber.value,
                ),
            )
        }

        runemaster.villainCooldown.value = cooldown
    }
}
