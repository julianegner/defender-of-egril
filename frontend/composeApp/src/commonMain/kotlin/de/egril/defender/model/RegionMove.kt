package de.egril.defender.model

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Smallest region that may be rotated. A seven-hex flower has an exact centre, so a spin around it
 * maps the region onto itself every six steps. Smaller regions drift because their rounded centre
 * changes with every spin.
 */
const val MIN_ROTATABLE_REGION_TILES = 7

/**
 * A rigid move of a movable tile region (see [TileZone.movable]): the region is first rotated
 * [rotationSteps] times by 60 degrees around its centre, then shifted by ([dx], [dy]) grid cells.
 *
 * The shift is measured on the region's anchor — its top-most, then left-most tile after the
 * rotation — and applied as a hex translation, so the shape of the region is preserved exactly
 * whatever row parity the anchor sits on.
 */
data class RegionMove(
    val rotationSteps: Int = 0,
    val dx: Int = 0,
    val dy: Int = 0,
) {
    val isIdentity: Boolean get() = rotationSteps.mod(6) == 0 && dx == 0 && dy == 0

    /**
     * The function that maps every tile position to its new position for a region covering
     * [region], rotating around [pivot] (by default the centre of [region]). The mapping is a
     * bijection of the hex grid, so tiles and objects that do not belong to the region are never
     * mapped onto each other.
     */
    fun mapperFor(
        region: Collection<Position>,
        pivot: Position = centreOf(region),
    ): (Position) -> Position {
        if (region.isEmpty()) return { it }
        val pivotCube = pivot.toCube()
        val steps = rotationSteps.mod(6)
        val rotate: (CubeCoord) -> CubeCoord = { c -> (c - pivotCube).rotated60(steps) + pivotCube }
        val rotatedAnchor =
            region
                .map { rotate(it.toCube()).toPosition() }
                .minWith(compareBy<Position>({ it.y }, { it.x }))
        val shift =
            if (dx == 0 && dy == 0) {
                CubeCoord.ZERO
            } else {
                Position(rotatedAnchor.x + dx, rotatedAnchor.y + dy).toCube() - rotatedAnchor.toCube()
            }
        return { position -> (rotate(position.toCube()) + shift).toPosition() }
    }

    companion object {
        /**
         * Rounded centre of [region] as a hex, so that a rotation around it maps hexes onto hexes.
         * A moved region keeps its pivot (see [TileZoneSystem]), so this is only computed once.
         */
        internal fun centreOf(region: Collection<Position>): Position {
            val cubes = region.map { it.toCube() }
            val n = cubes.size.toDouble()
            val q = cubes.sumOf { it.q } / n
            val r = cubes.sumOf { it.r } / n
            val s = cubes.sumOf { it.s } / n
            var rq = q.roundToInt()
            var rr = r.roundToInt()
            var rs = s.roundToInt()
            val dq = abs(rq - q)
            val dr = abs(rr - r)
            val ds = abs(rs - s)
            // Restore q + r + s == 0 by correcting the component that was rounded the most.
            if (dq > dr && dq > ds) {
                rq = -rr - rs
            } else if (dr > ds) {
                rr = -rq - rs
            } else {
                rs = -rq - rr
            }
            return CubeCoord(rq, rr, rs).toPosition()
        }
    }
}

/**
 * Cube coordinates of a hex (q + r + s == 0). Used for rotations and translations, which are
 * simple in cube space but not in the odd-row offset layout of [Position].
 */
internal data class CubeCoord(
    val q: Int,
    val r: Int,
    val s: Int,
) {
    operator fun plus(other: CubeCoord) = CubeCoord(q + other.q, r + other.r, s + other.s)

    operator fun minus(other: CubeCoord) = CubeCoord(q - other.q, r - other.r, s - other.s)

    /** Rotates by [steps] * 60 degrees around the origin. */
    fun rotated60(steps: Int): CubeCoord {
        var result = this
        repeat(steps.mod(6)) { result = CubeCoord(-result.r, -result.s, -result.q) }
        return result
    }

    fun toPosition(): Position {
        // Inverse of Position.toCube for odd-row offset coordinates.
        val row = r
        val column = q + (row - (row and 1)) / 2
        return Position(column, row)
    }

    companion object {
        val ZERO = CubeCoord(0, 0, 0)
    }
}

internal fun Position.toCube(): CubeCoord {
    val row = y
    val q = x - (row - (row and 1)) / 2
    return CubeCoord(q, row, -q - row)
}
