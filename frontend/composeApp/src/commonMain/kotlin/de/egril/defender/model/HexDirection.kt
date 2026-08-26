package de.egril.defender.model

import kotlin.math.atan2

/**
 * The six directions an enemy unit can face on the pointy-top hexagonal grid.
 *
 * The order and meaning of the directions match the neighbour order returned by
 * [getHexNeighbors]: E, NE, NW, W, SW, SE.  Each enemy sprite has one frame per
 * direction so the unit can be shown looking the way it is travelling.
 */
enum class HexDirection {
    E,
    NE,
    NW,
    W,
    SW,
    SE,
    ;

    companion object {
        /**
         * The direction a freshly-spawned enemy faces and the direction used for the
         * "looking forward" portrait shown in the enemy info area.
         */
        val DEFAULT: HexDirection = SW
    }
}

/**
 * Determine which of the six hexagon directions best describes moving from [from] to [to].
 *
 * When [to] is an immediate neighbour of [from] the exact direction is returned by matching
 * the parity-aware neighbour offsets (see [getHexNeighbors]).  For non-adjacent moves the
 * direction whose pixel angle is closest to the travel vector is returned so multi-step or
 * teleport-style moves still yield a sensible facing.  Returns `null` when [from] == [to].
 */
fun hexDirectionBetween(
    from: Position,
    to: Position,
): HexDirection? {
    if (from == to) return null

    // Exact match against the six immediate neighbours (parity aware).
    val neighbors = from.getHexNeighbors()
    val order = listOf(
        HexDirection.E,
        HexDirection.NE,
        HexDirection.NW,
        HexDirection.W,
        HexDirection.SW,
        HexDirection.SE,
    )
    val exactIndex = neighbors.indexOf(to)
    if (exactIndex >= 0) return order[exactIndex]

    // Fallback: pick the neighbour direction whose angle is closest to the travel vector.
    val (fromX, fromY) = from.hexToPixel(1f)
    val (toX, toY) = to.hexToPixel(1f)
    val targetAngle = atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())

    var best: HexDirection = HexDirection.DEFAULT
    var bestDelta = Double.MAX_VALUE
    for ((index, neighbor) in neighbors.withIndex()) {
        val (nx, ny) = neighbor.hexToPixel(1f)
        val angle = atan2((ny - fromY).toDouble(), (nx - fromX).toDouble())
        var delta = kotlin.math.abs(angle - targetAngle)
        if (delta > kotlin.math.PI) delta = 2 * kotlin.math.PI - delta
        if (delta < bestDelta) {
            bestDelta = delta
            best = order[index]
        }
    }
    return best
}
