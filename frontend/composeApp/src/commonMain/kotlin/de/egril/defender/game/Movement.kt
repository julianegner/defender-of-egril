package de.egril.defender.game

import de.egril.defender.config.LogConfig
import de.egril.defender.model.*

/**
 * Encapsulates enemy movement application and newly spawned movement simulation.
 */
data class EnemyTurnMovements(
    val allMovementSteps: List<List<Pair<Int, Position>>>,
    val attackersStoppedByBarricade: Set<Pair<Attacker, Position>>,
)

class Movement(
    private val state: GameState,
    private val pathfinding: PathfindingSystem,
    private val barricadeSystem: BarricadeSystem,
    private val bridgeSystem: BridgeSystem,
    private val enemyMovement: EnemyMovementSystem,
    private val enemyAbilities: EnemyAbilitySystem,
    private val mineOperations: MineOperations,
    private val attackBarricade: (Position, Attacker) -> Boolean,
    private val getBarricadeDamageForEnemyUnit: (Attacker) -> Int,
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
    fun calculateEnemyTurnMovements(): EnemyTurnMovements {
        val allMovementSteps = mutableListOf<List<Pair<Int, Position>>>()

        val allAttackers = state.attackers.filter { !it.isDefeated.value }
        val dragons = allAttackers.filter { it.type.isDragon }
        val floaters = allAttackers.filter { it.type.canFlyOverTerrain }
        val krakens = allAttackers.filter { it.type.canOnlyMoveOnWater }
        val regularAttackers =
            allAttackers
                .filter { !it.type.isDragon && !it.type.canFlyOverTerrain && !it.type.canOnlyMoveOnWater }
                .toMutableList()

        if (allAttackers.isEmpty()) return EnemyTurnMovements(allMovementSteps, emptySet())

        for (dragon in dragons) {
            val movementPath = enemyMovement.calculateDragonMovementPath(dragon)
            for (position in movementPath) {
                allMovementSteps.add(listOf(Pair(dragon.id, position)))
            }
        }

        for (floater in floaters) {
            val movementPath = enemyMovement.calculateFloatingMovementPath(floater)
            for (position in movementPath) {
                allMovementSteps.add(listOf(Pair(floater.id, position)))
            }
        }

        for (kraken in krakens) {
            val movementPath = enemyMovement.calculateKrakenMovementPath(kraken)
            for (position in movementPath) {
                allMovementSteps.add(listOf(Pair(kraken.id, position)))
            }
        }

        if (regularAttackers.isEmpty()) return EnemyTurnMovements(allMovementSteps, emptySet())

        val araxxaCanMoveThisTurn =
            regularAttackers
                .filter { it.type == AttackerType.ARAXXA }
                .associate { attacker ->
                    attacker.movementTurnsElapsed.value += 1
                    attacker.id to (attacker.movementTurnsElapsed.value % 2 == 1)
                }

        val alternatingCanMoveThisTurn =
            regularAttackers
                .filter { it.type.movesEveryOtherTurn }
                .associate { attacker ->
                    attacker.movementTurnsElapsed.value += 1
                    attacker.id to (attacker.movementTurnsElapsed.value % 2 == 1)
                }

        val currentPositions = mutableMapOf<Int, Position>()
        regularAttackers.forEach { currentPositions[it.id] = it.position.value }
        val attackersStoppedByBarricade = mutableSetOf<Pair<Attacker, Position>>()

        val fearedAttackerIds = mutableSetOf<Int>()
        regularAttackers.forEach { attacker ->
            val startPos = attacker.position.value
            val isFearedAtStart =
                state.activeSpellEffects.any { effect ->
                    (effect.spell == SpellType.FEAR_SPELL && effect.attackerId == attacker.id) ||
                        (
                            effect.spell == SpellType.FEAR_SPELL_AREA &&
                                effect.position != null &&
                                startPos.hexDistanceTo(effect.position) <= 2
                        )
                }
            if (isFearedAtStart) fearedAttackerIds.add(attacker.id)
        }

        val maxSpeed = regularAttackers.maxOfOrNull { calculateEffectiveEnemySpeed(it, it.position.value) } ?: 0
        val imminentBombs = state.activeSpellEffects.filter { it.spell == SpellType.BOMB && it.position != null && it.turnsRemaining <= 1 }
        // Track portals "used" during this pre-calculation so that each portal can only teleport
        // one unit per turn and the currentPositions update reflects the exit side for correct
        // subsequent path calculations.
        val virtuallyUsedPortals = mutableSetOf<Portal>()

        for (stepIndex in 0 until maxSpeed) {
            val movementsInThisStep = mutableListOf<Pair<Int, Position>>()
            val positionsToOccupy = mutableSetOf<Position>()

            for (attacker in regularAttackers) {
                val currentPos = currentPositions[attacker.id] ?: continue

                val isFrozen =
                    state.activeSpellEffects.any {
                        it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                    }
                if (isFrozen) continue

                if (attackersStoppedByBarricade.map { it.first }.map { it.id }.contains(attacker.id)) continue
                if (attacker.type == AttackerType.ARAXXA && araxxaCanMoveThisTurn[attacker.id] == false) continue

                if (attacker.type.movesEveryOtherTurn && alternatingCanMoveThisTurn[attacker.id] == false) {
                    val adjacentBarricadeTarget = findAdjacentBarricadeForStationaryAttack(attacker, currentPos)
                    if (adjacentBarricadeTarget != null) {
                        attackersStoppedByBarricade.add(Pair(attacker, adjacentBarricadeTarget))
                    }
                    continue
                }

                val effectiveSpeed = calculateEffectiveEnemySpeed(attacker, currentPos)
                if (stepIndex >= effectiveSpeed) continue

                if (attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    val bridgeablePositions = bridgeSystem.canBuildBridge(attacker)
                    if (bridgeablePositions.isNotEmpty() && bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt && attacker.isDefeated.value) continue
                    }
                }

                // Zythar stays near the back and only advances when a portal exit is close
                // to a target (≤ PORTAL_NEAR_TARGET_DISTANCE tiles). The pathfinding already
                // routes him through the nearest portal entry when one qualifies.
                if (attacker.type == AttackerType.ZYTHAR_THE_RIFTCALLER) {
                    val activeTargets = state.getActiveTargetPositions()
                    val hasQualifyingPortal = state.activePortals.any { portal ->
                        activeTargets.any { target ->
                            portal.exitPosition.hexDistanceTo(target) <= Portal.PORTAL_NEAR_TARGET_DISTANCE
                        }
                    }
                    if (!hasQualifyingPortal) continue
                }

                val isFeared = fearedAttackerIds.contains(attacker.id)
                val target =
                    if (isFeared) {
                        state.level.startPositions.minByOrNull { spawnPos -> currentPos.hexDistanceTo(spawnPos) }
                            ?: state.level.startPositions.first()
                    } else if (attacker.type == AttackerType.ZYTHAR_THE_RIFTCALLER) {
                        // Route Zythar to the entry of the qualifying portal whose EXIT is closest
                        // to a target, so he always picks the best portal rather than the nearest entry.
                        val activeTargets = state.getActiveTargetPositions()
                        val bestPortalEntry =
                            state.activePortals
                                .filter { portal ->
                                    activeTargets.any { t ->
                                        portal.exitPosition.hexDistanceTo(t) <= Portal.PORTAL_NEAR_TARGET_DISTANCE
                                    }
                                }
                                .minByOrNull { portal ->
                                    activeTargets.minOfOrNull { t -> portal.exitPosition.hexDistanceTo(t) }
                                        ?: Int.MAX_VALUE
                                }
                                ?.entryPosition
                        bestPortalEntry
                            ?: attacker.currentTarget?.value
                            ?: activeTargets.minByOrNull { currentPos.distanceTo(it) }
                            ?: state.level.targetPositions.first()
                    } else if (attacker.type == AttackerType.GREEN_WITCH) {
                        val healingTarget = enemyAbilities.findHealingTarget(attacker)
                        healingTarget?.position?.value
                            ?: attacker.currentTarget?.value
                            ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                            ?: state.level.targetPositions.first()
                    } else if (attacker.type == AttackerType.RED_WITCH || attacker.type == AttackerType.MORGUK_BONEWHISPER) {
                        enemyAbilities.findTowerTarget(attacker)
                            ?: attacker.currentTarget?.value
                            ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                            ?: state.level.targetPositions.first()
                    } else {
                        attacker.currentTarget?.value
                            ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                            ?: state.level.targetPositions.first()
                    }

                val nearbyBomb =
                    if (imminentBombs.isNotEmpty()) {
                        imminentBombs.find { effect ->
                            effect.position != null && currentPos.hexDistanceTo(effect.position) <= 3
                        }
                    } else {
                        null
                    }

                var path =
                    if (nearbyBomb?.position != null) {
                        val bombPos = nearbyBomb.position
                        val neighbors =
                            currentPos.getHexNeighbors().filter { neighbor ->
                                neighbor.x >= 0 &&
                                    neighbor.x < state.level.gridWidth &&
                                    neighbor.y >= 0 &&
                                    neighbor.y < state.level.gridHeight &&
                                    (state.level.isEnemyTraversable(neighbor)) &&
                                    state.barricades.none { b -> b.position == neighbor && !b.isDestroyed() } &&
                                    state.attackers.none { a -> !a.isDefeated.value && a.id != attacker.id && a.position.value == neighbor }
                            }
                        val fleeTo = neighbors.maxByOrNull { it.hexDistanceTo(bombPos) }
                        if (fleeTo != null && fleeTo.hexDistanceTo(bombPos) > currentPos.hexDistanceTo(bombPos)) {
                            listOf(currentPos, fleeTo)
                        } else {
                            pathfinding.findPath(currentPos, target, attacker)
                        }
                    } else {
                        pathfinding.findPath(currentPos, target, attacker)
                    }

                if (path.size < 2 && attacker.type.canBuildBridge && !attacker.isBuildingBridge.value) {
                    if (bridgeSystem.shouldAutoBuildBridge(attacker)) {
                        val bridgeBuilt = bridgeSystem.autoBuildBridge(attacker)
                        if (bridgeBuilt) {
                            if (attacker.isDefeated.value) continue
                            path = pathfinding.findPath(currentPos, target, attacker)
                        }
                    }
                }

                if (path.size < 2) {
                    val nextPos = pathfinding.moveTowards(currentPos, target, attacker)
                    if (nextPos != currentPos) {
                        val barricadeAtPos = barricadeSystem.getBarricadeAt(nextPos)
                        if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                            attackersStoppedByBarricade.add(Pair(attacker, nextPos))
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
                        attackersStoppedByBarricade.add(Pair(attacker, newPos))
                        continue
                    }
                }

                val trampledByAttacker =
                    if (attacker.type.canTrampleSmallerEnemies && !state.isActiveTargetPosition(newPos)) {
                        state.attackers.find { victim ->
                            !victim.isDefeated.value &&
                                victim.id != attacker.id &&
                                currentPositions[victim.id] == newPos &&
                                !victim.type.isVillain &&
                                !victim.type.isDragon &&
                                victim.type.health < AttackerType.ORK.health
                        }
                    } else {
                        null
                    }
                if (trampledByAttacker != null) {
                    trampledByAttacker.wasMerged.value = true
                    trampledByAttacker.isDefeated.value = true
                    currentPositions.remove(trampledByAttacker.id)
                }

                val isOccupied =
                    if (state.isActiveTargetPosition(newPos)) {
                        false
                    } else if (attacker.type.isSwarmUnit()) {
                        currentPositions.any { (id, pos) ->
                            id != attacker.id &&
                                pos == newPos &&
                                state.attackers.find { it.id == id }?.type != attacker.type
                        } ||
                            barricadeSystem.getBarricadeAt(newPos) != null
                    } else {
                        currentPositions.any { (id, pos) ->
                            id != attacker.id && pos == newPos
                        } ||
                            positionsToOccupy.contains(newPos) ||
                            barricadeSystem.getBarricadeAt(newPos) != null
                    }

                if (!isOccupied) {
                    // Portal pre-calculation: if the next step is a portal entry, record the unit
                    // as landing on the entry tile (one visual step) but track its effective position
                    // as the exit side so subsequent path calculations start from there.  This makes
                    // the full portal transit cost exactly one movement step.
                    val portalAtNewPos =
                        if (!attacker.type.canOnlyMoveOnWater) {
                            state.activePortals.firstOrNull { portal ->
                                portal.entryPosition == newPos &&
                                    !virtuallyUsedPortals.contains(portal) &&
                                    !portal.usedThisTurn.value
                            }
                        } else {
                            null
                        }

                    if (portalAtNewPos != null) {
                        // Find the best free exit-adjacent path tile (closest to any active target).
                        val exitNeighbors =
                            portalAtNewPos.exitPosition.getHexNeighbors().filter { neighbor ->
                                neighbor.x >= 0 &&
                                    neighbor.x < state.level.gridWidth &&
                                    neighbor.y >= 0 &&
                                    neighbor.y < state.level.gridHeight &&
                                    (state.level.isOnPath(neighbor) || state.level.isTargetPosition(neighbor)) &&
                                    !currentPositions.any { (id, pos) -> id != attacker.id && pos == neighbor } &&
                                    !positionsToOccupy.contains(neighbor)
                            }
                        val exitAdjacent =
                            exitNeighbors.minByOrNull { neighbor ->
                                state.getActiveTargetPositions().minOfOrNull { neighbor.hexDistanceTo(it) }
                                    ?: Int.MAX_VALUE
                            }
                        if (exitAdjacent != null) {
                            movementsInThisStep.add(Pair(attacker.id, newPos))
                            positionsToOccupy.add(newPos)   // prevent others from landing on the entry
                            currentPositions[attacker.id] = exitAdjacent
                            positionsToOccupy.add(exitAdjacent)
                            virtuallyUsedPortals.add(portalAtNewPos)
                            continue
                        }
                        // No free exit tile: fall through to normal movement onto the entry tile.
                    }

                    movementsInThisStep.add(Pair(attacker.id, newPos))
                    if (!state.isActiveTargetPosition(newPos)) {
                        positionsToOccupy.add(newPos)
                    }
                    currentPositions[attacker.id] = newPos
                } else {
                    val alternativePos = findAlternativePosition(currentPos, target, attacker.id, currentPositions, positionsToOccupy)
                    if (alternativePos != null) {
                        if (barricadeSystem.getBarricadeAt(alternativePos) == null) {
                            movementsInThisStep.add(Pair(attacker.id, alternativePos))
                            if (!state.isActiveTargetPosition(alternativePos)) {
                                positionsToOccupy.add(alternativePos)
                            }
                            currentPositions[attacker.id] = alternativePos
                        } else {
                            handleBarricades(currentPos, target, attacker, attackersStoppedByBarricade)
                        }
                    } else {
                        val barricadePos = pathfinding.moveTowards(currentPos, target, attacker)
                        if (barricadePos != currentPos) {
                            val barricadeAtPos = barricadeSystem.getBarricadeAt(barricadePos)
                            if (barricadeAtPos != null && !barricadeAtPos.isDestroyed()) {
                                attackersStoppedByBarricade.add(Pair(attacker, barricadePos))
                            }
                        }
                    }

                    if (barricadeSystem.getBarricadeAt(newPos) != null) {
                        attackersStoppedByBarricade.add(Pair(attacker, newPos))
                    }
                }
            }

            if (movementsInThisStep.isNotEmpty()) {
                allMovementSteps.add(movementsInThisStep)
            }
        }

        return EnemyTurnMovements(allMovementSteps, attackersStoppedByBarricade)
    }

    private fun findAdjacentBarricadeForStationaryAttack(
        attacker: Attacker,
        currentPos: Position,
    ): Position? {
        val target =
            attacker.currentTarget?.value
                ?: state.getActiveTargetPositions().minByOrNull { currentPos.distanceTo(it) }
                ?: state.level.targetPositions.firstOrNull()
                ?: return null
        val adjacentBarricades =
            currentPos.getHexNeighbors().mapNotNull { neighbor ->
                if (neighbor.x < 0 ||
                    neighbor.x >= state.level.gridWidth ||
                    neighbor.y < 0 ||
                    neighbor.y >= state.level.gridHeight
                ) {
                    null
                } else {
                    val barricade = barricadeSystem.getBarricadeAt(neighbor)
                    if (barricade != null && !barricade.isDestroyed()) {
                        Pair(neighbor, barricade)
                    } else {
                        null
                    }
                }
            }
        if (adjacentBarricades.isEmpty()) return null

        return adjacentBarricades
            .minWithOrNull(
                compareBy<Pair<Position, Barricade>> { it.first.distanceTo(target) }
                    .thenBy { if (it.second.hasTower()) 0 else 1 },
            )?.first
    }

    private fun handleBarricades(
        currentPos: Position,
        target: Position,
        attacker: Attacker,
        attackersStoppedByBarricade: MutableSet<Pair<Attacker, Position>>,
    ) {
        val barricadePathSet = mutableSetOf<Pair<Barricade, List<Position>>>()
        val pathIgnoringBarricades = pathfinding.findPath(currentPos, target, attacker, ignoreBarricades = true)
        if (pathIgnoringBarricades.size > 1) {
            val barricadeAtPath = barricadeSystem.getBarricadeAt(pathIgnoringBarricades[1])
            if (barricadeAtPath != null) {
                barricadePathSet.add(Pair(barricadeAtPath, pathIgnoringBarricades))
            }
        }

        val excludedPositions = mutableSetOf<Position>()
        val maxIterations = 10
        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            val currentPath = pathfinding.findPath(currentPos, target, attacker, excludedPositions)
            if (currentPath.size < 2) break
            val nextPos = currentPath[1]
            val barricadeAtNextPos = barricadeSystem.getBarricadeAt(nextPos)

            if (barricadeAtNextPos != null) {
                barricadePathSet.add(Pair(barricadeAtNextPos, currentPath))
                excludedPositions.add(nextPos)
            } else {
                if (currentPath.last() == target) {
                    break
                } else {
                    excludedPositions.add(nextPos)
                }
            }
        }

        var min = Int.MAX_VALUE
        var pathOfLeastEffort: Pair<Barricade, List<Position>>? = null
        barricadePathSet.forEach { (barricade, path) ->
            val hp =
                if (barricade.hasTower()) {
                    barricade.healthPoints.value - 100
                } else {
                    barricade.healthPoints.value
                }
            val pathSteps = path.size - 1
            val effort = (hp / getBarricadeDamageForEnemyUnit(attacker)) + pathSteps
            if (effort < min) {
                min = effort
                pathOfLeastEffort = Pair(barricade, path)
            }
        }

        if (pathOfLeastEffort != null) {
            attackersStoppedByBarricade.add(Pair(attacker, pathOfLeastEffort.first.position))
        }
    }

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
                } else {
                    // Portal teleportation: if the unit just stepped onto a portal entry tile,
                    // transport it to a free path tile adjacent to the portal exit.
                    applyPortalTeleportation(attacker)
                    // Demonling portal creation: if a demonling has advanced far enough, sacrifice
                    // it to open a new rift portal.
                    if (!attacker.isDefeated.value && attacker.type == AttackerType.DEMONLING) {
                        enemyAbilities.checkAndCreatePortalForDemonling(attacker)
                    }
                }
            }
        }
    }

    /**
     * If [attacker] is standing on an active portal entry tile, teleport it to the best free path
     * tile adjacent to the portal exit.  The portal entry is consumed (removed) after use so that
     * multiple units cannot chain-teleport through the same portal in a single turn.
     *
     * Also handles Zythar himself stepping through his portal: if a ZYTHAR_THE_RIFTCALLER unit is
     * on a portal entry and the exit is within [Portal.PORTAL_NEAR_TARGET_DISTANCE] of any target,
     * he is teleported too (regardless of who owns the portal).
     */
    private fun applyPortalTeleportation(attacker: Attacker) {
        val pos = attacker.position.value
        // Find a portal whose entry matches this attacker's current position and has not yet
        // been used this turn (one use per portal per enemy turn to prevent chain-teleportation).
        val portal = state.activePortals.firstOrNull { it.entryPosition == pos && !it.usedThisTurn.value } ?: return
        // Zythar only uses portals that lead close to a target; other enemies always teleport.
        if (attacker.type == AttackerType.ZYTHAR_THE_RIFTCALLER) {
            val minTargetDist =
                state.getActiveTargetPositions().minOfOrNull { portal.exitPosition.hexDistanceTo(it) }
                    ?: return
            if (minTargetDist > Portal.PORTAL_NEAR_TARGET_DISTANCE) return
        }

        // Find the best free path tile adjacent to the exit.
        val exitNeighbors =
            portal.exitPosition.getHexNeighbors()
                .filter { neighbor ->
                    state.level.isOnPath(neighbor) &&
                        !state.attackers.any { it.id != attacker.id && !it.isDefeated.value && it.position.value == neighbor }
                }
        val destination = exitNeighbors.minByOrNull { neighbor ->
            state.getActiveTargetPositions().minOfOrNull { neighbor.hexDistanceTo(it) } ?: Int.MAX_VALUE
        } ?: return

        attacker.position.value = destination
        // Update the attacker's waypoint target if needed at the new position.
        updateWaypointTargetIfReached(attacker, destination, "Portal teleport")
        // Mark the portal as used for this turn so no second unit can chain through it.
        portal.usedThisTurn.value = true
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
                            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                println(
                                    "Newly spawned unit ${attacker.id} (${attacker.type}) built bridge at $currentPos during movement at turn ${state.turnNumber.value}",
                                )
                            }
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
                            if (LogConfig.ENABLE_GAME_STATE_LOGGING) {
                                println(
                                    "Newly spawned unit ${attacker.id} (${attacker.type}) built bridge (fallback) during movement at turn ${state.turnNumber.value}",
                                )
                            }
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

    private fun calculateEffectiveEnemySpeed(
        attacker: Attacker,
        currentPos: Position,
    ): Int = de.egril.defender.game.calculateEffectiveEnemySpeed(state, attacker, currentPos)

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
