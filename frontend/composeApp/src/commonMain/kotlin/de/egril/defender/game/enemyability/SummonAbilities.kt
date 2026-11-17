package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Generic summoning used by evil wizards, runemasters, ewhads and Zythar. Extracted from
 * [EnemyAbilitySystem].
 */
internal class SummonAbilities(
    private val state: GameState,
    private val zytharPortal: ZytharPortalAbilities,
) {
    internal fun handleSummon(
        attacker: Attacker,
        state: GameState,
        blueDemons: Int,
        redDemons: Int,
        undead: Int = 0,
        fixedLevel: Int? = null,
    ) {
        if (attacker.summonCooldown.value == 0) {
            repeat(blueDemons) {
                spawnDemonNear(attacker, AttackerType.BLUE_DEMON, fixedLevel ?: state.turnNumber.value)
            }
            repeat(redDemons) {
                spawnDemonNear(attacker, AttackerType.RED_DEMON, fixedLevel ?: state.turnNumber.value)
            }
            repeat(undead) {
                spawnUndeadNear(attacker, 10 + (fixedLevel ?: state.turnNumber.value))
            }
            attacker.summonCooldown.value = 3
        }
    }

    /**
     * Zythar summons blue and red demons plus demonling scouts every 3 enemy turns.
     */
    internal fun handleZytharSummon(zythar: Attacker) {
        if (zythar.summonCooldown.value > 0) return
        val level = zythar.level.value
        // Summon blue demons, red demons, and demonlings
        repeat(level) { spawnDemonNear(zythar, AttackerType.BLUE_DEMON, level) }
        repeat(maxOf(1, level / 2)) { spawnDemonNear(zythar, AttackerType.RED_DEMON, level) }
        repeat(2 + level) { zytharPortal.spawnDemonlingNear(zythar) }
        zythar.summonCooldown.value = 3
    }

    /**
     * Spawn a demon near the given attacker (1-2 cells away)
     */
    private fun spawnDemonNear(
        summoner: Attacker,
        demonType: AttackerType,
        level: Int,
    ) {
        val summonerPos = summoner.position.value

        // Try to find a free position 1-2 cells away
        val possiblePositions = mutableListOf<Position>()

        // Get positions 1 cell away
        possiblePositions.addAll(summonerPos.getHexNeighbors())

        // Get positions 2 cells away
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }

        // Filter valid positions (on path, not occupied, within bounds)
        val validPositions =
            possiblePositions
                .filter { pos ->
                    pos.x >= 0 &&
                        pos.x < state.level.gridWidth &&
                        pos.y >= 0 &&
                        pos.y < state.level.gridHeight &&
                        state.level.isOnPath(pos) &&
                        !state.isPortalTile(pos) &&
                        !state.attackers.any { it.position.value == pos && !it.isDefeated.value }
                }.distinct()

        if (validPositions.isEmpty()) return

        // Pick a random position
        val spawnPos = validPositions.random()

        // Inherit the summoner's current target so demons follow the same waypoint chain
        val inheritedTarget =
            summoner.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                // Use the first waypoint's next target, not the waypoint position itself
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val demon =
            Attacker(
                id = state.nextAttackerId.value++,
                type = demonType,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(level),
                currentTarget = mutableStateOf(inheritedTarget),
            )
        state.attackers.add(demon)
    }

    /**
     * Spawn an undead (skeleton) near the given attacker
     */
    private fun spawnUndeadNear(
        summoner: Attacker,
        level: Int,
    ) {
        spawnDemonNear(summoner, AttackerType.SKELETON, level)
    }
}
