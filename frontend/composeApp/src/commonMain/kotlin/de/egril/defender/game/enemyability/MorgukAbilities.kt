package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Abilities of Morguk Bonewhisper, the undead necromancer.
 */
internal class MorgukAbilities(
    private val state: GameState,
) {
    /**
     * Spirit Summon: Morguk Bonewhisper conjures goblins on every adjacent path tile.
     * Each goblin spawns at the same level as Morguk. Activates every 3 turns.
     */
    fun handleMorgukSpiritSummon(morguk: Attacker) {
        if (morguk.summonCooldown.value > 0) return

        val morgukPos = morguk.position.value
        val inheritedTarget =
            morguk.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        // Spawn on adjacent path tiles that are not already occupied by a living unit
        val adjacentPathTiles =
            morgukPos.getHexNeighbors().filter { pos ->
                pos.x >= 0 &&
                    pos.x < state.level.gridWidth &&
                    pos.y >= 0 &&
                    pos.y < state.level.gridHeight &&
                    state.level.isOnPath(pos) &&
                    state.attackers.none { !it.isDefeated.value && it.position.value == pos }
            }

        for (spawnPos in adjacentPathTiles) {
            val goblin =
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.GOBLIN,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(morguk.level.value),
                    currentTarget = mutableStateOf(inheritedTarget),
                )
            state.attackers.add(goblin)
        }

        morguk.summonCooldown.value = 3
    }
}
