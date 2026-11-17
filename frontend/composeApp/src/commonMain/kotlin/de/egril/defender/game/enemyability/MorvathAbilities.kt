package de.egril.defender.game

import de.egril.defender.model.*

/**
 * Shadow fog ability of Morvath the Shadowmaster.
 */
internal class MorvathAbilities(
    private val state: GameState,
    private val isWithinBounds: (Position) -> Boolean,
    private val isTileOccupiedByStaticObject: (Position) -> Boolean,
) {
    private companion object {
        private const val SHADOW_FOG_DURATION_TURNS = 3
        private const val SHADOW_FOG_MAX_RANGE = 10
    }

    fun handleMorvathShadowFog(morvath: Attacker) {
        val origin = morvath.position.value
        val guaranteedShadowTiles = listOf(origin) + origin.getHexNeighbors()
        val guaranteedShadowSet = guaranteedShadowTiles.toSet()

        // Remove existing shadow fog from any tiles that now have towers, barricades or traps
        state.fieldEffects.removeAll { it.type == FieldEffectType.SHADOW_FOG && isTileOccupiedByStaticObject(it.position) }

        guaranteedShadowTiles
            .filter(isWithinBounds)
            .filter { !isTileOccupiedByStaticObject(it) }
            .forEach { pos ->
                refreshShadowFogAt(pos, morvath.id)
            }

        // Ranged candidates: exclude already-shadowed tiles, static objects, and guaranteed set
        val rangedCandidates =
            origin
                .getHexNeighborsWithinRadius(
                    SHADOW_FOG_MAX_RANGE,
                    state.level.gridWidth,
                    state.level.gridHeight,
                ).filter { it !in guaranteedShadowSet }
                .filter(isWithinBounds)
                .filter { !isTileOccupiedByStaticObject(it) }
                .filter { pos -> state.fieldEffects.none { it.type == FieldEffectType.SHADOW_FOG && it.position == pos } }

        val extraTarget =
            rangedCandidates.maxWithOrNull(
                compareBy<Position> { shadowFogPriorityScore(it, origin) }
                    .thenByDescending { it.y }
                    .thenByDescending { it.x },
            )
        if (extraTarget != null) {
            // Fog is NOT applied here — it will be applied after the orb animation completes.
            if (state.morvathShadowOrbEffects.none { it.sourcePosition == origin }) {
                state.morvathShadowOrbEffects.add(
                    MorvathShadowOrbEffect(
                        sourcePosition = origin,
                        targetPosition = extraTarget,
                        turnNumber = state.turnNumber.value,
                        attackerId = morvath.id,
                    ),
                )
            }
        }
    }

    /** Apply fog to all pending Morvath orb targets (called after the orb animation completes). */
    fun applyPendingMorvathFog() {
        state.morvathShadowOrbEffects.forEach { effect ->
            refreshShadowFogAt(effect.targetPosition, effect.attackerId)
        }
    }

    private fun shadowFogPriorityScore(
        position: Position,
        origin: Position,
    ): Int {
        val isOnPath = state.level.isOnPath(position) || state.level.isSpawnPoint(position) || state.level.isRiverTile(position)
        if (!isOnPath) return -10

        // Prefer tiles closer to the player's target than Morvath — i.e. ahead of him on the path
        val nearestTarget = state.level.targetPositions.minByOrNull { origin.hexDistanceTo(it) }
        val progressScore =
            if (nearestTarget != null) {
                val morvathDistToTarget = origin.hexDistanceTo(nearestTarget)
                val tileDistToTarget = position.hexDistanceTo(nearestTarget)
                morvathDistToTarget - tileDistToTarget // positive = tile is ahead toward target
            } else {
                0
            }

        val hasEnemy = state.attackers.any { !it.isDefeated.value && it.position.value == position }
        return if (hasEnemy) progressScore + 20 else progressScore
    }

    private fun refreshShadowFogAt(
        position: Position,
        sourceAttackerId: Int,
    ) {
        val existingEffect =
            state.fieldEffects.find {
                it.type == FieldEffectType.SHADOW_FOG && it.position == position
            }
        if (existingEffect != null) {
            existingEffect.turnsRemaining = SHADOW_FOG_DURATION_TURNS
            return
        }

        state.fieldEffects.add(
            FieldEffect(
                position = position,
                type = FieldEffectType.SHADOW_FOG,
                damage = 0,
                turnsRemaining = SHADOW_FOG_DURATION_TURNS,
                defenderId = 0,
                attackerId = sourceAttackerId,
            ),
        )
    }
}
