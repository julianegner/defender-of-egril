package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Mirror-image ability of Silas the Maskmaster.
 */
internal class SilasAbilities(
    private val state: GameState,
    private val isTileOccupiedByStaticObject: (Position) -> Boolean,
) {
    /**
     * Silas the Maskmaster creates two decoy copies on nearby enemy-traversable tiles every three
     * rounds. Existing images remain on the battlefield. After conjuring the new images, the real
     * Silas swaps to a random position among himself and the fresh copies so the player cannot tell
     * which of the three is genuine.
     */
    fun handleSilasMirrorImages(silas: Attacker) {
        val mirrorImageCooldown = silas.type.mirrorImageCooldown ?: return
        if (silas.summonCooldown.value > 0) return

        val mirrorCount = silas.type.mirrorImageCount ?: return
        val mirrorRange = silas.type.mirrorImageRange ?: return
        val silasPosition = silas.position.value
        val inheritedTarget =
            silas.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val candidatePositions =
            silasPosition
                .getHexNeighborsWithinRadius(mirrorRange, state.level.gridWidth, state.level.gridHeight)
                .filter { candidate ->
                    candidate != silasPosition &&
                        state.level.isEnemyTraversable(candidate) &&
                        !isTileOccupiedByStaticObject(candidate) &&
                        !state.isInactiveGargoyleAt(candidate) &&
                        state.attackers.none { !it.isDefeated.value && it.position.value == candidate }
                }.sortedWith(
                    compareBy<Position> { silasPosition.hexDistanceTo(it) }
                        .thenBy { it.x }
                        .thenBy { it.y },
                ).take(mirrorCount)

        if (candidatePositions.isEmpty()) return

        val mirrors =
            candidatePositions.map { spawnPos ->
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.SILAS_MIRROR_IMAGE,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(silas.level.value),
                    currentTarget = mutableStateOf(inheritedTarget),
                )
            }
        state.attackers.addAll(mirrors)
        candidatePositions.forEach { spawnPos ->
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = AttackerType.SILAS_MIRROR_IMAGE,
                ),
            )
        }

        val shuffledPositions = (listOf(silasPosition) + candidatePositions).shuffled()
        silas.position.value = shuffledPositions.first()
        mirrors.zip(shuffledPositions.drop(1)).forEach { (mirror, newPosition) ->
            mirror.position.value = newPosition
        }

        silas.summonCooldown.value = mirrorImageCooldown
    }
}
