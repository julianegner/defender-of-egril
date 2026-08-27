package de.egril.defender.game

import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.AttackType
import de.egril.defender.model.Defender
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.model.getHexNeighborsWithinRadius

/**
 * Encapsulates GameEngine auto-attack target selection logic.
 */
class AutoAttackSelector(
    private val state: GameState,
    private val getEffectiveRange: (Defender) -> Int,
    private val findClosestTargetPosition: (Position) -> Position,
) {
    fun selectAutoTargetForDefender(
        defender: Defender,
        candidates: List<Attacker>,
    ): Attacker? {
        val attackable =
            candidates.filter { attacker ->
                !attacker.isDefeated.value &&
                    defender.canAttack(attacker, getEffectiveRange(defender)) &&
                    !state.isShieldWallAttackBlocked(defender, attacker) &&
                    canAutoAttackDamage(defender, attacker)
            }
        if (attackable.isEmpty()) return null

        return attackable.minWithOrNull(
            compareByDescending<Attacker> { threatScore(it) }
                .thenBy { estimateRemainingDistanceToGoal(it) }
                .thenBy { it.currentHealth.value }
                .thenBy { it.id },
        )
    }

    /**
     * Select the best position for an area or lasting attack.
     * Tries to maximize the number of enemies hit, prioritizing high-threat enemies.
     */
    fun selectBestAreaAttackPosition(
        defender: Defender,
        candidates: List<Attacker>,
    ): Position? {
        val radius = defender.areaEffectRadius

        // Collect all enemy positions that could potentially be in range
        val enemyPositions = candidates.filter { !it.isDefeated.value }.map { it.position.value }.toSet()

        // Find all valid attack positions (within defender's direct range)
        // These are positions we can target with our area attack
        val validAttackPositions = mutableSetOf<Position>()

        // Check all enemy positions and their neighbors as potential target positions
        for (enemyPos in enemyPositions) {
            // Add the enemy position itself
            val distance = defender.position.value.distanceTo(enemyPos)
            if (distance >= defender.type.minRange && distance <= defender.range) {
                if (state.level.isOnPath(enemyPos) || state.level.getRiverTile(enemyPos) != null || state.isBridgeAt(enemyPos)) {
                    validAttackPositions.add(enemyPos)
                }
            }

            // Also consider positions near the enemy (within area radius)
            // that are within our direct attack range
            if (radius > 0) {
                val nearbyPositions = enemyPos.getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                for (nearbyPos in nearbyPositions) {
                    val nearbyDistance = defender.position.value.distanceTo(nearbyPos)
                    if (nearbyDistance >= defender.type.minRange && nearbyDistance <= defender.range) {
                        if (state.level.isOnPath(nearbyPos) || state.level.getRiverTile(nearbyPos) != null || state.isBridgeAt(nearbyPos)) {
                            validAttackPositions.add(nearbyPos)
                        }
                    }
                }
            }
        }

        if (validAttackPositions.isEmpty()) return null

        // For each position, count how many enemies would be hit (considering area effect)
        val positionScores =
            validAttackPositions.map { targetPos ->
                val affectedPositions = mutableSetOf(targetPos)

                if (radius == 1) {
                    affectedPositions.addAll(
                        targetPos.getHexNeighbors().filter { neighbor ->
                            neighbor.x >= 0 &&
                                neighbor.x < state.level.gridWidth &&
                                neighbor.y >= 0 &&
                                neighbor.y < state.level.gridHeight &&
                                (state.level.isOnPath(neighbor) || state.isBridgeAt(neighbor))
                        },
                    )
                } else {
                    affectedPositions.addAll(
                        targetPos
                            .getHexNeighborsWithinRadius(radius, state.level.gridWidth, state.level.gridHeight)
                            .filter { state.level.isOnPath(it) || state.isBridgeAt(it) },
                    )
                }

                // Count enemies in affected area (considering immunities)
                val affectedEnemies =
                    candidates.filter { attacker ->
                        !attacker.isDefeated.value &&
                            affectedPositions.contains(attacker.position.value) &&
                            !state.isShieldWallAttackBlocked(defender, attacker) &&
                            canAutoAttackDamage(defender, attacker)
                    }

                // Calculate score: number of enemies hit + sum of threat scores + proximity to goal
                val enemyCount = affectedEnemies.size
                val totalThreat = affectedEnemies.sumOf { threatScore(it) }
                val avgDistanceToGoal =
                    if (affectedEnemies.isNotEmpty()) {
                        affectedEnemies.map { estimateRemainingDistanceToGoal(it) }.average()
                    } else {
                        Double.MAX_VALUE
                    }

                Triple(targetPos, enemyCount * 1000 + totalThreat, avgDistanceToGoal)
            }

        // Select position with highest score (most enemies + highest threat)
        // Tie-breaker: closest to goal
        return positionScores
            .filter { it.second > 0 }
            .maxWithOrNull(
                compareBy<Triple<Position, Int, Double>> { it.second }
                    .thenBy { -it.third }, // Negative because lower distance is better
            )?.first
    }

    private fun canAutoAttackDamage(
        defender: Defender,
        attacker: Attacker,
    ): Boolean =
        when (defender.type.attackType) {
            AttackType.MELEE, AttackType.RANGED -> {
                !attacker.type.immuneToNonMagicTowerDamage &&
                    !attacker.type.immuneToNonMagical &&
                    !(
                        attacker.type.immuneToBladeAttacks &&
                            (defender.type.attackType == AttackType.MELEE || defender.type.attackType == AttackType.RANGED)
                    )
            }
            AttackType.AREA -> attacker.canBeDamagedByFireball()
            AttackType.LASTING -> attacker.canBeDamagedByAcid() && !attacker.type.immuneToNonMagicTowerDamage
            AttackType.NONE -> false
        }

    private fun threatScore(attacker: Attacker): Int {
        // Higher score = higher priority
        return when (attacker.type) {
            AttackerType.EWHAD -> 100
            AttackerType.DRAGON -> 90
            AttackerType.UNDEAD_DRAGON -> 88
            AttackerType.PRINCE_VALERIUS_THE_SOULREAPER -> 88
            AttackerType.GREEN_WITCH -> 80
            AttackerType.RED_WITCH -> 75
            AttackerType.EVIL_WIZARD -> 65
            AttackerType.RED_DEMON -> 60
            AttackerType.BLUE_DEMON -> 55
            AttackerType.SILAS_THE_MASKMASTER,
            AttackerType.SILAS_MIRROR_IMAGE,
            -> 85
            else -> 0
        }
    }

    private fun estimateRemainingDistanceToGoal(attacker: Attacker): Int {
        val currentPos = attacker.position.value
        val nextGoal = attacker.currentTarget?.value
        val finalGoal = findClosestTargetPosition(currentPos)

        return if (nextGoal != null) {
            // Estimate remaining progress as: distance to nextGoal + heuristic distance from nextGoal to final goal
            currentPos.distanceTo(nextGoal) + nextGoal.distanceTo(finalGoal)
        } else {
            currentPos.distanceTo(finalGoal)
        }
    }
}
