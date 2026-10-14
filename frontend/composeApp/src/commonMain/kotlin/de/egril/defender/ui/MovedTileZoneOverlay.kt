package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import de.egril.defender.mapgen.core.MapImageRenderer
import de.egril.defender.model.Position
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * Draws a moved movable zone from its zone-only overlay image as one picture: the image is rotated
 * and shifted as a whole so that every tile lands on its current hex.
 *
 * @param tiles the current position of each moved tile paired with its home position
 */
@Composable
fun MovedTileZoneOverlay(
    painter: Painter,
    tiles: List<Pair<Position, Position>>,
    modifier: Modifier = Modifier,
) {
    val transform = remember(tiles) { movedZoneImageTransform(tiles) } ?: return
    Canvas(modifier) {
        val intrinsic = painter.intrinsicSize
        if (intrinsic.width <= 0f || intrinsic.height <= 0f) return@Canvas
        withTransform({
            scale(size.width / intrinsic.width, size.height / intrinsic.height, pivot = Offset.Zero)
            translate(left = transform.targetX.toFloat(), top = transform.targetY.toFloat())
            rotate(degrees = transform.degrees.toFloat(), pivot = Offset.Zero)
            translate(left = -transform.sourceX.toFloat(), top = -transform.sourceY.toFloat())
        }) {
            with(painter) { draw(intrinsic) }
        }
    }
}

/**
 * Image-space rigid transform that maps the home hexes of a moved zone onto their current hexes:
 * `current = R(degrees) * (home - source) + target`.
 *
 * @property maxResidual largest pixel distance between a mapped home hex and its current hex
 */
internal class MovedZoneImageTransform(
    val degrees: Double,
    val sourceX: Double,
    val sourceY: Double,
    val targetX: Double,
    val targetY: Double,
    val maxResidual: Double,
)

/**
 * Finds the 60-degree multiple and translation that carry the home hexes of [tiles] onto their current
 * hexes with the smallest error. Zones whose shape is symmetric may match several rotations equally;
 * the first one in order (no rotation) is kept then, which places every tile correctly.
 */
internal fun movedZoneImageTransform(tiles: List<Pair<Position, Position>>): MovedZoneImageTransform? {
    if (tiles.isEmpty()) return null
    val homes = tiles.map { (_, home) -> MapImageRenderer.hexCenter(home.x, home.y) }
    val currents = tiles.map { (current, _) -> MapImageRenderer.hexCenter(current.x, current.y) }
    val sourceX = homes.sumOf { it.first } / homes.size
    val sourceY = homes.sumOf { it.second } / homes.size
    var best: MovedZoneImageTransform? = null
    for (step in 0 until 6) {
        val degrees = step * 60.0
        val radians = degrees * PI / 180.0
        val c = cos(radians)
        val s = sin(radians)
        val rotated =
            homes.map { (x, y) ->
                val dx = x - sourceX
                val dy = y - sourceY
                (c * dx - s * dy) to (s * dx + c * dy)
            }
        val targetX = tiles.indices.sumOf { currents[it].first - rotated[it].first } / tiles.size
        val targetY = tiles.indices.sumOf { currents[it].second - rotated[it].second } / tiles.size
        var residual = 0.0
        tiles.indices.forEach { index ->
            val errorX = currents[index].first - (rotated[index].first + targetX)
            val errorY = currents[index].second - (rotated[index].second + targetY)
            residual = max(residual, hypot(errorX, errorY))
        }
        val candidate = MovedZoneImageTransform(degrees, sourceX, sourceY, targetX, targetY, residual)
        if (best == null || candidate.maxResidual < best.maxResidual - RESIDUAL_TIE_TOLERANCE) {
            best = candidate
        }
    }
    return best
}

private const val RESIDUAL_TIE_TOLERANCE = 1e-6
