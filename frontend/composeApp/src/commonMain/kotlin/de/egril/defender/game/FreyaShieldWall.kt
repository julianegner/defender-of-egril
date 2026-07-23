package de.egril.defender.game

import de.egril.defender.model.Attacker
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Defender
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.model.getHexDirectionTo
import de.egril.defender.model.getHexNeighbor
import de.egril.defender.model.hexDistanceTo

private const val HEX_DIRECTION_COUNT = 6
private val SHIELD_WALL_FRONT_ARC_OFFSETS = listOf(0, 1, -1)
private val SHIELD_WALL_FLANK_OFFSETS = listOf(2, -2)

fun GameState.isShieldWallAttackBlocked(
    defender: Defender,
    target: Attacker,
): Boolean = isShieldWallAttackBlocked(defender, target.position.value)

fun GameState.isShieldWallAttackBlocked(
    defender: Defender,
    targetPosition: Position,
): Boolean {
    if (defender.type == de.egril.defender.model.DefenderType.BALLISTA_TOWER) return false

    val pathfinding = PathfindingSystem(this)
    return attackers.any { freya ->
        !freya.isDefeated.value &&
            freya.type == AttackerType.FALLEN_SHIELDMAIDEN_FREYA &&
            freya.type.shieldWallFormationWidth > 0 &&
            isProtectedByFreyaShieldWall(
                freya = freya,
                protectedPosition = targetPosition,
                attackOrigin = defender.position.value,
                pathfinding = pathfinding,
            )
    }
}

fun GameState.freyaShieldWallVisiblePositions(): Set<Position> {
    val pathfinding = PathfindingSystem(this)
    return attackers
        .filter {
            !it.isDefeated.value &&
                it.type == AttackerType.FALLEN_SHIELDMAIDEN_FREYA &&
                it.type.shieldWallFormationWidth > 0
        }.flatMap { freya ->
            freyaShieldWallPositions(
                freya = freya,
                pathfinding = pathfinding,
                includeFreyaTile = false,
            )
        }.toSet()
}

private fun GameState.isProtectedByFreyaShieldWall(
    freya: Attacker,
    protectedPosition: Position,
    attackOrigin: Position,
    pathfinding: PathfindingSystem,
): Boolean {
    val frontDirection = shieldWallFrontDirection(freya, pathfinding) ?: return false
    if (protectedPosition !in freyaShieldWallPositions(freya, pathfinding, includeFreyaTile = true)) return false

    val attackDirection = hexDirectionToward(protectedPosition, attackOrigin) ?: return false
    return SHIELD_WALL_FRONT_ARC_OFFSETS.any { offset ->
        (frontDirection + offset).mod(HEX_DIRECTION_COUNT) == attackDirection
    }
}

private fun GameState.freyaShieldWallPositions(
    freya: Attacker,
    pathfinding: PathfindingSystem,
    includeFreyaTile: Boolean,
): Set<Position> {
    val freyaPosition = freya.position.value
    val frontDirection = shieldWallFrontDirection(freya, pathfinding) ?: return emptySet()
    val flankRadius = freya.type.shieldWallFormationWidth / 2
    val positions = linkedSetOf<Position>()
    if (includeFreyaTile) {
        positions += freyaPosition
    }

    SHIELD_WALL_FLANK_OFFSETS.forEach { flankOffset ->
        var current = freyaPosition
        repeat(flankRadius) {
            current = current.getHexNeighbor(frontDirection + flankOffset)
            positions += current
        }
    }
    return positions
}

private fun GameState.shieldWallFrontDirection(
    freya: Attacker,
    pathfinding: PathfindingSystem,
): Int? {
    val goal =
        freya.currentTarget?.value
            ?: getActiveTargetPositions().minByOrNull { freya.position.value.hexDistanceTo(it) }
            ?: level.targetPositions.firstOrNull()
            ?: return null
    val nextStep = pathfinding.moveTowards(freya.position.value, goal, freya)
    if (nextStep == freya.position.value) return null
    return freya.position.value.getHexDirectionTo(nextStep)
}

private fun hexDirectionToward(
    from: Position,
    to: Position,
): Int? {
    if (from == to) return null
    return (0 until HEX_DIRECTION_COUNT)
        .minByOrNull { direction -> from.getHexNeighbor(direction).hexDistanceTo(to) }
}
