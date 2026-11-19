package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*

/**
 * If [attacker] is standing on an active portal entry tile, teleport it to the best free path
 * tile adjacent to the portal exit.
 *
 * Zythar also steps through portals once he reaches an entry tile.
 */
internal fun Movement.applyPortalTeleportation(attacker: Attacker): Boolean {
    val pos = attacker.position.value
    val portal = state.getPortalAtEntry(pos) ?: return true
    if (portal.usedThisTurn.value) return false
    // Find the best free path tile adjacent to the exit.
    val exitNeighbors =
        portal.exitPosition
            .getHexNeighbors()
            .filter { neighbor ->
                (state.level.isOnPath(neighbor) || state.isBridgeAt(neighbor)) &&
                    !state.isPortalTile(neighbor) &&
                    !state.isInactiveGargoyleAt(neighbor) &&
                    !state.attackers.any { it.id != attacker.id && !it.isDefeated.value && it.position.value == neighbor }
            }
    val destination =
        exitNeighbors.minByOrNull { neighbor ->
            state.getActiveTargetPositions().minOfOrNull { neighbor.hexDistanceTo(it) } ?: Int.MAX_VALUE
        } ?: return false

    attacker.position.value = destination
    attacker.teleportedThisTurn.value = true
    // Update the attacker's waypoint target if needed at the new position.
    updateWaypointTargetIfReached(attacker, destination, "Portal teleport")
    // Mark the portal as used for this turn so no second unit can chain through it.
    portal.usedThisTurn.value = true
    return true
}

internal fun Movement.findZytharSafeSpot(
    from: Position,
    attackerId: Int,
): Position {
    val activeTargets = state.getActiveTargetPositions()
    if (activeTargets.isEmpty()) return from

    val candidates =
        (from.getHexNeighbors() + listOf(from))
            .filter { pos ->
                pos.x >= 0 &&
                    pos.x < state.level.gridWidth &&
                    pos.y >= 0 &&
                    pos.y < state.level.gridHeight &&
                    (state.level.isOnPath(pos) || state.level.isTargetPosition(pos) || state.isBridgeAt(pos)) &&
                    !state.isPortalTile(pos) &&
                    !state.attackers.any { it.id != attackerId && !it.isDefeated.value && it.position.value == pos } &&
                    !state.barricades.any { it.position == pos && !it.isDestroyed() }
            }
    if (candidates.isEmpty()) return from

    return candidates.maxWithOrNull(
        compareBy<Position> { candidate ->
            activeTargets.minOfOrNull { target -> candidate.hexDistanceTo(target) } ?: Int.MIN_VALUE
        }.thenBy { -from.hexDistanceTo(it) },
    ) ?: from
}

internal fun Movement.calculateEffectiveEnemySpeed(
    attacker: Attacker,
    currentPos: Position,
): Int =
    de.egril.defender.game
        .calculateEffectiveEnemySpeed(state, attacker, currentPos)

internal fun Movement.applyDragonMovement(
    attacker: Attacker,
    newPosition: Position,
) {
    if (state.isInactiveGargoyleAt(newPosition)) return

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
                    (state.level.isOnPath(pos) || state.isBridgeAt(pos)) &&
                        state.attackers.none { it.position.value == pos && !it.isDefeated.value } &&
                        !state.isInactiveGargoyleAt(pos)
                }.minByOrNull {
                    it.distanceTo(
                        state.getActiveTargetPositions().minByOrNull { t -> newPosition.distanceTo(t) }
                            ?: state.level.targetPositions.first(),
                    )
                }

        if (alternatePos != null) {
            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                println("Dragon ${attacker.id} can't land on Ewhad, moving to alternate position $alternatePos")
            }
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

internal fun Movement.addMovementTrail(attacker: Attacker) {
    val oldPosition = attacker.position.value
    if (state.enemyMoveEffects.none { it.position == oldPosition }) {
        state.enemyMoveEffects.add(EnemyMoveEffect(oldPosition, state.turnNumber.value))
    }
}

internal fun Movement.applyTileEffectsForArrival(
    attacker: Attacker,
    position: Position,
) {
    mineOperations.checkAndActivateTrapForAttacker(attacker)
    destroyFiefAt(position, attacker)
    consumeMushroomAt(position, attacker)
}

internal fun Movement.updateWaypointTargetIfReached(
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

internal fun Movement.isOccupiedInSpawnSimulation(
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

internal fun Movement.findAlternativePosition(
    currentPos: Position,
    target: Position,
    attackerId: Int,
    currentPositions: Map<Int, Position>,
    positionsToOccupy: Set<Position>,
    canUseRiver: Boolean = false,
): Position? {
    val currentDistance = currentPos.distanceTo(target)
    val neighbors = currentPos.getHexNeighbors()
    val validNeighbors =
        neighbors.filter { neighbor ->
            neighbor.x >= 0 &&
                neighbor.x < state.level.gridWidth &&
                neighbor.y >= 0 &&
                neighbor.y < state.level.gridHeight &&
                (
                    state.level.isOnPath(neighbor) ||
                        state.isBridgeAt(neighbor) ||
                        // River-traversal units (e.g. Cap'n Roderich, Pirate) must also be able to
                        // sidestep onto river tiles here, or they get stuck in place forever whenever
                        // their primary next tile is occupied by another attacker congesting near the
                        // water's edge (see issue: pirates stall right at the shoreline).
                        (canUseRiver && state.level.isRiverTile(neighbor))
                )
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
                    positionsToOccupy.contains(neighbor) ||
                    state.isInactiveGargoyleAt(neighbor)
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
