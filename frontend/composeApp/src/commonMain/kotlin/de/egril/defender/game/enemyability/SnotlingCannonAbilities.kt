package de.egril.defender.game

import de.egril.defender.model.*
import kotlin.random.Random

/**
 * Snotling cannon: a snotling stack that moved this turn throws part of its surplus members
 * a few tiles ahead along its path. The thrown snotlings land at the start of the next enemy phase.
 */
internal class SnotlingCannonAbilities(
    private val state: GameState,
    private val pathfinding: PathfindingSystem,
    private val maxSnotlingsPerTile: Int,
    private val getOrCreateSnotlingStack: (Position) -> Attacker,
) {
    private companion object {
        const val SNOTLING_CANNON_BASE_HEALTH = 100
        const val SNOTLING_CANNON_MAX_THROW = 50
        const val SNOTLING_CANNON_THROW_DISTANCE = 3
        const val SNOTLING_CANNON_MIN_CASUALTY_PERCENT = 10
        const val SNOTLING_CANNON_MAX_CASUALTY_PERCENT = 20
    }

    fun handleSnotlingCannon(snotling: Attacker) {
        val startPosition = state.enemyTurnStartPositions[snotling.id] ?: return
        if (startPosition == snotling.position.value) return
        if (snotling.currentHealth.value < SnotlingCannonRules.MIN_STACK_HEALTH) return

        val thrownCount =
            minOf(
                snotling.currentHealth.value - SNOTLING_CANNON_BASE_HEALTH,
                SNOTLING_CANNON_MAX_THROW,
            )
        if (thrownCount <= 0) return

        val landingTile = findSnotlingCannonLandingTile(snotling) ?: return

        val casualtyPercent =
            Random.nextInt(
                from = SNOTLING_CANNON_MIN_CASUALTY_PERCENT,
                until = SNOTLING_CANNON_MAX_CASUALTY_PERCENT + 1,
            )
        val casualties = (thrownCount * casualtyPercent) / 100
        val survivors = thrownCount - casualties

        snotling.currentHealth.value -= thrownCount
        if (survivors <= 0) return

        state.pendingSnotlingCannonArrivals.add(
            PendingSnotlingCannonArrival(
                targetPosition = landingTile,
                thrownCount = survivors,
                turnNumber = state.turnNumber.value,
            ),
        )
        state.snotlingCannonThrowEffects.add(
            SnotlingCannonThrowEffect(
                sourcePosition = snotling.position.value,
                targetPosition = landingTile,
                thrownCount = survivors,
                turnNumber = state.turnNumber.value,
            ),
        )
    }

    fun processPendingSnotlingCannonArrivals() {
        for (arrival in state.pendingSnotlingCannonArrivals.toList()) {
            val landingStack = getOrCreateSnotlingStack(arrival.targetPosition)
            landingStack.currentHealth.value = minOf(maxSnotlingsPerTile, landingStack.currentHealth.value + arrival.thrownCount)
        }
        state.pendingSnotlingCannonArrivals.clear()
    }

    private fun findSnotlingCannonLandingTile(snotling: Attacker): Position? {
        val currentPosition = snotling.position.value
        val currentTarget =
            snotling.currentTarget?.value ?: state.getActiveTargetPositions().minByOrNull { currentPosition.distanceTo(it) }
                ?: return null
        val path = pathfinding.findPath(currentPosition, currentTarget, snotling, ignoreBarricades = true)
        val landingTile = path.getOrNull(SNOTLING_CANNON_THROW_DISTANCE) ?: return null
        if (!state.level.isOnPath(landingTile)) return null
        if (
            state.attackers.any {
                !it.isDefeated.value &&
                    it.type != AttackerType.SNOTLING &&
                    it.position.value == landingTile
            }
        ) {
            return null
        }
        return landingTile
    }
}
