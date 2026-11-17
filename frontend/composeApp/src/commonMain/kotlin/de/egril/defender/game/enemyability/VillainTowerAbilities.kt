package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Tower-disabling and summoning abilities of villains: Sylvanas, Malakor, Ignis-Va and Vaelen.
 */
internal class VillainTowerAbilities(
    private val state: GameState,
) {
    /**
     * Sylvanas's Root Grip: every [towerDisableCooldown] rounds, thorny vines burst from beneath
     * the highest-damage non-disabled tower within [towerDisableRangeBase] tiles, blocking it for
     * [towerDisableDurationTurns] player turns (+1 to account for the immediate decrement at the
     * end of the enemy turn).
     */
    fun handleSylvanasRootGrip(sylvanas: Attacker) {
        val cooldown = sylvanas.type.towerDisableCooldown ?: return
        if (sylvanas.villainCooldown.value > 0) {
            sylvanas.villainCooldown.value--
            return
        }

        val range = sylvanas.type.towerDisableRangeBase ?: 0
        // +1 to account for the immediate decrement in updateTowerDisableStatus at enemy-turn end
        val disableDurationTurns = (sylvanas.type.towerDisableDurationTurns ?: 0) + 1

        val targetTower =
            state.defenders
                .filter { tower ->
                    tower.isReady &&
                        !tower.isDisabled.value &&
                        sylvanas.position.value.hexDistanceTo(tower.position.value) <= range
                }.maxWithOrNull(
                    compareBy<Defender> { it.actualDamage }
                        .thenByDescending { sylvanas.position.value.hexDistanceTo(it.position.value) },
                )

        if (targetTower != null) {
            targetTower.isDisabled.value = true
            targetTower.disabledTurnsRemaining.value = disableDurationTurns
            targetTower.hasRootGripAnimation.value = true // Show vine animation while disabled
        }

        sylvanas.villainCooldown.value = cooldown
    }

    /**
     * Time Loop (Archmage Malakor the Renegade): every [towerDisableCooldown] rounds, Malakor
     * slows the flow of time in a radius of [towerDisableRangeBase] tiles. All ready, non-disabled
     * towers within that radius must skip their next player turn.
     */
    fun handleMalakorTimeLoop(malakor: Attacker) {
        val cooldown = malakor.type.towerDisableCooldown ?: return
        if (malakor.villainCooldown.value > 0) {
            malakor.villainCooldown.value--
            return
        }

        val range = malakor.type.towerDisableRangeBase ?: 0
        // +1 to account for the immediate decrement in updateTowerDisableStatus at enemy-turn end
        val disableDurationTurns = (malakor.type.towerDisableDurationTurns ?: 0) + 1

        // Time Loop affects ALL towers in range, not just the nearest one
        state.defenders
            .filter { tower ->
                tower.isReady &&
                    !tower.isDisabled.value &&
                    malakor.position.value.hexDistanceTo(tower.position.value) <= range
            }.forEach { tower ->
                tower.isDisabled.value = true
                tower.disabledTurnsRemaining.value = disableDurationTurns
            }

        malakor.villainCooldown.value = cooldown
    }

    /**
     * Ignis-Va's Call of the Brood: every 3 rounds summons two flying Dragon-Terrors near her
     * position. Each Dragon-Terror spawns at the villain's level (minimum level 2).
     */
    fun handleIgnisVaCallOfTheBrood(ignisVa: Attacker) {
        if (ignisVa.summonCooldown.value > 0) return

        val spawnLevel = maxOf(2, ignisVa.level.value)
        repeat(2) {
            spawnDragonTerrorNear(ignisVa, spawnLevel)
        }
        ignisVa.summonCooldown.value = 3
    }

    /**
     * Spawn a Dragon-Terror near the given summoner on a free path tile.
     * Dragon-Terrors are always at least level 2.
     */
    private fun spawnDragonTerrorNear(
        summoner: Attacker,
        level: Int,
    ) {
        val summonerPos = summoner.position.value

        val possiblePositions = mutableListOf<Position>()
        possiblePositions.addAll(summonerPos.getHexNeighbors())
        for (neighbor in summonerPos.getHexNeighbors()) {
            possiblePositions.addAll(neighbor.getHexNeighbors())
        }

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

        val spawnPos = validPositions.random()

        val inheritedTarget =
            summoner.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        val dragonTerror =
            Attacker(
                id = state.nextAttackerId.value++,
                type = AttackerType.DRAGON_TERROR,
                position = mutableStateOf(spawnPos),
                level = mutableStateOf(level),
                currentTarget = mutableStateOf(inheritedTarget),
            )
        state.attackers.add(dragonTerror)

        state.enemySpawnEffects.add(
            EnemySpawnEffect(
                position = spawnPos,
                turnNumber = state.turnNumber.value,
                attackerType = AttackerType.DRAGON_TERROR,
            ),
        )
    }

    fun handleVaelenRunicSurge(vaelen: Attacker) {
        if (vaelen.summonCooldown.value > 0) return

        val range = vaelen.type.towerDisableRangeBase ?: return
        val cooldown = vaelen.type.towerDisableCooldown ?: return
        val duration = vaelen.type.towerDisableDurationTurns ?: return
        state.defenders
            .asSequence()
            .filter { tower ->
                tower.isReady &&
                    !tower.isDisabled.value &&
                    vaelen.position.value.hexDistanceTo(tower.position.value) <= range
            }.sortedBy { tower -> vaelen.position.value.hexDistanceTo(tower.position.value) }
            .take(2)
            .forEach { tower ->
                tower.isDisabled.value = true
                tower.disabledTurnsRemaining.value = duration + 1
            }

        vaelen.summonCooldown.value = cooldown
    }
}
