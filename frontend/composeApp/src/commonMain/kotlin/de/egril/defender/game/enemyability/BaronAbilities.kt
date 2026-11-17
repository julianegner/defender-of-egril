package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*

/**
 * Baron Ratterzahn: hatches scrap piles into robotic goblins, drops new scrap piles after moving,
 * and fires rockets that disable towers.
 */
internal class BaronAbilities(
    private val state: GameState,
    private val isWithinBounds: (Position) -> Boolean,
    private val isTileOccupiedByStaticObject: (Position) -> Boolean,
) {
    private companion object {
        const val BARON_SCRAP_BOT_COUNT = 2
    }

    fun handleBaronRatterzahn(baron: Attacker) {
        hatchBaronScrapPiles(baron)
        val movedThisTurn = clearBaronScrapPilesAfterMovement(baron)
        dropBaronScrapPiles(baron, movedThisTurn)
        fireBaronRocket(baron)
    }

    private fun clearBaronScrapPilesAfterMovement(baron: Attacker): Boolean {
        val turnStartPosition = state.enemyTurnStartPositions[baron.id] ?: return false
        if (turnStartPosition == baron.position.value) return false
        state.scrapPiles.removeAll { it.ownerAttackerId == baron.id }
        return true
    }

    private fun hatchBaronScrapPiles(baron: Attacker) {
        val hatchable =
            state.scrapPiles.filter {
                it.ownerAttackerId == baron.id &&
                    it.hatchTurn <= state.turnNumber.value
            }
        if (hatchable.isEmpty()) return

        val inheritedTarget =
            baron.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }
        val usedPositions = mutableSetOf<Position>()
        for (scrapPile in hatchable) {
            val spawnPos = resolveRobotGoblinSpawn(scrapPile.position, usedPositions) ?: continue
            usedPositions.add(spawnPos)
            state.attackers.add(
                Attacker(
                    id = state.nextAttackerId.value++,
                    type = AttackerType.ROBOTIC_GOBLIN,
                    position = mutableStateOf(spawnPos),
                    level = mutableStateOf(baron.level.value),
                    currentTarget = mutableStateOf(inheritedTarget),
                ),
            )
            state.enemySpawnEffects.add(
                EnemySpawnEffect(
                    position = spawnPos,
                    turnNumber = state.turnNumber.value,
                    attackerType = AttackerType.ROBOTIC_GOBLIN,
                ),
            )
        }
        state.scrapPiles.removeAll(hatchable.toSet())
    }

    private fun resolveRobotGoblinSpawn(
        origin: Position,
        usedPositions: Set<Position>,
    ): Position? {
        val primaryCandidates = listOf(origin) + origin.getHexNeighbors()
        for (candidate in primaryCandidates) {
            if (isValidRobotGoblinSpawnTile(candidate, usedPositions)) {
                return candidate
            }
        }
        return origin
            .getHexNeighborsWithinRadius(
                radius = 3,
                gridWidth = state.level.gridWidth,
                gridHeight = state.level.gridHeight,
            ).firstOrNull { isValidRobotGoblinSpawnTile(it, usedPositions) }
    }

    private fun isValidRobotGoblinSpawnTile(
        position: Position,
        usedPositions: Set<Position>,
    ): Boolean {
        if (!isWithinBounds(position)) return false
        if (position in usedPositions) return false
        if (!state.level.isEnemyTraversable(position)) return false
        if (isTileOccupiedByStaticObject(position)) return false
        return state.attackers.none { !it.isDefeated.value && it.position.value == position }
    }

    private fun dropBaronScrapPiles(
        baron: Attacker,
        movedThisTurn: Boolean,
    ) {
        if (!movedThisTurn) return
        val droppedPositions = mutableSetOf<Position>()
        val candidates = baronScrapDropCandidates(baron)
        for (candidate in candidates) {
            if (droppedPositions.size >= BARON_SCRAP_BOT_COUNT) break
            if (!isValidScrapDropTile(candidate, droppedPositions)) continue
            droppedPositions.add(candidate)
            state.scrapPiles.add(
                ScrapPile(
                    position = candidate,
                    ownerAttackerId = baron.id,
                    hatchTurn = state.turnNumber.value + 1,
                ),
            )
        }
    }

    private fun baronScrapDropCandidates(baron: Attacker): List<Position> {
        val anchorTiles =
            buildSet {
                add(baron.position.value)
                state.enemyTurnStartPositions[baron.id]?.let { add(it) }
            }

        val candidates = mutableListOf<Position>()
        for (anchor in anchorTiles) {
            candidates.add(anchor)
            candidates.addAll(anchor.getHexNeighbors())
        }

        return candidates
            .filter { isWithinBounds(it) }
            .sortedWith(compareBy<Position> { it.hexDistanceTo(baron.position.value) }.thenBy { it.x }.thenBy { it.y })
            .distinct()
    }

    private fun isValidScrapDropTile(
        position: Position,
        droppedPositions: Set<Position>,
    ): Boolean {
        if (position in droppedPositions) return false
        if (state.scrapPiles.any { it.position == position }) return false
        if (isTileOccupiedByStaticObject(position)) return false
        if (!state.level.isEnemyTraversable(position)) return false
        val onOrNextToPath =
            state.level.isOnPath(position) ||
                state.level.isSpawnPoint(position) ||
                position.getHexNeighbors().any {
                    isWithinBounds(it) && (state.level.isOnPath(it) || state.level.isSpawnPoint(it))
                }
        return onOrNextToPath
    }

    private fun fireBaronRocket(baron: Attacker) {
        val cooldown = baron.type.towerDisableCooldown ?: return
        if (baron.villainCooldown.value > 0) {
            baron.villainCooldown.value--
            return
        }

        val range = (baron.type.towerDisableRangeBase ?: 0) + baron.level.value
        val disableDurationTurns = (baron.type.towerDisableDurationTurns ?: 0) + 1 // +1 because timers decrement at enemy-turn end

        val targetTower =
            state.defenders
                .filter { it.isReady && !it.isDisabled.value }
                .filter { baron.position.value.hexDistanceTo(it.position.value) <= range }
                .maxWithOrNull(
                    compareBy<Defender> { it.actualDamage }
                        .thenByDescending { baron.position.value.hexDistanceTo(it.position.value) },
                )

        if (targetTower != null) {
            targetTower.isDisabled.value = true
            targetTower.disabledTurnsRemaining.value = disableDurationTurns
            state.rocketAttackEffects.add(
                RocketAttackEffect(
                    sourcePosition = baron.position.value,
                    targetPosition = targetTower.position.value,
                    turnNumber = state.turnNumber.value,
                ),
            )
        }

        baron.villainCooldown.value = cooldown
    }
}
