package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.game.EnemyMovementSystem
import de.egril.defender.game.FreyaShieldWallArc
import de.egril.defender.game.PathfindingSystem
import de.egril.defender.model.*
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.*
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.icon.wunjoRunePath
import defender_of_egril.composeapp.generated.resources.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Vertical position (as a fraction of the tile height, measured from the top) where the coin-gain
 * "bubbling" animation ends. The rising coins in `files/animations/coin_gain.json` finish around
 * 25% down from the top, so the fly-to-counter animation launches from there to appear to peel off
 * the end of that animation rather than jumping back to the tile center.
 */
internal const val COIN_BUBBLE_END_HEIGHT_FRACTION = 0.25f

/**
 * On-screen diameter of a coin in the coin-gain "bubbling" Lottie, as a fraction of the tile's
 * smaller dimension. The animation (`files/animations/coin_gain.json`) is a 100x100 viewport with
 * 14-unit coins, fitted (ContentScale.Fit) to the tile, so each coin renders at 14/100 of the
 * fitted (smaller) side. The fly-to-counter coins use this so they match the bubbling coins' size.
 */
internal const val COIN_BUBBLE_COIN_SIZE_FRACTION = 0.14f

internal fun oneShotTileAnimationKey(
    animationName: String,
    position: Position,
    turnNumber: Int,
    suffix: String = "",
): String = "$animationName:$turnNumber:${position.x},${position.y}${if (suffix.isNotEmpty()) ":$suffix" else ""}"

@Composable
internal fun rememberShouldPlayOneShotTileAnimation(
    gameState: GameState,
    animationKey: String?,
): Boolean {
    if (animationKey == null) {
        return false
    }
    val shouldPlay = remember(animationKey) { !gameState.playedTileAnimationKeys.containsKey(animationKey) }
    if (shouldPlay && !gameState.playedTileAnimationKeys.containsKey(animationKey)) {
        SideEffect {
            gameState.playedTileAnimationKeys[animationKey] = true
        }
    }
    return shouldPlay
}

internal fun ghostSwarmCount(
    attackerType: AttackerType,
    displayedHealth: Int,
): Int? =
    if (attackerType.isSwarmUnit()) {
        displayedHealth.coerceAtLeast(1)
    } else {
        null
    }

private const val HEX_ROW_VERTICAL_SPACING_FACTOR = 0.75f

// The CW traversal start/end corner index for each edge direction.
// CW order of edges: NE(1), E(0), SE(5), SW(4), W(3), NW(2).
// CW order of corners: top(0), top-right(1), bottom-right(2), bottom(3), bottom-left(4), top-left(5).
private val SHIELD_WALL_EDGE_START_CORNER = intArrayOf(1, 0, 5, 4, 3, 2)
private val SHIELD_WALL_EDGE_END_CORNER = intArrayOf(2, 1, 0, 5, 4, 3)

// For corner K of tile P, the two neighbor-directions whose tiles also share that corner.
private val SHIELD_WALL_CORNER_DIR_PAIRS =
    arrayOf(
        intArrayOf(1, 2), // Corner 0 (top): NE and NW neighbors
        intArrayOf(0, 1), // Corner 1 (top-right): E and NE neighbors
        intArrayOf(5, 0), // Corner 2 (bottom-right): SE and E neighbors
        intArrayOf(4, 5), // Corner 3 (bottom): SW and SE neighbors
        intArrayOf(3, 4), // Corner 4 (bottom-left): W and SW neighbors
        intArrayOf(2, 3), // Corner 5 (top-left): NW and W neighbors
    )

/** Edge between a shield-wall tile and a non-shield-wall neighbor, identified by its midpoint and its two endpoint corner keys. */
private data class ShieldWallBoundaryEdge(
    val midpoint: Offset,
    val startKey: String,
    val endKey: String,
)

internal fun getFisherRodRotationDegrees(
    position: Position,
    level: Level,
): Float {
    val waterNeighbor =
        position
            .getHexNeighbors()
            .filter { neighbor ->
                neighbor.x in 0 until level.gridWidth &&
                    neighbor.y in 0 until level.gridHeight
            }.firstOrNull { neighbor ->
                level.isRiverTile(neighbor)
            } ?: return 0f

    val sourceX = position.x + if (position.y % 2 == 1) 0.5f else 0f
    val sourceY = position.y * HEX_ROW_VERTICAL_SPACING_FACTOR
    val targetX = waterNeighbor.x + if (waterNeighbor.y % 2 == 1) 0.5f else 0f
    val targetY = waterNeighbor.y * HEX_ROW_VERTICAL_SPACING_FACTOR
    val dx = targetX - sourceX
    val dy = targetY - sourceY

    return (atan2(dy, dx) * 180f / PI.toFloat())
}

/** Returns a canonical key for a hex corner shared by three tiles (tile P and two neighbors). */
private fun shieldWallCornerKey(
    pos: Position,
    cornerIndex: Int,
): String {
    val dirPair = SHIELD_WALL_CORNER_DIR_PAIRS[cornerIndex]
    val neighbors = pos.getHexNeighbors()
    val sorted =
        listOf(pos, neighbors[dirPair[0]], neighbors[dirPair[1]])
            .sortedWith(compareBy({ it.y }, { it.x }))
    return "${sorted[0].x},${sorted[0].y}|${sorted[1].x},${sorted[1].y}|${sorted[2].x},${sorted[2].y}"
}

/**
 * Trim [trimFraction] (0..1) of the total arc length from each end of an ordered list of path
 * points. Returns a new list whose first/last points are interpolated so that the rendered arc
 * does not extend all the way to the sharp tip corners.
 */
private fun trimArcPathEnds(
    points: List<Offset>,
    trimFraction: Float,
): List<Offset> {
    if (points.size < 2) return points
    val segLens =
        FloatArray(points.size - 1) { i ->
            val dx = points[i + 1].x - points[i].x
            val dy = points[i + 1].y - points[i].y
            sqrt(dx * dx + dy * dy)
        }
    val totalLen = segLens.sum()
    if (totalLen == 0f) return points
    val startDist = totalLen * trimFraction
    val endDist = totalLen * (1f - trimFraction)

    fun pointAt(dist: Float): Offset {
        var cum = 0f
        for (i in segLens.indices) {
            val segEnd = cum + segLens[i]
            if (dist <= segEnd || i == segLens.lastIndex) {
                val t = if (segLens[i] > 0f) ((dist - cum) / segLens[i]).coerceIn(0f, 1f) else 0f
                return Offset(
                    points[i].x + t * (points[i + 1].x - points[i].x),
                    points[i].y + t * (points[i + 1].y - points[i].y),
                )
            }
            cum = segEnd
        }
        return points.last()
    }

    val result = mutableListOf(pointAt(startDist))
    var cum = 0f
    for (i in segLens.indices) {
        val segEnd = cum + segLens[i]
        if (segEnd > startDist && segEnd < endDist) result.add(points[i + 1])
        cum = segEnd
    }
    result.add(pointAt(endDist))
    return result
}

/**
 * Map-level overlay that draws the Freya shield wall as a smooth open arc along the front-facing
 * edges of the shield wall formation.
 *
 * Only edges whose direction falls within the front arc (forward ± 1 direction) are included,
 * producing a visual barrier on the side facing the enemy. The arc is rendered as a
 * grey–blue–grey stripe, rounded via [PathEffect.cornerPathEffect] for a smooth appearance.
 */
@Composable
internal fun FreyaShieldWallMapOverlay(
    shieldWallArcs: List<FreyaShieldWallArc>,
    hexSizeDp: Float,
    contentSize: IntSize,
    modifier: Modifier = Modifier,
) {
    if (shieldWallArcs.isEmpty()) return

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val hexSizePx = hexSizeDp * density
    val hexWidthPx = hexSizePx * sqrt(3f)
    val hexHeightPx = hexSizePx * 2f
    val rowSpacingPx =
        hexHeightPx * 0.75f - hexHeightPx + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT * density
    val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING * density
    val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO

    fun tileCenterPx(pos: Position): Offset {
        val oddRowOffset = if (pos.y % 2 == 1) oddOffsetPx else 0f
        return Offset(
            pos.x * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset,
            pos.y * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f,
        )
    }

    // Build an open-chain arc path for each Freya.
    val arcPaths = mutableListOf<Path>()

    for (arc in shieldWallArcs) {
        val shieldWallPositions = arc.positions
        val frontDirection = arc.frontDirection
        // Only edges facing the front arc (forward, forward-left, forward-right).
        val frontArcDirs =
            setOf(
                frontDirection.mod(6),
                (frontDirection + 1).mod(6),
                (frontDirection + 5).mod(6),
            )

        // Collect front-arc boundary edges (edges between a shield tile and a non-shield tile).
        val boundaryEdges = mutableListOf<ShieldWallBoundaryEdge>()
        for (pos in shieldWallPositions) {
            val center = tileCenterPx(pos)
            val neighbors = pos.getHexNeighbors()
            for (dir in 0..5) {
                if (dir in frontArcDirs && neighbors[dir] !in shieldWallPositions) {
                    val nbrCenter = tileCenterPx(neighbors[dir])
                    val mid = Offset((center.x + nbrCenter.x) / 2f, (center.y + nbrCenter.y) / 2f)
                    boundaryEdges.add(
                        ShieldWallBoundaryEdge(
                            midpoint = mid,
                            startKey = shieldWallCornerKey(pos, SHIELD_WALL_EDGE_START_CORNER[dir]),
                            endKey = shieldWallCornerKey(pos, SHIELD_WALL_EDGE_END_CORNER[dir]),
                        ),
                    )
                }
            }
        }
        if (boundaryEdges.isEmpty()) continue

        // Build adjacency: corner key → edges touching that corner.
        val cornerToEdges = mutableMapOf<String, MutableList<ShieldWallBoundaryEdge>>()
        for (edge in boundaryEdges) {
            cornerToEdges.getOrPut(edge.startKey) { mutableListOf() }.add(edge)
            cornerToEdges.getOrPut(edge.endKey) { mutableListOf() }.add(edge)
        }

        // Find terminal corners (degree 1) — the two endpoints of the open chain.
        val startKey =
            cornerToEdges.entries.firstOrNull { it.value.size == 1 }?.key ?: continue

        // Walk the chain from one terminal to the other, collecting midpoints in order.
        val midpoints = mutableListOf<Offset>()
        val visited = mutableSetOf<ShieldWallBoundaryEdge>()
        var prevKey = startKey
        var cur = cornerToEdges[startKey]!!.first()
        while (true) {
            if (cur in visited) break
            visited.add(cur)
            midpoints.add(cur.midpoint)
            val nextKey = if (prevKey == cur.startKey) cur.endKey else cur.startKey
            val next = cornerToEdges[nextKey]?.firstOrNull { it !in visited } ?: break
            prevKey = nextKey
            cur = next
        }
        if (midpoints.size < 2) continue

        // Trim 12% from each end so the arc does not extend too far at its tips.
        val trimmed = trimArcPathEnds(midpoints, 0.12f)
        if (trimmed.size < 2) continue

        // Build as straight segments; smoothing is applied via PathEffect.cornerPathEffect.
        arcPaths.add(
            Path().apply {
                moveTo(trimmed[0].x, trimmed[0].y)
                for (i in 1 until trimmed.size) lineTo(trimmed[i].x, trimmed[i].y)
            },
        )
    }

    if (arcPaths.isEmpty()) return

    val outerStrokeWidth = hexSizePx * 0.26f
    val innerStrokeWidth = hexSizePx * 0.10f
    val cornerRadius = hexSizePx * 0.45f
    // Match the shield trim color from Freya's icon (FallenShieldmaidenFreya.kt shieldTrim).
    val blueColor = Color(0xFF7DD7FF).copy(alpha = 0.95f)
    val greyColor = Color(0xFFAAAAAA).copy(alpha = 0.95f)

    val contentWidthDp = (contentSize.width / density).dp
    val contentHeightDp = (contentSize.height / density).dp

    Canvas(
        modifier =
            modifier
                .requiredSize(contentWidthDp, contentHeightDp)
                .semantics { contentDescription = "Shield Wall" },
    ) {
        for (path in arcPaths) {
            // Grey outer stroke (wider) drawn first, leaving grey visible on both sides of the arc.
            drawPath(
                path = path,
                color = greyColor,
                style =
                    Stroke(
                        width = outerStrokeWidth,
                        pathEffect = PathEffect.cornerPathEffect(cornerRadius),
                    ),
            )
            // Blue inner stroke drawn on top, creating the grey–blue–grey stripe effect.
            drawPath(
                path = path,
                color = blueColor,
                style =
                    Stroke(
                        width = innerStrokeWidth,
                        pathEffect = PathEffect.cornerPathEffect(cornerRadius),
                    ),
            )
        }
    }
}

/**
 * Map-level overlay that draws rift portal runes for each active [Portal].
 *
 * Each portal pair shares the same Futhark rune shape so the player can see which entry matches
 * which exit.  Different portals on the map cycle through a pool of distinct rune shapes.
 *
 * Entry portal (blue circle) is drawn at the villain-side tile.
 * Exit portal (orange circle) is drawn at the demonling-side tile.
 */
@Composable
internal fun RiftPortalOverlay(
    portals: List<de.egril.defender.model.Portal>,
    hexSizeDp: Float,
    contentSize: IntSize,
    modifier: Modifier = Modifier,
) {
    if (portals.isEmpty()) return

    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val hexSizePx = hexSizeDp * density
    val hexWidthPx = hexSizePx * sqrt(3f)
    val hexHeightPx = hexSizePx * 2f
    val rowSpacingPx =
        hexHeightPx * 0.75f - hexHeightPx + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT * density
    val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING * density
    val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO

    fun tileCenterPx(pos: de.egril.defender.model.Position): androidx.compose.ui.geometry.Offset {
        val oddRowOffset = if (pos.y % 2 == 1) oddOffsetPx else 0f
        return androidx.compose.ui.geometry.Offset(
            pos.x * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset,
            // Apply the same per-row visual correction as HexagonalMapView's
            // `.offset(y = (-(y - 1)).dp)` modifier on each Row.
            pos.y * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f - (pos.y - 1) * density,
        )
    }

    /**
     * Builds the Path for rune [index] centred at ([cx], [cy]) with half-height [r].
     * Each index maps to a distinct Elder Futhark rune shape, path-approximated.
     *
     * Elder Futhark order used here:
     *   0=ᚠ Fehu    1=ᚢ Uruz    2=ᚦ Thurisaz  3=ᚨ Ansuz    4=ᚱ Raido    5=ᚲ Kenaz
     *   6=ᚷ Gebo    7=ᚹ Wunjo   8=ᚺ Hagalaz   9=ᚾ Nauthiz  10=ᛁ Isa    11=ᛃ Jera
     *  12=ᛇ Eihwaz  13=ᛈ Perthro 14=ᛉ Algiz   15=ᛊ Sowilo   16=ᛏ Tiwaz  17=ᛒ Berkano
     *  18=ᛖ Ehwaz   19=ᛗ Mannaz  20=ᛚ Laguz   21=ᛜ Ingwaz   22=ᛞ Dagaz  23=ᛟ Othala
     */
    @Suppress("MagicNumber")
    fun runePathFor(
        index: Int,
        cx: Float,
        cy: Float,
        r: Float,
    ): androidx.compose.ui.graphics.Path =
        androidx.compose.ui.graphics.Path().apply {
            when (index % de.egril.defender.model.Portal.FUTHARK_RUNE_COUNT) {
                0 -> { // ᚠ Fehu – staff + two rightward branches
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.55f)
                    lineTo(cx + r * 0.75f, cy - r * 0.15f)
                    moveTo(cx, cy + r * 0.05f)
                    lineTo(cx + r * 0.75f, cy + r * 0.45f)
                }
                1 -> { // ᚢ Uruz – two legs, arched top
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r * 0.2f)
                    lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx + r * 0.4f, cy - r * 0.2f)
                }
                2 -> { // ᚦ Thurisaz – staff + rightward thorn (diamond lobe)
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.3f)
                    lineTo(cx + r * 0.8f, cy + r * 0.25f)
                    lineTo(cx, cy + r * 0.2f)
                }
                3 -> { // ᚨ Ansuz – staff + two leftward branches pointing down
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.3f)
                    lineTo(cx - r * 0.7f, cy + r * 0.1f)
                    moveTo(cx, cy + r * 0.2f)
                    lineTo(cx - r * 0.7f, cy + r * 0.6f)
                }
                4 -> { // ᚱ Raido – staff + two rightward legs from upper half
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.85f, cy - r * 0.1f)
                    lineTo(cx, cy + r * 0.1f)
                    lineTo(cx + r * 0.85f, cy + r)
                }
                5 -> { // ᚲ Kenaz – staff + short rightward diagonal at bottom
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy + r)
                    lineTo(cx + r * 0.7f, cy + r * 0.3f)
                }
                6 -> { // ᚷ Gebo – X cross
                    moveTo(cx - r * 0.6f, cy - r)
                    lineTo(cx + r * 0.6f, cy + r)
                    moveTo(cx + r * 0.6f, cy - r)
                    lineTo(cx - r * 0.6f, cy + r)
                }
                7 -> addPath(wunjoRunePath(cx, cy, r)) // ᚹ Wunjo – staff + rightward flag at the top
                8 -> { // ᚺ Hagalaz – two diagonals crossing + short horizontal bar
                    moveTo(cx - r * 0.5f, cy - r)
                    lineTo(cx - r * 0.5f, cy + r)
                    moveTo(cx + r * 0.5f, cy - r)
                    lineTo(cx + r * 0.5f, cy + r)
                    moveTo(cx - r * 0.5f, cy)
                    lineTo(cx + r * 0.5f, cy)
                }
                9 -> { // ᚾ Nauthiz – staff + crossing diagonal brace
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r)
                    lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r * 0.3f)
                    lineTo(cx + r * 0.4f, cy + r * 0.3f)
                }
                10 -> { // ᛁ Isa – single vertical staff
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                }
                11 -> { // ᛃ Jera – two opposing angled chevrons (top-right, bottom-left)
                    moveTo(cx - r * 0.5f, cy - r)
                    lineTo(cx + r * 0.5f, cy - r * 0.3f)
                    lineTo(cx - r * 0.5f, cy + r * 0.3f)
                    moveTo(cx + r * 0.5f, cy - r * 0.3f)
                    lineTo(cx + r * 0.5f, cy + r)
                    lineTo(cx - r * 0.5f, cy + r * 0.3f)
                }
                12 -> { // ᛇ Eihwaz – staff + small branches each side near top and bottom
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.5f)
                    lineTo(cx - r * 0.55f, cy - r)
                    moveTo(cx, cy + r * 0.5f)
                    lineTo(cx + r * 0.55f, cy + r)
                }
                13 -> { // ᛈ Perthro – open cup/bowl facing right (staff + two horizontal arms)
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx + r * 0.55f, cy - r * 0.5f)
                    moveTo(cx - r * 0.4f, cy + r)
                    lineTo(cx + r * 0.55f, cy + r * 0.5f)
                }
                14 -> { // ᛉ Algiz – staff + upward Y fork at the top
                    moveTo(cx, cy + r)
                    lineTo(cx, cy - r * 0.2f)
                    moveTo(cx, cy - r * 0.2f)
                    lineTo(cx - r * 0.6f, cy - r)
                    moveTo(cx, cy - r * 0.2f)
                    lineTo(cx + r * 0.6f, cy - r)
                }
                15 -> { // ᛊ Sowilo – two diagonal strokes forming a lightning-bolt S
                    moveTo(cx + r * 0.5f, cy - r)
                    lineTo(cx - r * 0.5f, cy - r * 0.2f)
                    lineTo(cx + r * 0.5f, cy + r * 0.2f)
                    lineTo(cx - r * 0.5f, cy + r)
                }
                16 -> { // ᛏ Tiwaz – upward arrow / spear
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r)
                    lineTo(cx - r * 0.6f, cy - r * 0.3f)
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.6f, cy - r * 0.3f)
                }
                17 -> { // ᛒ Berkano – staff + two rightward bumps (upper and lower)
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.65f, cy - r * 0.45f)
                    lineTo(cx, cy)
                    moveTo(cx, cy)
                    lineTo(cx + r * 0.65f, cy + r * 0.45f)
                    lineTo(cx, cy + r)
                }
                18 -> { // ᛖ Ehwaz – two opposing E-like strokes (left and right)
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r)
                    lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy)
                    lineTo(cx + r * 0.4f, cy)
                }
                19 -> { // ᛗ Mannaz – two staffs + X brace between them
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r)
                    lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r)
                    lineTo(cx + r * 0.4f, cy - r * 0.2f)
                    moveTo(cx + r * 0.4f, cy - r)
                    lineTo(cx - r * 0.4f, cy - r * 0.2f)
                }
                20 -> { // ᛚ Laguz – staff + single leftward diagonal pointing down
                    moveTo(cx, cy - r)
                    lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.1f)
                    lineTo(cx - r * 0.7f, cy + r * 0.5f)
                }
                21 -> { // ᛜ Ingwaz – diamond shape
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.7f, cy)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.7f, cy)
                    close()
                }
                22 -> { // ᛞ Dagaz – horizontal infinity / bow-tie
                    moveTo(cx - r * 0.6f, cy - r)
                    lineTo(cx + r * 0.6f, cy + r)
                    moveTo(cx + r * 0.6f, cy - r)
                    lineTo(cx - r * 0.6f, cy + r)
                    moveTo(cx - r * 0.6f, cy - r)
                    lineTo(cx - r * 0.6f, cy + r)
                    moveTo(cx + r * 0.6f, cy - r)
                    lineTo(cx + r * 0.6f, cy + r)
                }
                else -> { // 23: ᛟ Othala – diamond with two descending legs
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.6f, cy)
                    lineTo(cx, cy + r * 0.3f)
                    lineTo(cx - r * 0.6f, cy)
                    close()
                    moveTo(cx - r * 0.3f, cy + r * 0.3f)
                    lineTo(cx - r * 0.6f, cy + r)
                    moveTo(cx + r * 0.3f, cy + r * 0.3f)
                    lineTo(cx + r * 0.6f, cy + r)
                }
            }
        }

    val entryColor =
        androidx.compose.ui.graphics
            .Color(0xFF2080FF)
    val exitColor =
        androidx.compose.ui.graphics
            .Color(0xFFFF7000)
    val glowAlpha = 0.25f
    val runeSize = hexSizePx * 0.55f
    val runeR = runeSize * 0.30f
    val runeStroke =
        androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.5f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round,
        )

    val contentWidthDp = (contentSize.width / density).dp
    val contentHeightDp = (contentSize.height / density).dp

    Canvas(modifier = modifier.requiredSize(contentWidthDp, contentHeightDp)) {
        for (portal in portals) {
            // Entry circle (blue)
            val ec = tileCenterPx(portal.entryPosition)
            drawCircle(color = entryColor.copy(alpha = glowAlpha), radius = runeSize * 0.75f, center = ec)
            drawCircle(
                color = entryColor,
                radius = runeSize * 0.42f,
                center = ec,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 3f),
            )
            drawPath(
                runePathFor(
                    de.egril.defender.model.Portal
                        .runeGlyphIndex(portal.runeIndex),
                    ec.x,
                    ec.y,
                    runeR,
                ),
                color = entryColor,
                style = runeStroke,
            )

            // Exit circle (orange) — same rune shape, different colour
            val xc = tileCenterPx(portal.exitPosition)
            drawCircle(color = exitColor.copy(alpha = glowAlpha), radius = runeSize * 0.75f, center = xc)
            drawCircle(
                color = exitColor,
                radius = runeSize * 0.42f,
                center = xc,
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 3f),
            )
            drawPath(
                runePathFor(
                    de.egril.defender.model.Portal
                        .runeGlyphIndex(portal.runeIndex),
                    xc.x,
                    xc.y,
                    runeR,
                ),
                color = exitColor,
                style = runeStroke,
            )
        }
    }
}

internal fun displayedRiverTile(
    levelRiverTile: RiverTile?,
    sandboxPaintedRiverTile: RiverTile?,
): RiverTile? = sandboxPaintedRiverTile ?: levelRiverTile

internal fun plannedEnemyPathForDisplay(
    gameState: GameState,
    attacker: Attacker,
): List<Position>? {
    if (attacker.isDefeated.value) {
        return null
    }
    val pathfinding = PathfindingSystem(gameState)

    // Enemies do not walk in a single straight A* line to the final target: they follow a chain
    // of waypoints (attacker.currentTarget is only the NEXT waypoint, not the final destination —
    // see EnemyMovementSystem.canReachTargetNextTurn for the equivalent real-movement logic).
    // A single findPath(start, finalTarget) call therefore either returns just the next short
    // segment (when using currentTarget) or can fail to resolve a long, winding, cost-inflated
    // route within the A* iteration cap (when aiming straight at the final target). To display the
    // WHOLE planned route we must replicate the same waypoint-by-waypoint chaining used by actual
    // enemy movement, concatenating each resolved segment until an active target is reached.
    var position = attacker.position.value
    var target =
        attacker.currentTarget?.value
            ?: gameState.getActiveTargetPositions().minByOrNull { position.distanceTo(it) }
            ?: gameState.level.targetPositions.firstOrNull()
            ?: return null

    val fullPath = mutableListOf(position)
    val visitedWaypointTargets = mutableSetOf<Position>()
    val maxSegments = 200 // Safety guard against pathological waypoint loops

    for (segmentIndex in 0 until maxSegments) {
        // Use the unweighted BFS search (not findPath's cost-inflated A*) so a long / winding
        // segment can never silently fall back to a single-step path (see findSimplePath kdoc).
        // Prefer the real, barricade-respecting route first (what the enemy can actually walk
        // right now). Only if that yields no full path to this segment's target do we fall back
        // to the cheapest route through barricades (findPathThroughBarricades weighs both distance
        // and barricade-destruction time — see its kdoc), keeping the preview consistent with the
        // enemy's actual movement/fallback logic in Movement.kt.
        var segment = pathfinding.findSimplePath(position, target, attacker)
        if (segment.size < 2 || segment.last() != target) {
            val segmentThroughBarricades = pathfinding.findPathThroughBarricades(position, target, attacker)
            if (segmentThroughBarricades.size >= 2 && segmentThroughBarricades.last() == target) {
                segment = segmentThroughBarricades
            }
        }
        if (segment.size < 2) {
            break // No progress possible towards this segment's target
        }

        fullPath.addAll(segment.drop(1))
        position = segment.last()

        if (gameState.isActiveTargetPosition(position)) {
            break // Reached an active final target — the preview stops here
        }

        if (position != target) {
            break // Segment couldn't fully reach its target (blocked) — stop to avoid looping
        }

        val nextWaypointTarget = gameState.level.getWaypointAt(position)?.nextTarget
        if (nextWaypointTarget == null || !visitedWaypointTargets.add(target)) {
            break // No further waypoint, or this waypoint target was already used (loop guard)
        }
        target = nextWaypointTarget
    }

    return fullPath.takeIf { it.size > 1 }
}

