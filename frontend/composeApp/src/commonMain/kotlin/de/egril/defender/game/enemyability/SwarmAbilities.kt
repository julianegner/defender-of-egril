package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Shared spawn helpers for swarm-style summons (snotlings, spiderlings, shadow units).
 */
internal class SwarmAbilities(
    private val state: GameState,
    private val isWithinBounds: (Position) -> Boolean,
    private val isTileOccupiedByStaticObject: (Position) -> Boolean,
) {
    fun summonSwarmUnit(
        type: AttackerType,
        spawnPos: Position,
        level: Int,
        currentTarget: Position,
    ) {
        val existingSwarmUnit =
            state.attackers.find {
                !it.isDefeated.value &&
                    it.position.value == spawnPos &&
                    it.type == type
            }
        if (existingSwarmUnit != null) {
            existingSwarmUnit.currentHealth.value += type.health * level
            return
        }

        state.attackers.add(
            Attacker(
                id = state.nextAttackerId.value++,
                type = type,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(level),
                currentTarget = mutableStateOf(currentTarget),
            ),
        )
    }

    fun resolveSwarmSpawnPositions(
        candidates: Collection<Position>,
        stackableType: AttackerType,
        isBaseTraversable: (Position) -> Boolean,
        maxSearchRings: Int,
    ): List<Position> {
        fun isValidSpawnTile(pos: Position): Boolean =
            isWithinBounds(pos) &&
                isBaseTraversable(pos) &&
                !isTileOccupiedByStaticObject(pos) &&
                state.attackers.none {
                    !it.isDefeated.value &&
                        it.position.value == pos &&
                        it.type != stackableType
                }

        val spawnPositions = mutableListOf<Position>()
        for (candidate in candidates) {
            if (isValidSpawnTile(candidate)) {
                spawnPositions.add(candidate)
            } else {
                val visited = mutableSetOf(candidate)
                var frontier = candidate.getHexNeighbors().toMutableList()
                var found = false
                repeat(maxSearchRings) {
                    if (found) return@repeat
                    val nextFrontier = mutableListOf<Position>()
                    for (pos in frontier) {
                        if (pos in visited) continue
                        visited.add(pos)
                        if (isValidSpawnTile(pos)) {
                            spawnPositions.add(pos)
                            found = true
                            break
                        }
                        nextFrontier.addAll(pos.getHexNeighbors())
                    }
                    frontier = nextFrontier
                }
            }
        }
        return spawnPositions
    }
}
