package de.egril.defender.model

/**
 * Enemy target selection and tower placement rules for [GameState].
 */
internal object TargetPlacementOps {
    /**
     * Returns true if [position] is a target that can still be reached by enemies.
     * Taken SINGLE_HIT targets are excluded.
     */
    fun isActiveTargetPosition(state: GameState, position: Position): Boolean {
        return with(state) {
            if (!level.isTargetPosition(position) || takenTargets.contains(position)) return false
            val nextOrderedTarget = getNextSingleHitTargetPosition()
            nextOrderedTarget == null || position == nextOrderedTarget
        }
    }

    /**
     * Returns the next unclaimed SINGLE_HIT target in the configured order, or null when ordering
     * is disabled or every ordered target has been claimed.
     */
    fun getNextSingleHitTargetPosition(state: GameState): Position? {
        return with(state) {
            level.singleHitTargetOrder.firstOrNull { target ->
                level.isTargetPosition(target) &&
                    level.targetInfoMap[target]?.type == TargetType.SINGLE_HIT &&
                    !takenTargets.contains(target)
            }
        }
    }

    /**
     * Returns the target positions enemies may currently attack.
     * While an ordered SINGLE_HIT target remains, it is the only available enemy destination.
     */
    fun getActiveTargetPositions(state: GameState): List<Position> {
        return with(state) {
            val nextOrderedTarget = getNextSingleHitTargetPosition()
            if (nextOrderedTarget != null) {
                listOf(nextOrderedTarget)
            } else {
                level.targetPositions.filter { !takenTargets.contains(it) }
            }
        }
    }

    /**
     * When a SINGLE_HIT target at [takenPosition] is taken, redirect enemies to the next
     * ordered target when sequencing is enabled, otherwise to their nearest remaining target.
     */
    fun retargetEnemiesFromTakenTarget(state: GameState, takenPosition: Position) {
        with(state) {
            val remaining = getActiveTargetPositions()
            if (remaining.isEmpty()) return // No active targets left – level will be lost
            val nextOrderedTarget = getNextSingleHitTargetPosition()
            for (enemy in attackers) {
                if (enemy.isDefeated.value) continue
                val newTarget =
                    nextOrderedTarget
                        ?: if (enemy.currentTarget?.value == takenPosition) {
                            remaining.minByOrNull { enemy.position.value.distanceTo(it) } ?: continue
                        } else {
                            continue
                        }
                enemy.currentTarget?.value = newTarget
                println("Enemy ${enemy.id} (${enemy.type}) retargeted from $takenPosition to $newTarget")
            }
        }
    }

    /**
     * Returns the effective next waypoint target, redirecting to the nearest active target
     * if the waypoint's next target is a taken SINGLE_HIT target.
     */
    fun resolveWaypointNextTarget(state: GameState, waypointNextTarget: Position, from: Position): Position {
        return with(state) {
            val nextOrderedTarget = getNextSingleHitTargetPosition()
            if (nextOrderedTarget != null && level.isTargetPosition(waypointNextTarget)) {
                nextOrderedTarget
            } else if (takenTargets.contains(waypointNextTarget)) {
                getActiveTargetPositions().minByOrNull { from.distanceTo(it) } ?: waypointNextTarget
            } else {
                waypointNextTarget
            }
        }
    }

    fun canPlaceDefender(state: GameState, type: DefenderType, extraCost: Int = 0): Boolean {
        return with(state) {
            type != DefenderType.ALTAR && (level.isSandbox || coins.value >= type.baseCost + extraCost) && level.availableTowers.contains(type)
        }
    }

    /**
     * Whether a tower buy button should be enabled. With auto tower bases, a tower on a PATH tile
     * costs the surcharge extra, so the button stays active only while an empty buildable tile
     * exists or the surcharge is also affordable.
     */
    fun canAffordTowerButton(state: GameState, type: DefenderType): Boolean {
        return with(state) {
            if (!canPlaceDefender(type)) return false
            if (!level.autoTowerBase || type == DefenderType.DWARVEN_MINE) return true
            val canAffordWithBase = level.isSandbox || coins.value >= type.baseCost + level.autoTowerBaseExtraCost
            canAffordWithBase || hasEmptyBuildableTile(state)
        }
    }

    /**
     * Positions offered for keyboard placement, in priority order: empty build areas and tower bases
     * first, then empty PATH tiles when the auto tower base option is enabled. Only positions the
     * player can afford are returned. The caller still validates each one through the normal
     * placement path, which also covers rules like spawn points, fiefs and gargoyles.
     */
    fun keyboardPlacementCandidates(state: GameState, type: DefenderType): List<Position> {
        return with(state) {
            val isFree: (Position) -> Boolean = { pos -> defenders.none { it.position.value == pos } }
            val buildable =
                (level.buildAreas + barricades.filter { it.canSupportTower() && !it.hasTower() }.map { it.position })
                    .filter(isFree)
                    .distinct()
                    .filter { canPlaceDefender(type) }
            if (!level.autoTowerBase || type == DefenderType.DWARVEN_MINE || type == DefenderType.ALTAR) {
                buildable
            } else {
                val path =
                    level.pathCells
                        .filter { isAutoTowerBasePathSpot(type, it) }
                        .sortedWith(compareBy({ it.y }, { it.x }))
                        .filter { canPlaceDefender(type, level.autoTowerBaseExtraCost) }
                buildable + path
            }
        }
    }

    /**
     * Auto tower base: whether a PATH tile can take a tower (with a tower base barricade underneath).
     * The tile must be free of any occupant (barricade, fief, enemy, trap, tower, inactive gargoyle)
     * and must not lie within [AUTO_TOWER_BASE_SPAWN_DISTANCE] of a spawn point.
     */
    fun isAutoTowerBasePathSpot(state: GameState, type: DefenderType, position: Position): Boolean {
        return with(state) {
            if (!level.autoTowerBase || type == DefenderType.ALTAR || type == DefenderType.DWARVEN_MINE) return false
            if (!level.isOnPath(position)) return false
            if (level.isSpawnPoint(position) || level.isTargetPosition(position) || level.isRiverTile(position)) return false
            if (level.startPositions.any { it.distanceTo(position) <= AUTO_TOWER_BASE_SPAWN_DISTANCE }) return false
            if (barricades.any { it.position == position }) return false
            if (fiefs.any { it.position == position }) return false
            if (traps.any { it.position == position }) return false
            if (defenders.any { it.position.value == position }) return false
            if (attackers.any { it.position.value == position }) return false
            if (submergedAttackers.any { it.position.value == position }) return false
            !isInactiveGargoyleAt(position)
        }
    }

    fun hasEmptyBuildableTile(state: GameState): Boolean {
        return with(state) {
            level.buildAreas.any { pos -> defenders.none { it.position.value == pos } } ||
                barricades.any { it.canSupportTower() && !it.hasTower() }
        }
    }
}
