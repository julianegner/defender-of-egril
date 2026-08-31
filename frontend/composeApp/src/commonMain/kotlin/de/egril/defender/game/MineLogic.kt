package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.DefenderType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position

class MineLogic(
    private val state: GameState,
    private val findClosestTargetPosition: (Position) -> Position,
    private val recordDragonLevelChange: (Attacker, Int) -> Unit,
) {
    private fun findNearestMine(from: Position): Pair<Defender, Int>? {
        val mines =
            state.defenders.filter {
                it.type == DefenderType.DWARVEN_MINE &&
                    !state.destroyedMinePositions.contains(it.position.value)
            }
        if (mines.isEmpty()) return null
        return mines
            .map { mine -> Pair(mine, from.distanceTo(mine.position.value)) }
            .minByOrNull { it.second }
    }

    fun updateDragonMineTargeting(dragon: Attacker) {
        if (dragon.type != AttackerType.DRAGON || dragon.greed <= 5) {
            if (dragon.targetMineId.value != null) {
                dragon.targetMineId.value = null
                dragon.currentTarget?.value =
                    if (state.level.waypoints.isNotEmpty()) {
                        state.level.waypoints
                            .first()
                            .nextTarget
                    } else {
                        findClosestTargetPosition(dragon.position.value)
                    }
            }
            return
        }

        val nearestMine = findNearestMine(dragon.position.value)
        if (nearestMine != null) {
            val (mine, _) = nearestMine
            if (dragon.targetMineId.value != mine.id) {
                dragon.targetMineId.value = mine.id
                dragon.currentTarget?.value = mine.position.value
                dragon.mineWarningShown.value = false
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Dragon ${dragon.id} (greed ${dragon.greed}) now targeting mine ${mine.id} at ${mine.position.value}")
                }
            }
        } else {
            if (dragon.targetMineId.value != null) {
                dragon.targetMineId.value = null
                dragon.currentTarget?.value =
                    if (state.level.waypoints.isNotEmpty()) {
                        state.level.waypoints
                            .first()
                            .nextTarget
                    } else {
                        findClosestTargetPosition(dragon.position.value)
                    }
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Dragon ${dragon.id} no more mines, returning to closest target")
                }
            }
        }
    }

    fun checkMineWarning(dragon: Attacker) {
        if (dragon.type != AttackerType.DRAGON) return
        if (dragon.targetMineId.value == null || dragon.mineWarningShown.value) return

        val targetMine = state.defenders.find { it.id == dragon.targetMineId.value } ?: return
        val nextTurnNumber = dragon.dragonTurnsSinceSpawned.value + 1
        val nextTurnSpeed = if (nextTurnNumber % 2 == 1) 1 else 5
        val distance = dragon.position.value.distanceTo(targetMine.position.value)
        val canReachNextTurn = distance <= nextTurnSpeed

        if (canReachNextTurn) {
            if (!state.mineWarnings.contains(targetMine.id)) {
                state.mineWarnings.add(targetMine.id)
            }
            dragon.mineWarningShown.value = true
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "Warning: Dragon ${dragon.id} can reach mine ${targetMine.id} next turn! (distance: $distance, next speed: $nextTurnSpeed)",
                )
            }
        }
    }

    fun checkAndDestroyMine(dragon: Attacker) {
        if (dragon.type != AttackerType.DRAGON) return
        if (dragon.targetMineId.value == null) return

        val targetMine = state.defenders.find { it.id == dragon.targetMineId.value } ?: return
        val isAtMine = dragon.position.value == targetMine.position.value
        if (isAtMine && dragon.mineWarningShown.value) {
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Dragon ${dragon.id} destroys mine ${targetMine.id} at ${targetMine.position.value}")
            }

            val healthGain = AttackerType.DRAGON.health
            dragon.currentHealth.value += healthGain
            val oldLevelMine = dragon.level.value
            dragon.updateDragonLevel()
            recordDragonLevelChange(dragon, oldLevelMine)

            state.destroyedMinePositions.add(targetMine.position.value)
            state.defenders.remove(targetMine)
            state.mineWarnings.remove(targetMine.id)
            dragon.targetMineId.value = null
            dragon.mineWarningShown.value = false
        }
    }
}
