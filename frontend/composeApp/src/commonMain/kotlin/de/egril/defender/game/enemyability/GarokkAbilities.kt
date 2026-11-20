package de.egril.defender.game

import de.egril.defender.model.*

/**
 * Garokk's war cry and horde momentum (Waaagh points). Extracted from [EnemyAbilitySystem].
 */
internal class GarokkAbilities(
    private val state: GameState,
) {
    fun hordeUnitCountsForWaagh(attackerType: AttackerType): Boolean = attackerType.faction == EnemyFaction.HORDE || attackerType.unitSize > 0

    fun handleGarokkWarCry() {
        if (!state.level.waaghEnabled) return
        val garokk =
            state.attackers.firstOrNull { attacker -> !attacker.isDefeated.value && attacker.type == AttackerType.GAROKK }
                ?: return
        state.addWaaghPoints(5)
        state.garokkWarCryEffects.add(
            GarokkWarCryEffect(
                position = garokk.position.value,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    fun applyHordeMomentumWaagh() {
        if (!state.level.waaghEnabled) return
        val hordeUnits = state.attackers.filter { attacker -> !attacker.isDefeated.value && hordeUnitCountsForWaagh(attacker.type) }
        if (hordeUnits.isEmpty()) return

        val visitedIds = mutableSetOf<Int>()
        var totalGain = 0
        for (unit in hordeUnits) {
            if (unit.id in visitedIds) continue
            val queue = ArrayDeque<Attacker>()
            queue.add(unit)
            visitedIds.add(unit.id)
            val cluster = mutableListOf<Attacker>()

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                cluster.add(current)
                val adjacentPositions =
                    current.position.value
                        .getHexNeighbors()
                        .toSet() + current.position.value
                for (candidate in hordeUnits) {
                    if (candidate.id in visitedIds) continue
                    if (candidate.position.value in adjacentPositions) {
                        queue.add(candidate)
                        visitedIds.add(candidate.id)
                    }
                }
            }

            if (cluster.size >= 9) {
                totalGain += cluster.size / 2
            }
        }

        state.addWaaghPoints(totalGain)
    }
}
