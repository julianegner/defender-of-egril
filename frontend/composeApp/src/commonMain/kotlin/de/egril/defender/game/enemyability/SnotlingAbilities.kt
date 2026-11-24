package de.egril.defender.game

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.model.*
import kotlin.math.min

/**
 * Snotling swarm mechanics: growth, overflow spreading, horde eating and Gribnak's rally.
 * Extracted from [EnemyAbilitySystem].
 */
internal class SnotlingAbilities(
    private val state: GameState,
    private val swarm: SwarmAbilities,
    private val isWithinBounds: (Position) -> Boolean,
    private val maxSnotlingsPerTile: Int,
    private val maxSwarmSpawnSearchRings: Int,
) {
    private fun hasStandingBarricade(position: Position): Boolean =
        state.barricades.any { it.position == position && !it.isDestroyed() }

    fun processSnotlingGrowth() {
        val snotlings =
            state.attackers.filter {
                !it.isDefeated.value && it.type == AttackerType.SNOTLING
            }
        val growthNumerator = if (state.waaghFrenzyActive.value) 3 else 5
        val growthDenominator = if (state.waaghFrenzyActive.value) 2 else 4

        snotlings.forEach { snotling ->
            val grownCount = (snotling.currentHealth.value * growthNumerator) / growthDenominator
            if (grownCount > maxSnotlingsPerTile) {
                val overflow = grownCount - maxSnotlingsPerTile
                distributeSnotlingOverflow(snotling.position.value, overflow)
            }
            snotling.currentHealth.value = minOf(grownCount, maxSnotlingsPerTile)
        }
    }

    fun processHordeEating() {
        if (!state.level.waaghEnabled) return
        val attackersSnapshot = state.attackers.filter { !it.isDefeated.value }.toList()

        attackersSnapshot.forEach { attacker ->
            when (attacker.type) {
                AttackerType.ORK -> {
                    if (consumeNearbySnotlings(attacker, 10)) {
                        healAttacker(attacker, 10)
                        attacker.bloodlustRoundsLeft.value = maxOf(attacker.bloodlustRoundsLeft.value, 1)
                    }
                }
                AttackerType.OGRE -> {
                    if (consumeNearbySnotlings(attacker, 15)) {
                        healAttacker(attacker, 15)
                    }
                    eatGoblinOnSameTile(attacker)
                }
                else -> Unit
            }
        }
    }

    private fun consumeNearbySnotlings(
        eater: Attacker,
        requiredAmount: Int,
    ): Boolean {
        val candidatePositions =
            listOf(eater.position.value) +
                eater.position.value
                    .getHexNeighbors()
                    .filter { isWithinBounds(it) }
        val snotlingStacks =
            candidatePositions.mapNotNull { position ->
                state.attackers.find {
                    !it.isDefeated.value &&
                        it.type == AttackerType.SNOTLING &&
                        it.position.value == position
                }
            }
        val totalAvailable = snotlingStacks.sumOf { it.currentHealth.value }
        if (totalAvailable < requiredAmount) return false

        var remainingToEat = requiredAmount
        snotlingStacks.forEach { stack ->
            if (remainingToEat <= 0) return@forEach
            val eatenFromStack = min(stack.currentHealth.value, remainingToEat)
            stack.currentHealth.value -= eatenFromStack
            remainingToEat -= eatenFromStack
            if (stack.currentHealth.value <= 0) {
                stack.wasMerged.value = true
                stack.isDefeated.value = true
            }
        }
        state.addWaaghPoints(requiredAmount / 5)
        return true
    }

    private fun eatGoblinOnSameTile(ogre: Attacker) {
        val goblin =
            state.attackers.find {
                !it.isDefeated.value &&
                    it.id != ogre.id &&
                    it.type == AttackerType.GOBLIN &&
                    it.position.value == ogre.position.value
            } ?: return

        healAttacker(ogre, maxOf(10, goblin.currentHealth.value))
        goblin.wasMerged.value = true
        goblin.isDefeated.value = true
    }

    private fun healAttacker(
        attacker: Attacker,
        amount: Int,
    ) {
        attacker.currentHealth.value = minOf(attacker.maxHealth, attacker.currentHealth.value + amount)
    }

    private fun distributeSnotlingOverflow(
        origin: Position,
        overflowAmount: Int,
    ) {
        val pending = ArrayDeque<Pair<Position, Int>>()
        pending.add(origin to overflowAmount)

        while (pending.isNotEmpty()) {
            val (source, amount) = pending.removeFirst()
            if (amount <= 0) continue

            val validNeighbors =
                source.getHexNeighbors().filter { neighbor ->
                    isWithinBounds(neighbor) &&
                        state.level.isEnemyTraversable(neighbor) &&
                        !hasStandingBarricade(neighbor) &&
                        state.attackers.none {
                            !it.isDefeated.value &&
                                it.position.value == neighbor &&
                                it.type != AttackerType.SNOTLING
                        }
                }
            if (validNeighbors.isEmpty()) continue

            val distributed = mutableMapOf<Position, Int>()
            repeat(amount) { index ->
                val neighbor = validNeighbors[index % validNeighbors.size]
                distributed[neighbor] = (distributed[neighbor] ?: 0) + 1
            }

            distributed.forEach { (targetPosition, extraCount) ->
                val stack = getOrCreateSnotlingStack(targetPosition)
                stack.currentHealth.value += extraCount
                if (stack.currentHealth.value > maxSnotlingsPerTile) {
                    val overflow = stack.currentHealth.value - maxSnotlingsPerTile
                    stack.currentHealth.value = maxSnotlingsPerTile
                    pending.add(targetPosition to overflow)
                }
            }
        }
    }

    fun getOrCreateSnotlingStack(position: Position): Attacker {
        val existing =
            state.attackers.find {
                !it.isDefeated.value &&
                    it.type == AttackerType.SNOTLING &&
                    it.position.value == position
            }
        if (existing != null) return existing

        val initialTarget =
            state.level.getWaypointAt(position)?.let { waypoint ->
                state.resolveWaypointNextTarget(waypoint.nextTarget, position)
            } ?: state.getActiveTargetPositions().minByOrNull { position.distanceTo(it) }
                ?: state.level.targetPositions.first()

        return Attacker(
            id = state.nextAttackerId.value++,
            type = AttackerType.SNOTLING,
            position = mutableStateOf(position),
            level = mutableStateOf(1),
            currentHealth = mutableStateOf(0),
            currentTarget = mutableStateOf(initialTarget),
        ).also { state.attackers.add(it) }
    }

    /**
     * Snotling Rally: Gribnak the Squealer summons a rabble of very weak snotlings on every
     * path tile within distance 2. If a candidate tile is blocked by a non-snotling enemy or is
     * not a path tile at all, the snotling is redirected to the closest applicable tile instead
     * (tiles already occupied by snotlings are valid targets, since snotlings merge on arrival).
     */
    fun handleSnotlingRally(boss: Attacker) {
        if (boss.summonCooldown.value > 0) return

        val bossPos = boss.position.value
        // Collect all tiles within a distance of 2 (neighbours and neighbours-of-neighbours)
        val candidateTiles = mutableSetOf<Position>()
        for (neighbor in bossPos.getHexNeighbors()) {
            candidateTiles.add(neighbor)
            candidateTiles.addAll(neighbor.getHexNeighbors())
        }
        candidateTiles.remove(bossPos)

        val spawnPositions =
            swarm.resolveSwarmSpawnPositions(
                candidates = candidateTiles,
                stackableType = AttackerType.SNOTLING,
                isBaseTraversable = { state.level.isOnPath(it) },
                maxSearchRings = maxSwarmSpawnSearchRings,
            )

        if (spawnPositions.isEmpty()) return

        // Snotlings follow the same waypoint chain as Gribnak
        val inheritedTarget =
            boss.currentTarget?.value ?: if (state.level.waypoints.isNotEmpty()) {
                state.level.waypoints
                    .first()
                    .nextTarget
            } else {
                state.level.targetPositions.first()
            }

        for (spawnPos in spawnPositions) {
            swarm.summonSwarmUnit(
                type = AttackerType.SNOTLING,
                spawnPos = spawnPos,
                level = boss.level.value,
                currentTarget = inheritedTarget,
            )
        }

        boss.summonCooldown.value = 3
    }
}
