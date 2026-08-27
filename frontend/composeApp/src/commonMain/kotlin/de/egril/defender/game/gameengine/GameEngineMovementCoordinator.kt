package de.egril.defender.game.gameengine

import de.egril.defender.config.LogConfig
import de.egril.defender.game.BarricadeSystem
import de.egril.defender.game.BridgeSystem
import de.egril.defender.game.EnemyAbilitySystem
import de.egril.defender.game.MineOperations
import de.egril.defender.game.PathfindingSystem
import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.EnemyMoveEffect
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.isSwarmUnit

/**
 * Encapsulates enemy movement application and newly spawned movement simulation.
 */
class GameEngineMovementCoordinator(
    private val state: GameState,
    private val pathfinding: PathfindingSystem,
    private val barricadeSystem: BarricadeSystem,
    private val bridgeSystem: BridgeSystem,
    private val enemyAbilities: EnemyAbilitySystem,
    private val mineOperations: MineOperations,
    private val calculateEffectiveEnemySpeed: (Attacker, Position) -> Int,
    private val attackBarricade: (Position, Attacker) -> Boolean,
    private val applyTargetDamage: (Attacker) -> Unit,
    private val updateDragonMineTargeting: (Attacker) -> Unit,
    private val checkMineWarning: (Attacker) -> Unit,
    private val checkAndDestroyMine: (Attacker) -> Unit,
    private val processDragonGreed: (Attacker) -> Unit,
    private val applyOrkFrenzyTowerAttack: (Attacker) -> Unit,
    private val destroyFiefAt: (Position, Attacker) -> Unit,
    private val consumeMushroomAt: (Position, Attacker) -> Unit,
    private val recordDragonLevelChange: (Attacker, Int) -> Unit,
) {
    fun applyMovement(
        attackerId: Int,
        newPosition: Position,
    ) {
        val attacker = state.attackers.find { it.id == attackerId } ?: return
        if (attacker.isDefeated.value) return
        if (attackBarricade(newPosition, attacker)) return

        if (attacker.type.isDragon) {
            applyDragonMovement(attacker, newPosition)
            return
        }

        if (attacker.type.isSwarmUnit() && !state.isActiveTargetPosition(newPosition)) {
            val existingSwarmUnit =
                state.attackers.find {
                    it.id != attacker.id &&
                        !it.isDefeated.value &&
                        it.position.value == newPosition &&
                        it.type == attacker.type
                }
            if (existingSwarmUnit != null) {
                existingSwarmUnit.currentHealth.value += attacker.currentHealth.value
                attacker.wasMerged.value = true
                attacker.isDefeated.value = true
                return
            }
        }

        val isOccupied =
            if (state.isActiveTargetPosition(newPosition)) {
                false
            } else {
                state.attackers.any {
                    it.id != attacker.id && !it.isDefeated.value && it.position.value == newPosition
                }
            }

        if (!isOccupied) {
            val oldPosition = attacker.position.value
            attacker.position.value = newPosition
            if (oldPosition != newPosition) {
                applyTileEffectsForArrival(attacker, newPosition)
            }

            if (!attacker.isDefeated.value) {
                applyOrkFrenzyTowerAttack(attacker)
                updateWaypointTargetIfReached(attacker, newPosition, "Attacker")
                if (state.isActiveTargetPosition(newPosition)) {
                    applyTargetDamage(attacker)
                }
            }
        }
    }

    fun calculateNewlySpawnedMovements(): List<List<Pair<Int, Position>>> {
        val allMovementSteps = mutableListOf<List<Pair<Int, Position>>>()

        val newlySpawned =
            state.attackers
                .filter { attacker ->
                    !attacker.isDefeated.value && state.level.isSpawnPoint(attacker.position.value)
                }.toMutableList()

        if (newlySpawned.isEmpty()) return allMovementSteps

        val currentPositions = mutableMapOf<Int, Position>()
        newlySpawned.forEach { currentPositions[it.id] = it.position.value }
        val attackersStoppedByBarricade = mutableSetOf<Int>()
        val maxSpeed = newlySpawned.maxOfOrNull { calculateEffectiveEnemySpeed(it, it.position.value) } ?: 0

        for (stepIndex in 0 until maxSpeed) {
            val movementsInThisStep = mutableListOf<Pair<Int, Position>>()
            val positionsToOccupy = mutableSetOf<Position>()

            for (attacker in newlySpawned) {
                val currentPos = currentPositions[attacker.id] ?: continue
                if (attackersStoppedByBarricade.contains(attacker.id)) continue

                val effectiveSpeed = calculateEffectiveEnemySpeed(attacker, currentPos)
                if (stepIndex >= effectiveSpeed) continue

                if (attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    val bridgeablePositions = bridgeSystem.canBuildBridge(attacker)
                    if (bridgeablePositions.isNotEmpty() && bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            println(
                                "Newly spawned unit ${attacker.id} (${attacker.type}) built bridge at $currentPos during movement at turn ${state.turnNumber.value}",
                            )
                            if (attacker.isDefeated.value) {
                                continue
                            }
                        }
                    }
                }

                val target =
                    if (attacker.type == AttackerType.GREEN_WITCH) {
                        val healingTarget = enemyAbilities.findHealingTarget(attacker)
                        if (healingTarget != null) {
                            healingTarget.position.value
                        } else {
                            attacker.currentTarget?.value
                                ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                                ?: state.level.targetPositions.first()
                        }
                    } else if (attacker.type == AttackerType.RED_WITCH || attacker.type == AttackerType.MORGUK_BONEWHISPER) {
                        val towerTarget = enemyAbilities.findTowerTarget(attacker)
                        if (towerTarget != null) {
                            towerTarget
                        } else {
                            attacker.currentTarget?.value
                                ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                                ?: state.level.targetPositions.first()
                        }
                    } else {
                        attacker.currentTarget?.value
                            ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                            ?: state.level.targetPositions.first()
                    }
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println(
                        "Newly spawned attacker ${attacker.id} at $currentPos pathing to target: $target (currentTarget: ${attacker.currentTarget?.value})",
                    )
                }
                var path = pathfinding.findPath(currentPos, target, attacker)

                if (path.size < 2 && attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    if (bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            println(
                                "Newly spawned unit ${attacker.id} (${attacker.type}) built bridge (fallback) during movement at turn ${state.turnNumber.value}",
                            )
                            if (attacker.isDefeated.value) {
                                continue
                            }
                            path = pathfinding.findPath(currentPos, target, attacker)
                        }
                    }
                }

                if (path.size < 2) {
                    val nextPos = pathfinding.moveTowards(currentPos, target, attacker)
                    if (nextPos != currentPos) {
                        val barricadeAtPos = barricadeSystem.getBarricadeAt(nextPos)
                        if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                            attackersStoppedByBarricade.add(attacker.id)
                            continue
                        }
                    }
                    continue
                }

                val newPos = path[1]
                val isFlying = attacker.isFlying.value == true
                if (!isFlying) {
                    val barricadeAtNewPos = barricadeSystem.getBarricadeAt(newPos)
                    if (barricadeAtNewPos != null && !barricadeAtNewPos.isDestroyed()) {
                        attackersStoppedByBarricade.add(attacker.id)
                    }
                }

                val isOccupied = isOccupiedInSpawnSimulation(attacker, newPos, currentPositions, positionsToOccupy)
                if (!isOccupied) {
                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    if (!state.isActiveTargetPosition(newPos)) {
                        positionsToOccupy.add(newPos)
                    }
                    currentPositions[attacker.id] = newPos
                    updateWaypointTargetIfReached(attacker, newPos, "Attacker")
                } else {
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        movementsInThisStep.add(Pair(attacker.id, alternativePos))
                        if (!state.isActiveTargetPosition(alternativePos)) {
                            positionsToOccupy.add(alternativePos)
                        }
                        currentPositions[attacker.id] = alternativePos
                        updateWaypointTargetIfReached(attacker, alternativePos, "Attacker")
                    } else {
                        val barricadePos = pathfinding.moveTowards(currentPos, target, attacker)
                        if (barricadePos != currentPos) {
                            val barricadeAtPos = barricadeSystem.getBarricadeAt(barricadePos)
                            if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                                attackersStoppedByBarricade.add(attacker.id)
                                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                    println(
                                        "CETM -C---------------------- attackersStoppedByBarricade Newly spawned unit ${attacker.id} (${attacker.type}) at $currentPos blocked by other units, will attack optimal barricade at $barricadePos (HP: ${barricadeAtPos.healthPoints.value})",
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (movementsInThisStep.isNotEmpty()) {
                allMovementSteps.add(movementsInThisStep)
            }
        }

        return allMovementSteps
    }

    private fun applyDragonMovement(
        attacker: Attacker,
        newPosition: Position,
    ) {
        val unitAtPosition =
            state.attackers.find {
                it.id != attacker.id && !it.isDefeated.value && it.position.value == newPosition
            }

        if (attacker.type == AttackerType.DRAGON && unitAtPosition != null && unitAtPosition.type != AttackerType.EWHAD) {
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println(
                    "Dragon ${attacker.id} eating ${unitAtPosition.type} at $newPosition, gaining ${unitAtPosition.currentHealth.value} HP",
                )
            }
            attacker.currentHealth.value += unitAtPosition.currentHealth.value
            val oldLevelEating = attacker.level.value
            attacker.updateDragonLevel()
            recordDragonLevelChange(attacker, oldLevelEating)
            unitAtPosition.isDefeated.value = true

            addMovementTrail(attacker)
            attacker.position.value = newPosition
            applyTileEffectsForArrival(attacker, newPosition)

            if (!attacker.isDefeated.value) {
                updateWaypointTargetIfReached(attacker, newPosition, "Dragon")
            }
        } else if (unitAtPosition != null && unitAtPosition.type == AttackerType.EWHAD) {
            val alternatePos =
                newPosition
                    .getHexNeighbors()
                    .filter { pos ->
                        state.level.isOnPath(pos) &&
                            state.attackers.none { it.position.value == pos && !it.isDefeated.value }
                    }.minByOrNull {
                        it.distanceTo(
                            state.getActiveTargetPositions().minByOrNull { t -> newPosition.distanceTo(t) }
                                ?: state.level.targetPositions.first(),
                        )
                    }

            if (alternatePos != null) {
                println("Dragon ${attacker.id} can't land on Ewhad, moving to alternate position $alternatePos")
                addMovementTrail(attacker)
                attacker.position.value = alternatePos
                mineOperations.checkAndActivateTrapForAttacker(attacker)

                if (!attacker.isDefeated.value) {
                    updateWaypointTargetIfReached(attacker, alternatePos, "Dragon")
                }
            } else {
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println("Dragon ${attacker.id} blocked by Ewhad at $newPosition, staying in place")
                }
            }
        } else {
            addMovementTrail(attacker)
            attacker.position.value = newPosition
            applyTileEffectsForArrival(attacker, newPosition)
        }

        updateWaypointTargetIfReached(attacker, attacker.position.value, "Dragon")
        if (state.isActiveTargetPosition(attacker.position.value)) {
            applyTargetDamage(attacker)
        }

        if (!attacker.isDefeated.value) {
            updateDragonMineTargeting(attacker)
            checkMineWarning(attacker)
            checkAndDestroyMine(attacker)
            processDragonGreed(attacker)
        }
    }

    private fun addMovementTrail(attacker: Attacker) {
        val oldPosition = attacker.position.value
        if (state.enemyMoveEffects.none { it.position == oldPosition }) {
            state.enemyMoveEffects.add(EnemyMoveEffect(oldPosition, state.turnNumber.value))
        }
    }

    private fun applyTileEffectsForArrival(
        attacker: Attacker,
        position: Position,
    ) {
        mineOperations.checkAndActivateTrapForAttacker(attacker)
        destroyFiefAt(position, attacker)
        consumeMushroomAt(position, attacker)
    }

    private fun updateWaypointTargetIfReached(
        attacker: Attacker,
        position: Position,
        label: String,
    ) {
        if (state.level.isWaypoint(position) && attacker.currentTarget?.value == position) {
            val waypoint = state.level.getWaypointAt(position)
            if (waypoint != null) {
                attacker.currentTarget.value = state.resolveWaypointNextTarget(waypoint.nextTarget, position)
                if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                    println(
                        "$label ${attacker.id} reached waypoint at $position, next target: ${attacker.currentTarget.value}",
                    )
                }
            }
        }
    }

    private fun isOccupiedInSpawnSimulation(
        attacker: Attacker,
        newPos: Position,
        currentPositions: Map<Int, Position>,
        positionsToOccupy: Set<Position>,
    ): Boolean =
        if (state.isActiveTargetPosition(newPos)) {
            false
        } else if (attacker.type.isSwarmUnit()) {
            state.attackers.any {
                it.id != attacker.id &&
                    !it.isDefeated.value &&
                    it.position.value == newPos &&
                    it.type != attacker.type
            } ||
                currentPositions.any { (id, pos) ->
                    id != attacker.id &&
                        pos == newPos &&
                        state.attackers.find { it.id == id }?.type != attacker.type
                } ||
                barricadeSystem.getBarricadeAt(newPos) != null
        } else {
            state.attackers.any {
                it.id != attacker.id && !it.isDefeated.value && it.position.value == newPos
            } ||
                currentPositions.any { (id, pos) ->
                    id != attacker.id && pos == newPos
                } ||
                positionsToOccupy.contains(newPos)
        }

    private fun findAlternativePosition(
        currentPos: Position,
        target: Position,
        attackerId: Int,
        currentPositions: Map<Int, Position>,
        positionsToOccupy: Set<Position>,
    ): Position? {
        val currentDistance = currentPos.distanceTo(target)
        val neighbors = currentPos.getHexNeighbors()
        val validNeighbors =
            neighbors.filter { neighbor ->
                neighbor.x >= 0 &&
                    neighbor.x < state.level.gridWidth &&
                    neighbor.y >= 0 &&
                    neighbor.y < state.level.gridHeight &&
                    state.level.isOnPath(neighbor)
            }

        val availableNeighbors =
            validNeighbors.filter { neighbor ->
                val isOccupied =
                    state.attackers.any {
                        it.id != attackerId && !it.isDefeated.value && it.position.value == neighbor
                    } ||
                        currentPositions.any { (id, pos) ->
                            id != attackerId && pos == neighbor
                        } ||
                        positionsToOccupy.contains(neighbor)
                !isOccupied
            }

        if (availableNeighbors.isEmpty()) return null
        val movingCloser = availableNeighbors.filter { it.distanceTo(target) < currentDistance }
        if (movingCloser.isNotEmpty()) {
            return movingCloser.minByOrNull { it.distanceTo(target) }
        }

        val sameDist = availableNeighbors.filter { it.distanceTo(target) == currentDistance }
        if (sameDist.isNotEmpty()) {
            return sameDist.first()
        }

        if (state.level.isSpawnPoint(currentPos)) {
            return availableNeighbors.minByOrNull { it.distanceTo(target) }
        }

        return null
    }
}
