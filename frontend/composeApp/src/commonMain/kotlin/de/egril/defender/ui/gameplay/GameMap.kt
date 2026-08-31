package de.egril.defender.ui.gameplay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.hyperether.resources.stringResource
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.LogConfig
import de.egril.defender.game.EnemyMovementSystem
import de.egril.defender.game.FreyaShieldWallArc
import de.egril.defender.game.PathfindingSystem
import de.egril.defender.game.freyaShieldWallArcs
import de.egril.defender.model.*
import de.egril.defender.model.getHexNeighbors
import de.egril.defender.ui.*
import de.egril.defender.ui.animations.AlchemyAttackOverlay
import de.egril.defender.ui.animations.AlchemyIdleAnimation
import de.egril.defender.ui.animations.ArrowAttackAnimation
import de.egril.defender.ui.animations.BallistaAttackOverlay
import de.egril.defender.ui.animations.BarricadeDamageAnimation
import de.egril.defender.ui.animations.BombExplosionAnimation
import de.egril.defender.ui.animations.BowAttackOverlay
import de.egril.defender.ui.animations.CoinFlightController
import de.egril.defender.ui.animations.CoinGainAnimation
import de.egril.defender.ui.animations.CoolingAreaAnimation
import de.egril.defender.ui.animations.DragonLevelChangeAnimation
import de.egril.defender.ui.animations.DragonTargetAnimation
import de.egril.defender.ui.animations.EnemyDeathAnimation
import de.egril.defender.ui.animations.EnemyMoveAnimation
import de.egril.defender.ui.animations.EnemySpawnAnimation
import de.egril.defender.ui.animations.FearSpellAnimation
import de.egril.defender.ui.animations.FreezeSpellAnimation
import de.egril.defender.ui.animations.GarokkWarCryOverlay
import de.egril.defender.ui.animations.GreenWitchHealingAnimation
import de.egril.defender.ui.animations.InstantTowerSpellAnimation
import de.egril.defender.ui.animations.MineDigAnimation
import de.egril.defender.ui.animations.MushroomBuffAnimation
import de.egril.defender.ui.animations.PikeAttackOverlay
import de.egril.defender.ui.animations.RocketAttackOverlay
import de.egril.defender.ui.animations.SkyIsFallingAnimation
import de.egril.defender.ui.animations.SnotlingCannonThrowOverlay
import de.egril.defender.ui.animations.SpearAttackOverlay
import de.egril.defender.ui.animations.SpellDoubleReachColor
import de.egril.defender.ui.animations.TowerAttackImpactAnimation
import de.egril.defender.ui.animations.TowerConstructionCompleteAnimation
import de.egril.defender.ui.animations.TowerReadyPulseAnimation
import de.egril.defender.ui.animations.TrapTriggerAnimation
import de.egril.defender.ui.animations.WaterFlowAnimation
import de.egril.defender.ui.animations.WizardAttackOverlay
import de.egril.defender.ui.animations.WizardIdleAnimation
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.map.MapControlState
import de.egril.defender.ui.editor.map.MapControls
import de.egril.defender.ui.hexagon.BaseGridCell
import de.egril.defender.ui.hexagon.HexagonMinimap
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import de.egril.defender.ui.hexagon.HexagonalMapConfig
import de.egril.defender.ui.hexagon.HexagonalMapView
import de.egril.defender.ui.hexagon.MinimapConfig
import de.egril.defender.ui.icon.BombIcon
import de.egril.defender.ui.icon.CrossIcon
import de.egril.defender.ui.icon.ExplosionIcon
import de.egril.defender.ui.icon.GateIcon
import de.egril.defender.ui.icon.MushroomIcon
import de.egril.defender.ui.icon.PentagramIcon
import de.egril.defender.ui.icon.TestTubeIcon
import de.egril.defender.ui.icon.TrapIcon
import de.egril.defender.ui.icon.WebIcon
import de.egril.defender.ui.icon.WoodIcon
import de.egril.defender.ui.icon.enemy.EnemyAttackPreview
import de.egril.defender.ui.icon.enemy.EnemyAttackPreviewIcon
import de.egril.defender.ui.icon.enemy.EnemyIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon
import de.egril.defender.ui.icon.enemy.enemyAttackPreview
import de.egril.defender.ui.rememberMapImageState
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Vertical position (as a fraction of the tile height, measured from the top) where the coin-gain
 * "bubbling" animation ends. The rising coins in `files/animations/coin_gain.json` finish around
 * 25% down from the top, so the fly-to-counter animation launches from there to appear to peel off
 * the end of that animation rather than jumping back to the tile center.
 */
private const val COIN_BUBBLE_END_HEIGHT_FRACTION = 0.25f

/**
 * On-screen diameter of a coin in the coin-gain "bubbling" Lottie, as a fraction of the tile's
 * smaller dimension. The animation (`files/animations/coin_gain.json`) is a 100x100 viewport with
 * 14-unit coins, fitted (ContentScale.Fit) to the tile, so each coin renders at 14/100 of the
 * fitted (smaller) side. The fly-to-counter coins use this so they match the bubbling coins' size.
 */
private const val COIN_BUBBLE_COIN_SIZE_FRACTION = 0.14f

private fun oneShotTileAnimationKey(
    animationName: String,
    position: Position,
    turnNumber: Int,
    suffix: String = "",
): String = "$animationName:$turnNumber:${position.x},${position.y}${if (suffix.isNotEmpty()) ":$suffix" else ""}"

@Composable
private fun rememberShouldPlayOneShotTileAnimation(
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

internal fun ghostSwarmCount(attackerType: AttackerType, displayedHealth: Int): Int? =
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

private fun getFisherRodRotationDegrees(
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
private fun FreyaShieldWallMapOverlay(
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
private fun RiftPortalOverlay(
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
     * Each index maps to a distinct Elder Futhark rune shape (all 24 runes, path-approximated).
     *
     * Elder Futhark order used here:
     *   0=ᚠ Fehu    1=ᚢ Uruz    2=ᚦ Thurisaz  3=ᚨ Ansuz    4=ᚱ Raido    5=ᚲ Kenaz
     *   6=ᚷ Gebo    7=ᚹ Wunjo   8=ᚺ Hagalaz   9=ᚾ Nauthiz  10=ᛁ Isa    11=ᛃ Jera
     *  12=ᛇ Eihwaz  13=ᛈ Perthro 14=ᛉ Algiz   15=ᛊ Sowilo   16=ᛏ Tiwaz  17=ᛒ Berkano
     *  18=ᛖ Ehwaz   19=ᛗ Mannaz  20=ᛚ Laguz   21=ᛜ Ingwaz   22=ᛞ Dagaz  23=ᛟ Othala
     */
    @Suppress("MagicNumber")
    fun runePathFor(index: Int, cx: Float, cy: Float, r: Float): androidx.compose.ui.graphics.Path =
        androidx.compose.ui.graphics.Path().apply {
            when (index % de.egril.defender.model.Portal.RUNE_POOL_SIZE) {
                0 -> { // ᚠ Fehu – staff + two rightward branches
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.55f); lineTo(cx + r * 0.75f, cy - r * 0.15f)
                    moveTo(cx, cy + r * 0.05f); lineTo(cx + r * 0.75f, cy + r * 0.45f)
                }
                1 -> { // ᚢ Uruz – two legs, arched top
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r * 0.2f); lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx + r * 0.4f, cy - r * 0.2f)
                }
                2 -> { // ᚦ Thurisaz – staff + rightward thorn (diamond lobe)
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.3f); lineTo(cx + r * 0.8f, cy + r * 0.25f)
                    lineTo(cx, cy + r * 0.2f)
                }
                3 -> { // ᚨ Ansuz – staff + two leftward branches pointing down
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.3f); lineTo(cx - r * 0.7f, cy + r * 0.1f)
                    moveTo(cx, cy + r * 0.2f); lineTo(cx - r * 0.7f, cy + r * 0.6f)
                }
                4 -> { // ᚱ Raido – staff + two rightward legs from upper half
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r); lineTo(cx + r * 0.85f, cy - r * 0.1f)
                    lineTo(cx, cy + r * 0.1f); lineTo(cx + r * 0.85f, cy + r)
                }
                5 -> { // ᚲ Kenaz – staff + short rightward diagonal at bottom
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy + r); lineTo(cx + r * 0.7f, cy + r * 0.3f)
                }
                6 -> { // ᚷ Gebo – X cross
                    moveTo(cx - r * 0.6f, cy - r); lineTo(cx + r * 0.6f, cy + r)
                    moveTo(cx + r * 0.6f, cy - r); lineTo(cx - r * 0.6f, cy + r)
                }
                7 -> { // ᚹ Wunjo – staff + rightward flag at the top
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r); lineTo(cx + r * 0.7f, cy - r * 0.4f)
                    lineTo(cx, cy - r * 0.2f)
                }
                8 -> { // ᚺ Hagalaz – two diagonals crossing + short horizontal bar
                    moveTo(cx - r * 0.5f, cy - r); lineTo(cx - r * 0.5f, cy + r)
                    moveTo(cx + r * 0.5f, cy - r); lineTo(cx + r * 0.5f, cy + r)
                    moveTo(cx - r * 0.5f, cy); lineTo(cx + r * 0.5f, cy)
                }
                9 -> { // ᚾ Nauthiz – staff + crossing diagonal brace
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r); lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r * 0.3f); lineTo(cx + r * 0.4f, cy + r * 0.3f)
                }
                10 -> { // ᛁ Isa – single vertical staff
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                }
                11 -> { // ᛃ Jera – two opposing angled chevrons (top-right, bottom-left)
                    moveTo(cx - r * 0.5f, cy - r); lineTo(cx + r * 0.5f, cy - r * 0.3f)
                    lineTo(cx - r * 0.5f, cy + r * 0.3f)
                    moveTo(cx + r * 0.5f, cy - r * 0.3f); lineTo(cx + r * 0.5f, cy + r)
                    lineTo(cx - r * 0.5f, cy + r * 0.3f)
                }
                12 -> { // ᛇ Eihwaz – staff + small branches each side near top and bottom
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.5f); lineTo(cx - r * 0.55f, cy - r)
                    moveTo(cx, cy + r * 0.5f); lineTo(cx + r * 0.55f, cy + r)
                }
                13 -> { // ᛈ Perthro – open cup/bowl facing right (staff + two horizontal arms)
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx + r * 0.55f, cy - r * 0.5f)
                    moveTo(cx - r * 0.4f, cy + r); lineTo(cx + r * 0.55f, cy + r * 0.5f)
                }
                14 -> { // ᛉ Algiz – staff + upward Y fork at the top
                    moveTo(cx, cy + r); lineTo(cx, cy - r * 0.2f)
                    moveTo(cx, cy - r * 0.2f); lineTo(cx - r * 0.6f, cy - r)
                    moveTo(cx, cy - r * 0.2f); lineTo(cx + r * 0.6f, cy - r)
                }
                15 -> { // ᛊ Sowilo – two diagonal strokes forming a lightning-bolt S
                    moveTo(cx + r * 0.5f, cy - r); lineTo(cx - r * 0.5f, cy - r * 0.2f)
                    lineTo(cx + r * 0.5f, cy + r * 0.2f); lineTo(cx - r * 0.5f, cy + r)
                }
                16 -> { // ᛏ Tiwaz – upward arrow / spear
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r); lineTo(cx - r * 0.6f, cy - r * 0.3f)
                    moveTo(cx, cy - r); lineTo(cx + r * 0.6f, cy - r * 0.3f)
                }
                17 -> { // ᛒ Berkano – staff + two rightward bumps (upper and lower)
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r); lineTo(cx + r * 0.65f, cy - r * 0.45f)
                    lineTo(cx, cy)
                    moveTo(cx, cy); lineTo(cx + r * 0.65f, cy + r * 0.45f)
                    lineTo(cx, cy + r)
                }
                18 -> { // ᛖ Ehwaz – two opposing E-like strokes (left and right)
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r); lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy); lineTo(cx + r * 0.4f, cy)
                }
                19 -> { // ᛗ Mannaz – two staffs + X brace between them
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx - r * 0.4f, cy + r)
                    moveTo(cx + r * 0.4f, cy - r); lineTo(cx + r * 0.4f, cy + r)
                    moveTo(cx - r * 0.4f, cy - r); lineTo(cx + r * 0.4f, cy - r * 0.2f)
                    moveTo(cx + r * 0.4f, cy - r); lineTo(cx - r * 0.4f, cy - r * 0.2f)
                }
                20 -> { // ᛚ Laguz – staff + single leftward diagonal pointing down
                    moveTo(cx, cy - r); lineTo(cx, cy + r)
                    moveTo(cx, cy - r * 0.1f); lineTo(cx - r * 0.7f, cy + r * 0.5f)
                }
                21 -> { // ᛜ Ingwaz – diamond shape
                    moveTo(cx, cy - r); lineTo(cx + r * 0.7f, cy)
                    lineTo(cx, cy + r); lineTo(cx - r * 0.7f, cy); close()
                }
                22 -> { // ᛞ Dagaz – horizontal infinity / bow-tie
                    moveTo(cx - r * 0.6f, cy - r); lineTo(cx + r * 0.6f, cy + r)
                    moveTo(cx + r * 0.6f, cy - r); lineTo(cx - r * 0.6f, cy + r)
                    moveTo(cx - r * 0.6f, cy - r); lineTo(cx - r * 0.6f, cy + r)
                    moveTo(cx + r * 0.6f, cy - r); lineTo(cx + r * 0.6f, cy + r)
                }
                else -> { // 23: ᛟ Othala – diamond with two descending legs
                    moveTo(cx, cy - r); lineTo(cx + r * 0.6f, cy)
                    lineTo(cx, cy + r * 0.3f); lineTo(cx - r * 0.6f, cy); close()
                    moveTo(cx - r * 0.3f, cy + r * 0.3f); lineTo(cx - r * 0.6f, cy + r)
                    moveTo(cx + r * 0.3f, cy + r * 0.3f); lineTo(cx + r * 0.6f, cy + r)
                }
            }
        }

    val entryColor = androidx.compose.ui.graphics.Color(0xFF2080FF)
    val exitColor = androidx.compose.ui.graphics.Color(0xFFFF7000)
    val glowAlpha = 0.25f
    val runeSize = hexSizePx * 0.55f
    val runeR = runeSize * 0.30f
    val runeStroke = androidx.compose.ui.graphics.drawscope.Stroke(
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
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
            )
            drawPath(runePathFor(portal.runeIndex, ec.x, ec.y, runeR), color = entryColor, style = runeStroke)

            // Exit circle (orange) — same rune shape, different colour
            val xc = tileCenterPx(portal.exitPosition)
            drawCircle(color = exitColor.copy(alpha = glowAlpha), radius = runeSize * 0.75f, center = xc)
            drawCircle(
                color = exitColor,
                radius = runeSize * 0.42f,
                center = xc,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
            )
            drawPath(runePathFor(portal.runeIndex, xc.x, xc.y, runeR), color = exitColor, style = runeStroke)
        }
    }
}

internal fun displayedRiverTile(
    levelRiverTile: RiverTile?,
    sandboxPaintedRiverTile: RiverTile?,
): RiverTile? = sandboxPaintedRiverTile ?: levelRiverTile

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GameGrid(
    gameState: GameState,
    selectedDefenderType: DefenderType?,
    selectedDefenderId: Int?,
    selectedTargetId: Int?,
    selectedTargetPosition: Position?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction? = null,
    selectedBarricadeAction: BarricadeAction? = null,
    onCellClick: (Position) -> Unit,
    modifier: Modifier = Modifier,
    scrollToPosition: Position? = null,
    onScrollToPositionConsumed: (() -> Unit)? = null,
    isDemoMode: Boolean = false,
    demoHoveredPosition: Position? = null, // overrides the local hover in demo mode
    keyboardHoveredPosition: Position? = null, // overrides the local hover for keyboard build tile selection
    keyboardPlacementCursor: Position? = null, // keyboard cursor tile while placing a support object / targeting a spell
    selectedSupportObject: SupportObjectType? = null, // support object currently selected for placement (barricade/trap/magical trap)
    selectedSupportFief: de.egril.defender.model.FiefType? = null, // fief type currently selected for placement
    extraFocusTrigger: Int = 0,
) {
    // Establish a snapshot dependency on runtime map edits (sandbox tile painting) so the entire
    // grid recomposes and re-derives its tile sets from the updated level when a tile is repainted.
    @Suppress("UNUSED_VARIABLE")
    val mapEditVersion = gameState.mapEditVersion.value

    // State for pan and zoom
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    var isInitialized by remember { mutableStateOf(false) }

    // State for hover position (for tower placement preview)
    var localHoveredPosition by remember { mutableStateOf<Position?>(null) }
    // In demo mode use the externally-driven hover; keyboard hover overrides local hover for build tile preview
    val hoveredPosition: Position? = if (isDemoMode) demoHoveredPosition else (keyboardHoveredPosition ?: localHoveredPosition)

    val hexSize = 40.dp // Radius of hexagon (center to corner)

    // Initialize viewport: zoom-to-fit in demo mode, show spawn points otherwise
    LaunchedEffect(containerSize, contentSize) {
        if (!isInitialized &&
            containerSize.width > 0 &&
            containerSize.height > 0 &&
            contentSize.width > 0 &&
            contentSize.height > 0
        ) {
            if (isDemoMode) {
                // Zoom to 100% fit-to-screen so the entire map fills the available space
                // (controls panel height is locked at its max so no layout jumps occur)
                val fitScaleX = containerSize.width.toFloat() / contentSize.width.toFloat()
                val fitScaleY = containerSize.height.toFloat() / contentSize.height.toFloat()
                scale = minOf(fitScaleX, fitScaleY).coerceAtLeast(0.2f)
                offsetX = 0f
                offsetY = 0f
            } else {
                // Show spawn points (upper left) instead of center
                val contentWidth = contentSize.width * scale
                val contentHeight = contentSize.height * scale

                if (contentWidth > containerSize.width) {
                    val maxOffsetX = (contentWidth - containerSize.width) / 2
                    offsetX = maxOffsetX
                }

                if (contentHeight > containerSize.height) {
                    val maxOffsetY = (contentHeight - containerSize.height) / 2
                    offsetY = maxOffsetY
                }
            }
            isInitialized = true
        }
    }

    // Scroll to position when requested (e.g. bomb explosion)
    LaunchedEffect(scrollToPosition) {
        if (scrollToPosition != null && containerSize.width > 0 && contentSize.width > 0) {
            // Calculate pixel position of the target hex
            val hexSizePx = hexSize.value
            val hexWidthPx = hexSizePx * kotlin.math.sqrt(3.0).toFloat()
            val hexHeightPx = hexSizePx * 2f
            val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING
            val rowSpacingPx = -hexHeightPx + hexHeightPx * 0.75f + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
            val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
            val col = scrollToPosition.x
            val row = scrollToPosition.y
            val oddRowOffset = if (row % 2 == 1) oddOffsetPx else 0f
            val cellCenterX = col * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset
            val cellCenterY = row * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f
            // Clamp offset so the cell is centered in the viewport
            val maxOffsetX = maxOf(0f, (contentSize.width * scale - containerSize.width) / 2f)
            val maxOffsetY = maxOf(0f, (contentSize.height * scale - containerSize.height) / 2f)
            val targetOffsetX = (contentSize.width * scale / 2f - cellCenterX * scale).coerceIn(-maxOffsetX, maxOffsetX)
            val targetOffsetY = (contentSize.height * scale / 2f - cellCenterY * scale).coerceIn(-maxOffsetY, maxOffsetY)
            offsetX = targetOffsetX
            offsetY = targetOffsetY
            onScrollToPositionConsumed?.invoke()
        }
    }

    // Calculate target circle info for each tile
    // Find the selected defender and track its actions for dependency tracking
    val selectedDefender = gameState.defenders.find { it.id == selectedDefenderId }
    val selectedDefenderActions = selectedDefender?.actionsRemaining?.value
    val freyaShieldWallArcs = gameState.freyaShieldWallArcs()

    val targetCircleMap =
        remember(selectedTargetPosition, selectedDefenderId, selectedDefenderActions, gameState.defenders.size) {
            if (selectedTargetPosition == null || selectedDefenderId == null || selectedDefender == null) {
                emptyMap()
            } else {
                val attackType = selectedDefender.type.attackType

                // Don't show target circles if the tower has no action points left
                if (selectedDefender.actionsRemaining.value <= 0) {
                    emptyMap()
                } else {
                    val markerColor =
                        when (attackType) {
                            AttackType.AREA -> Color(0xFFFF5722) // Deep orange/red for fireball
                            AttackType.LASTING -> Color(0xFF4CAF50) // Green for acid
                            AttackType.MELEE, AttackType.RANGED -> Color.DarkGray // DarkGray for single-target
                            AttackType.NONE -> null // No target circles for special structures
                        }

                    if (markerColor == null) {
                        emptyMap()
                    } else {
                        val result = mutableMapOf<Position, TargetCircleInfo>()
                        val areaRadius = selectedDefender.areaEffectRadius
                        val isExtendedArea = areaRadius >= 2

                        // Check if target position has a magical bridge (which cannot be targeted by non-area attacks)
                        val hasMagicalBridge =
                            gameState.isBridgeAt(selectedTargetPosition) &&
                                gameState.getBridgeAt(selectedTargetPosition)?.type == BridgeType.MAGICAL

                        // Check if there's an enemy at the target position
                        val hasEnemy =
                            gameState.attackers.any {
                                it.position.value == selectedTargetPosition && !it.isDefeated.value
                            }

                        // Don't show target circles for non-area attacks on magical bridges UNLESS there's an enemy
                        if (hasMagicalBridge && !hasEnemy && attackType != AttackType.AREA && attackType != AttackType.LASTING) {
                            emptyMap()
                        } else {
                            // Central target tile
                            result[selectedTargetPosition] =
                                TargetCircleInfo.CentralTarget(
                                    color = markerColor,
                                    attackType = attackType,
                                    isExtendedArea = isExtendedArea,
                                )

                            // For AREA and LASTING attacks, add neighbor tiles that are on the path, or have bridges/enemies
                            if (attackType == AttackType.AREA || attackType == AttackType.LASTING) {
                                if (areaRadius == 1) {
                                    // Standard radius 1 - use getHexNeighbors
                                    val neighbors =
                                        selectedTargetPosition
                                            .getHexNeighbors()
                                            .filter { neighbor ->
                                                neighbor.x >= 0 &&
                                                    neighbor.x < gameState.level.gridWidth &&
                                                    neighbor.y >= 0 &&
                                                    neighbor.y < gameState.level.gridHeight &&
                                                    (
                                                        gameState.level.isOnPath(neighbor) ||
                                                            gameState.isBridgeAt(neighbor) ||
                                                            gameState.attackers.any {
                                                                it.position.value == neighbor && !it.isDefeated.value
                                                            }
                                                    )
                                            }

                                    for (neighbor in neighbors) {
                                        result[neighbor] =
                                            TargetCircleInfo.NeighborTarget(
                                                color = markerColor,
                                                attackType = attackType,
                                                centerPosition = selectedTargetPosition,
                                                thisPosition = neighbor,
                                                distanceFromCenter = 1,
                                                isExtendedArea = false,
                                            )
                                    }
                                } else {
                                    // Extended radius 2 (level 20+) - use getHexNeighborsWithinRadius
                                    val allNeighbors =
                                        selectedTargetPosition
                                            .getHexNeighborsWithinRadius(
                                                areaRadius,
                                                gameState.level.gridWidth,
                                                gameState.level.gridHeight,
                                            ).filter { neighbor ->
                                                gameState.level.isOnPath(neighbor) ||
                                                    gameState.isBridgeAt(neighbor) ||
                                                    gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                                            }

                                    for (neighbor in allNeighbors) {
                                        val distance = selectedTargetPosition.hexDistanceTo(neighbor)
                                        result[neighbor] =
                                            TargetCircleInfo.NeighborTarget(
                                                color = markerColor,
                                                attackType = attackType,
                                                centerPosition = selectedTargetPosition,
                                                thisPosition = neighbor,
                                                distanceFromCenter = distance,
                                                isExtendedArea = true,
                                            )
                                    }
                                }
                            }
                            if (LogConfig.ENABLE_UI_LOGGING) {
                                println("Target circle map: $result")
                            }
                            result
                        }
                    }
                }
            }
        }

    // Calculate spell area circle preview for ATTACK_AREA, ATTACK_AIMED, FEAR_SPELL, FEAR_SPELL_AREA, BOMB in targeting mode
    val spellAreaTargeting = gameState.spellTargeting.value
    val currentHoveredPosition = hoveredPosition
    val spellAreaCircleMap =
        remember(currentHoveredPosition, spellAreaTargeting?.activeSpell) {
            val activeSpell = spellAreaTargeting?.activeSpell
            // All spell targeting previews use the same magic (purple) color to distinguish them from tower attacks
            val spellColor = TargetCircleConstants.ATTACK_AREA_SPELL_COLOR
            // Bomb uses a distinct orange/red color to represent fire/explosion
            val bombColor = TargetCircleConstants.BOMB_SPELL_COLOR
            val bombExplosionRange = TargetCircleConstants.BOMB_SPELL_RADIUS
            when {
                (activeSpell == SpellType.ATTACK_AREA || activeSpell == SpellType.ATTACK_AIMED) && currentHoveredPosition != null -> {
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    result[currentHoveredPosition] =
                        TargetCircleInfo.CentralTarget(
                            color = spellColor,
                            attackType = AttackType.AREA,
                            isExtendedArea = true,
                        )
                    if (activeSpell == SpellType.ATTACK_AREA) {
                        val allNeighbors =
                            currentHoveredPosition
                                .getHexNeighborsWithinRadius(
                                    TargetCircleConstants.ATTACK_AREA_SPELL_RADIUS,
                                    gameState.level.gridWidth,
                                    gameState.level.gridHeight,
                                ).filter { neighbor ->
                                    gameState.level.isOnPath(neighbor) ||
                                        gameState.isBridgeAt(neighbor) ||
                                        gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                                }
                        for (neighbor in allNeighbors) {
                            val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                            result[neighbor] =
                                TargetCircleInfo.NeighborTarget(
                                    color = spellColor,
                                    attackType = AttackType.AREA,
                                    centerPosition = currentHoveredPosition,
                                    thisPosition = neighbor,
                                    distanceFromCenter = distance,
                                    isExtendedArea = true,
                                )
                        }
                    }
                    result
                }
                activeSpell == SpellType.BOMB && currentHoveredPosition != null -> {
                    // Show explosion range (3 hex tiles) around hovered position in orange
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    result[currentHoveredPosition] =
                        TargetCircleInfo.CentralTarget(
                            color = bombColor,
                            attackType = AttackType.AREA,
                            isExtendedArea = true,
                        )
                    val allNeighbors =
                        currentHoveredPosition.getHexNeighborsWithinRadius(
                            bombExplosionRange,
                            gameState.level.gridWidth,
                            gameState.level.gridHeight,
                        )
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = bombColor,
                                attackType = AttackType.AREA,
                                centerPosition = currentHoveredPosition,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                    result
                }
                activeSpell == SpellType.FEAR_SPELL_AREA && currentHoveredPosition != null -> {
                    // Area circles in magic color at radius 2 (like ATTACK_AREA)
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    result[currentHoveredPosition] =
                        TargetCircleInfo.CentralTarget(
                            color = spellColor,
                            attackType = AttackType.AREA,
                            isExtendedArea = true,
                        )
                    val allNeighbors =
                        currentHoveredPosition
                            .getHexNeighborsWithinRadius(
                                TargetCircleConstants.ATTACK_AREA_SPELL_RADIUS,
                                gameState.level.gridWidth,
                                gameState.level.gridHeight,
                            ).filter { neighbor ->
                                gameState.level.isOnPath(neighbor) ||
                                    gameState.isBridgeAt(neighbor) ||
                                    gameState.attackers.any { it.position.value == neighbor && !it.isDefeated.value }
                            }
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = spellColor,
                                attackType = AttackType.AREA,
                                centerPosition = currentHoveredPosition,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                    result
                }
                activeSpell == SpellType.COOLING_SPELL && currentHoveredPosition != null -> {
                    // Show turquoise circles at radius 2 around hovered position, only for path/spawn tiles
                    val coolingColor = TargetCircleConstants.COOLING_SPELL_COLOR
                    val result = mutableMapOf<Position, TargetCircleInfo>()
                    if (gameState.level.isEnemyTraversable(currentHoveredPosition)) {
                        result[currentHoveredPosition] =
                            TargetCircleInfo.CentralTarget(
                                color = coolingColor,
                                attackType = AttackType.AREA,
                                isExtendedArea = true,
                            )
                    }
                    val allNeighbors =
                        currentHoveredPosition
                            .getHexNeighborsWithinRadius(
                                TargetCircleConstants.COOLING_SPELL_RADIUS,
                                gameState.level.gridWidth,
                                gameState.level.gridHeight,
                            ).filter { neighbor ->
                                gameState.level.isEnemyTraversable(neighbor)
                            }
                    for (neighbor in allNeighbors) {
                        val distance = currentHoveredPosition.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = coolingColor,
                                attackType = AttackType.AREA,
                                centerPosition = currentHoveredPosition,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                    result
                }
                activeSpell == SpellType.FEAR_SPELL && currentHoveredPosition != null -> {
                    // Single-target circles on hovered enemy tile in magic color (like tower attack)
                    val enemyAtHover =
                        gameState.attackers.find {
                            it.position.value == currentHoveredPosition && !it.isDefeated.value
                        }
                    if (enemyAtHover != null) {
                        mapOf(
                            currentHoveredPosition to
                                TargetCircleInfo.CentralTarget(
                                    color = spellColor,
                                    attackType = AttackType.RANGED,
                                    isExtendedArea = false,
                                ),
                        )
                    } else {
                        emptyMap()
                    }
                }
                else -> emptyMap()
            }
        }

    // Calculate range circles for already-placed bombs (show explosion range, but no center rings)
    val activeBombEffects = gameState.activeSpellEffects.filter { it.spell == SpellType.BOMB && it.position != null }
    val placedBombCircleMap =
        remember(activeBombEffects.map { it.position }) {
            val bombColor = TargetCircleConstants.BOMB_SPELL_COLOR
            val bombExplosionRange = TargetCircleConstants.BOMB_SPELL_RADIUS
            val result = mutableMapOf<Position, TargetCircleInfo>()
            for (effect in activeBombEffects) {
                val bombPos = effect.position ?: continue
                // Intentionally skip the bomb tile itself (no center rings on placed bombs)
                val allNeighbors =
                    bombPos.getHexNeighborsWithinRadius(
                        bombExplosionRange,
                        gameState.level.gridWidth,
                        gameState.level.gridHeight,
                    )
                for (neighbor in allNeighbors) {
                    if (!result.containsKey(neighbor)) {
                        val distance = bombPos.hexDistanceTo(neighbor)
                        result[neighbor] =
                            TargetCircleInfo.NeighborTarget(
                                color = bombColor,
                                attackType = AttackType.AREA,
                                centerPosition = bombPos,
                                thisPosition = neighbor,
                                distanceFromCenter = distance,
                                isExtendedArea = true,
                            )
                    }
                }
            }
            result
        }

    val mapId = gameState.level.mapId
    val mapImageState = rememberMapImageState(mapId)
    val mapImagePainter = mapImageState.painter
    val useLevelMapImage = AppSettings.useLevelMapImage.value
    val hasMapImage = mapImagePainter != null && useLevelMapImage
    val isLoadingMapImage = mapImageState.isLoading
    val hexMapSizePx =
        remember(gameState.level.gridWidth, gameState.level.gridHeight, hexSize) {
            val hexSizePx = hexSize.value
            val hexWidthPx = hexSizePx * sqrt(3.0).toFloat()
            val hexHeightPx = hexSizePx * 2f
            val verticalSpacingPx = hexHeightPx * 0.75f
            val rowSpacingPx = -hexHeightPx + verticalSpacingPx + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
            val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
            val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING

            val maxOddOffset = if (gameState.level.gridHeight > 1) oddOffsetPx else 0f
            val widthPx = (gameState.level.gridWidth * hexWidthPx) + ((gameState.level.gridWidth - 1) * colSpacingPx) + maxOddOffset
            val heightPx = ((gameState.level.gridHeight - 1) * (hexHeightPx + rowSpacingPx)) + hexHeightPx

            widthPx.roundToInt() to heightPx.roundToInt()
        }

    // Pre-compute position lookup maps for O(1) per-cell lookups.
    // On large maps (e.g. 80×80 = 6 400 tiles) replacing O(n) list scans with O(1) map
    // lookups gives a significant speedup when all cells recompose (e.g. after clicking a
    // tower-buy button or placing a tower).
    //
    // Use derivedStateOf so that these maps are only rebuilt when the defenders/attackers
    // SnapshotStateLists actually change — not on every unrelated state change (e.g. coins
    // updating).  derivedStateOf tracks the State reads inside its lambda automatically, so
    // no explicit remember key is needed for the list-based maps.
    val defendersByPosition by remember {
        derivedStateOf { gameState.defenders.associateBy { it.position.value } }
    }
    val activeAttackersByPosition by remember {
        derivedStateOf {
            gameState.attackers
                .filter { !it.isDefeated.value }
                .associateBy { it.position.value }
        }
    }
    val dangerousAttackerPositions by remember {
        derivedStateOf {
            val movementSystem = EnemyMovementSystem(gameState, PathfindingSystem(gameState))
            gameState.attackers
                .filter { movementSystem.canReachTargetNextTurn(it) }
                .mapTo(mutableSetOf()) { it.position.value }
        }
    }

    // Pre-compute the selected defender once (replaces 6+ O(n) searches per GridCell).
    // selectedDefenderId is a plain Int? parameter (not a State), so derivedStateOf cannot
    // track it automatically — include it as a remember key so the derived state is
    // recreated when the selection changes.
    val selectedDefenderForGrid by remember(selectedDefenderId) {
        derivedStateOf { selectedDefenderId?.let { id -> gameState.defenders.find { it.id == id } } }
    }

    // Pre-compute the set of positions that are structurally buildable (build areas or flowing
    // river tiles). This is static for a given level — it never changes during gameplay.
    // Level.buildAreas is already a Set<Position>, so the union is O(|riverTiles|) at most.
    val structurallyBuildablePositions =
        remember(gameState.level) {
            val flowingRiver =
                gameState.level.riverTiles.entries
                    .filter { (_, rt) -> rt.flowDirection != RiverFlow.NONE && rt.flowDirection != RiverFlow.MAELSTROM }
                    .mapTo(mutableSetOf()) { (pos, _) -> pos }
            gameState.level.buildAreas + flowingRiver
        }

    // Subset of structurally buildable positions that are currently unoccupied (no defender,
    // no active attacker). derivedStateOf re-evaluates when defendersByPosition or
    // activeAttackersByPosition change. remember(gameState.level) re-creates the derived
    // state when the level changes (so the new structurallyBuildablePositions is captured).
    val buildableEmptyPositions by remember(gameState.level) {
        derivedStateOf {
            structurallyBuildablePositions.filterTo(mutableSetOf()) { pos ->
                !defendersByPosition.containsKey(pos) && !activeAttackersByPosition.containsKey(pos)
            }
        }
    }

    // Set of positions where a barricade currently supports tower placement
    // (healthPoints >= 100 AND no tower already placed on it).
    // derivedStateOf reads barricade.canSupportTower() and barricade.hasTower(), which
    // read MutableState values — so this re-evaluates when barricade HP or tower status changes.
    // remember(gameState.level) re-creates on level change for consistency with the other sets.
    val barricadeTowerBasePositions by remember(gameState.level) {
        derivedStateOf {
            gameState.barricades
                .filter { b -> b.canSupportTower() && !b.hasTower() }
                .mapTo(mutableSetOf()) { it.position }
        }
    }

    // Pre-compute whether the hovered position is buildable. Uses buildableEmptyPositions
    // (O(1) Set.contains) instead of the previous 5-step manual check.
    val hoveredPositionIsBuildableForGrid =
        selectedDefenderType != null &&
            hoveredPosition != null &&
            buildableEmptyPositions.contains(hoveredPosition)

    // Valid tiles for placing the currently selected support object (barricade / trap / magical
    // trap). Computed once per selection change so the per-cell hover preview below is an O(1)
    // Set lookup. Empty when no support object is being placed.
    val supportObjectPlacementPositions: Set<Position> by remember(gameState.level, selectedSupportObject) {
        derivedStateOf {
            selectedSupportObject
                ?.let { supportObjectPlacementTiles(gameState, it).toHashSet() }
                ?: emptySet()
        }
    }

    // Valid tiles for placing a fief support token. Empty when no fief is being placed.
    val supportFiefPlacementPositions: Set<Position> by remember(gameState.level, selectedSupportFief) {
        derivedStateOf {
            selectedSupportFief
                ?.let { supportFiefPlacementTiles(gameState, it).toHashSet() }
                ?: emptySet()
        }
    }

    // Pre-compute O(1) position-keyed lookup maps to replace O(N) .find{} / .any{} scans per tile.
    // derivedStateOf ensures each map is only rebuilt when its source list actually changes.
    // Compose's strong-skipping then skips cells whose resolved value remains null.
    val healingEffectsByPosition by remember {
        derivedStateOf { gameState.healingEffects.associateBy { it.position } }
    }
    val damageEffectsByPosition by remember {
        derivedStateOf { gameState.damageEffects.associateBy { it.position } }
    }
    val defeatedEnemyEffectsByPosition by remember {
        derivedStateOf { gameState.defeatedEnemyEffects.associateBy { it.position } }
    }
    val coinGainEffectsByPosition by remember {
        derivedStateOf { gameState.coinGainEffects.associateBy { it.position } }
    }
    val towerAttackEffectsByTargetPosition by remember {
        derivedStateOf { gameState.towerAttackEffects.associateBy { it.targetPosition } }
    }
    val fieldEffectsByPosition by remember {
        derivedStateOf { gameState.fieldEffects.associateBy { it.position } }
    }
    val trapsByPositionMap by remember {
        derivedStateOf { gameState.traps.associateBy { it.position } }
    }
    val barricadesByPositionMap by remember {
        derivedStateOf { gameState.barricades.associateBy { it.position } }
    }
    val constructionCompleteEffectsByPosition by remember {
        derivedStateOf { gameState.constructionCompleteEffects.associateBy { it.position } }
    }
    val enemySpawnEffectsByPosition by remember {
        derivedStateOf { gameState.enemySpawnEffects.associateBy { it.position } }
    }
    val trapTriggerEffectsByPosition by remember {
        derivedStateOf { gameState.trapTriggerEffects.associateBy { it.position } }
    }
    val enemyMoveEffectsByPosition by remember {
        derivedStateOf { gameState.enemyMoveEffects.associateBy { it.position } }
    }
    val dragonLevelChangeEffectsByPosition by remember {
        derivedStateOf { gameState.dragonLevelChangeEffects.associateBy { it.position } }
    }
    val mineDigEffectsByPosition by remember {
        derivedStateOf { gameState.mineDigEffects.associateBy { it.position } }
    }
    val fiefsByPositionMap by remember {
        derivedStateOf { gameState.fiefs.associateBy { it.position } }
    }
    val mushroomsByPositionMap by remember {
        derivedStateOf { gameState.mushrooms.associateBy { it.position } }
    }
    // Attack target position sets for O(1) membership checks
    val arrowAttackTargetPositions by remember {
        derivedStateOf { gameState.arrowAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val ballistaAttackTargetPositions by remember {
        derivedStateOf { gameState.ballistaAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val bowAttackTargetPositions by remember {
        derivedStateOf { gameState.bowAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val spearAttackTargetPositions by remember {
        derivedStateOf { gameState.spearAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val pikeAttackTargetPositions by remember {
        derivedStateOf { gameState.pikeAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val wizardAttackTargetPositions by remember {
        derivedStateOf { gameState.wizardAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    val alchemyAttackTargetPositions by remember {
        derivedStateOf { gameState.alchemyAttackEffects.mapTo(mutableSetOf()) { it.targetPosition } }
    }
    // Expanded area sets (wizard radius 1, alchemy radius 2) for AoE attack suppression
    val wizardAttackAreaPositions by remember {
        derivedStateOf {
            val result = mutableSetOf<Position>()
            gameState.wizardAttackEffects.forEach { effect ->
                result.add(effect.targetPosition)
                effect.targetPosition.getHexNeighbors().forEach { result.add(it) }
            }
            result
        }
    }
    val alchemyAttackAreaPositions by remember {
        derivedStateOf {
            val result = mutableSetOf<Position>()
            gameState.alchemyAttackEffects.forEach { effect ->
                for (dx in -2..2) {
                    for (dy in -2..2) {
                        val candidate = Position(effect.targetPosition.x + dx, effect.targetPosition.y + dy)
                        if (effect.targetPosition.hexDistanceTo(candidate) <= 2) {
                            result.add(candidate)
                        }
                    }
                }
            }
            result
        }
    }
    // Pre-compute map of position -> ArrowAttackEffect covering source, target, and path tiles.
    // Uses the same linear-interpolation algorithm as the private isOnArrowLinePath() function so
    // the set of covered tiles is identical to the original per-tile computation.
    val arrowAttackEffectsByAffectedPosition by remember {
        derivedStateOf {
            val result = mutableMapOf<Position, ArrowAttackEffect>()
            gameState.arrowAttackEffects.forEach { effect ->
                result[effect.sourcePosition] = effect
                result[effect.targetPosition] = effect
                val dx = effect.targetPosition.x - effect.sourcePosition.x
                val dy = effect.targetPosition.y - effect.sourcePosition.y
                val steps = maxOf(abs(dx), abs(dy))
                for (step in 1 until steps) {
                    val t = step.toFloat() / steps
                    val ix = (effect.sourcePosition.x + dx * t).roundToInt()
                    val iy = (effect.sourcePosition.y + dy * t).roundToInt()
                    result.getOrPut(Position(ix, iy)) { effect }
                }
            }
            result
        }
    }

    // Stable reference to onCellClick via rememberUpdatedState.
    //
    // Why this matters for performance:
    //   HexagonalMapView calls `content(position)` in a plain for-loop — a @Composable lambda.
    //   Inside that lambda, `onClick = { onCellClick(position) }` creates a NEW Function0 object
    //   on every invocation.  Compose's strong-skipping compares GridCell parameters by identity;
    //   a new lambda object always differs → GridCell is NEVER skipped → all 6,400 bodies run.
    //
    //   By using rememberUpdatedState we get a stable State<> reference that can be captured
    //   once (inside remember(position)) and read at call-time without becoming stale.
    val onCellClickState = rememberUpdatedState(onCellClick)

    Box(
        modifier =
            modifier
                .onSizeChanged { containerSize = it },
    ) {
        if (useLevelMapImage && isLoadingMapImage) {
            LevelLoadingScreen(modifier = Modifier.fillMaxSize())
        } else {
            HexagonalMapView(
                gridWidth = gameState.level.gridWidth,
                gridHeight = gameState.level.gridHeight,
                config =
                    HexagonalMapConfig(
                        hexSize = hexSize.value,
                        enableKeyboardNavigation = !isDemoMode, // Disable keyboard navigation in demo mode
                        enablePanNavigation = !isDemoMode, // Disable pan navigation in demo mode
                        panUpBinding = AppSettings.shortcutPanUp.value,
                        panDownBinding = AppSettings.shortcutPanDown.value,
                        panLeftBinding = AppSettings.shortcutPanLeft.value,
                        panRightBinding = AppSettings.shortcutPanRight.value,
                        minScale = if (isDemoMode) 0.2f else 0.5f, // Allow lower zoom in demo mode
                    ),
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                onScaleChange = { newScale -> scale = newScale },
                onOffsetChange = { newOffsetX, newOffsetY ->
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                },
                onActualContentSizeChange = { newContentSize ->
                    contentSize = newContentSize
                },
                focusTrigger = Pair(gameState.phase.value, extraFocusTrigger), // Request focus when game phase changes or after dialogs close
                modifier = Modifier.fillMaxSize(),
                backgroundContent =
                    if (hasMapImage) {
                        { measuredContentSize ->
                            val density = androidx.compose.ui.platform.LocalDensity.current
                            val targetWidthPx = maxOf(hexMapSizePx.first, measuredContentSize.width)
                            val targetHeightPx = maxOf(hexMapSizePx.second, measuredContentSize.height)
                            with(density) {
                                androidx.compose.foundation.Image(
                                    painter = mapImagePainter,
                                    contentDescription = null,
                                    modifier =
                                        Modifier
                                            .requiredWidth(targetWidthPx.toDp())
                                            .requiredHeight(targetHeightPx.toDp()),
                                    contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
                                )
                            }
                        }
                    } else {
                        null
                    },
                overlayContent = { measuredContentSize ->
                    val ballistaEffects = gameState.ballistaAttackEffects.toList()
                    if (ballistaEffects.isNotEmpty()) {
                        BallistaAttackOverlay(
                            effects = ballistaEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val bowEffects = gameState.bowAttackEffects.toList()
                    if (bowEffects.isNotEmpty()) {
                        BowAttackOverlay(
                            effects = bowEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val spearEffects = gameState.spearAttackEffects.toList()
                    if (spearEffects.isNotEmpty()) {
                        SpearAttackOverlay(
                            effects = spearEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val pikeEffects = gameState.pikeAttackEffects.toList()
                    if (pikeEffects.isNotEmpty()) {
                        PikeAttackOverlay(
                            effects = pikeEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val wizardEffects = gameState.wizardAttackEffects.toList()
                    if (wizardEffects.isNotEmpty()) {
                        WizardAttackOverlay(
                            effects = wizardEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val alchemyEffects = gameState.alchemyAttackEffects.toList()
                    if (alchemyEffects.isNotEmpty()) {
                        AlchemyAttackOverlay(
                            effects = alchemyEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val rocketEffects = gameState.rocketAttackEffects.toList()
                    if (rocketEffects.isNotEmpty()) {
                        RocketAttackOverlay(
                            effects = rocketEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val snotlingCannonEffects = gameState.snotlingCannonThrowEffects.toList()
                    if (snotlingCannonEffects.isNotEmpty()) {
                        SnotlingCannonThrowOverlay(
                            effects = snotlingCannonEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    val garokkWarCryEffects = gameState.garokkWarCryEffects.toList()
                    if (garokkWarCryEffects.isNotEmpty()) {
                        GarokkWarCryOverlay(
                            effects = garokkWarCryEffects,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                            animate = AppSettings.enableAnimations.value,
                        )
                    }
                    // Full-map falling-meteor shower for the "Sky is Falling" support power.
                    SkyIsFallingAnimation(
                        triggerKey = gameState.skyIsFallingTrigger.value,
                        contentSize = measuredContentSize,
                        animate = AppSettings.enableAnimations.value,
                    )
                    // Freya shield wall boundary: three curvy parallel lines (grey, blue, grey)
                    // drawn above the map following the outer edge of the shield wall formation.
                    if (freyaShieldWallArcs.isNotEmpty()) {
                        FreyaShieldWallMapOverlay(
                            shieldWallArcs = freyaShieldWallArcs,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                        )
                    }
                    // Rift portals: blue entry rune and orange exit rune for each active portal.
                    val activePortals = gameState.activePortals.toList()
                    if (activePortals.isNotEmpty()) {
                        RiftPortalOverlay(
                            portals = activePortals,
                            hexSizeDp = hexSize.value,
                            contentSize = measuredContentSize,
                        )
                    }
                },
            ) { position ->
                // Pre-compute the two hover-position-dependent booleans per cell.
                //
                // Why Boolean parameters instead of Position:
                //   When `hoveredPosition` changes (every mouse-move), passing it as a Position
                //   parameter to GridCell causes ALL 6,400 cells to recompose because their
                //   `hoveredPosition` parameter changed.  By pre-computing per-cell Booleans here
                //   and passing those instead, only the 1-2 cells whose Boolean value actually
                //   changed need to recompose — all others see the same `false` value as before
                //   and are skipped by Compose's strong-skipping pass.
                val isHovering = hoveredPosition == position
                val isBuildingMode = selectedDefenderType != null

                // Pre-compute isInPreviewRange here because it depends on hoveredPosition (which
                // is no longer a GridCell parameter).  The expensive path runs only when
                // hoveredPositionIsBuildableForGrid is true (i.e. a tower type is selected AND
                // the cursor is over a valid placement tile), keeping the common case cheap.
                val isInPreviewRange: Boolean =
                    if (
                        !isHovering && hoveredPositionIsBuildableForGrid
                    ) {
                        val dist = hoveredPosition.distanceTo(position)
                        val minRange = selectedDefenderType.minRange
                        val maxRange =
                            selectedDefenderType.maxRange
                                ?.let { minOf(selectedDefenderType.baseRange, it) }
                                ?: selectedDefenderType.baseRange
                        val onPathHere = gameState.level.isOnPath(position)
                        val spawnHere = gameState.level.isSpawnPoint(position)
                        val riverHere = gameState.level.isRiverTile(position)
                        val traversable = onPathHere || spawnHere
                        val occupiable = onPathHere || spawnHere || riverHere
                        val areaAttack =
                            selectedDefenderType.attackType == AttackType.AREA ||
                                selectedDefenderType.attackType == AttackType.LASTING
                        val validTarget = if (areaAttack) occupiable else traversable
                        dist >= minRange && dist <= maxRange && validTarget
                    } else {
                        false
                    }

                // Per-cell booleans that replace selectedDefenderType: only ~640 buildable cells
                // see a change when the buy button is clicked; the other ~5,760 stay at false → SKIPPED.

                // showPlacementPreview: only the 1 hovered buildable cell can be true.
                val showPlacementPreview =
                    isHovering &&
                        isBuildingMode &&
                        buildableEmptyPositions.contains(position)

                // Green-bordered buildable highlight — excludes the hovered cell (shows preview instead).
                val isBuildableAndEmpty =
                    isBuildingMode &&
                        buildableEmptyPositions.contains(position) &&
                        !showPlacementPreview

                // Barricade tower-base highlight — only for barricade cells with HP >= 100 and no tower.
                val canBeUsedAsTowerBase =
                    isBuildingMode &&
                        barricadeTowerBasePositions.contains(position) &&
                        !showPlacementPreview

                // previewDefenderType: non-null only for the 1 cell showing the placement preview.
                // All other cells get null → their parameter is null both before and after a buy-button
                // click → those cells are not marked for recomposition due to this parameter.
                val previewDefenderType: DefenderType? = if (showPlacementPreview) selectedDefenderType else null

                // isKeyboardPlacementCursor: only the single tile under the keyboard placement/targeting
                // cursor is true, so at most one cell recomposes when the cursor moves.
                val isKeyboardPlacementCursor = keyboardPlacementCursor != null && keyboardPlacementCursor == position

                // supportObjectPreviewType: non-null only for the single hovered tile that is a valid
                // placement target for the currently selected support object. Mirrors previewDefenderType
                // so hovering a support object over a valid tile shows the same ghost preview as the
                // matching tower-placed barricade/trap.
                val supportObjectPreviewType: SupportObjectType? =
                    if (isHovering && selectedSupportObject != null && supportObjectPlacementPositions.contains(position)) {
                        selectedSupportObject
                    } else {
                        null
                    }

                // supportObjectPlacementHighlightType: non-null for every valid placement target of the
                // currently selected support object, so all reachable tiles are highlighted the same way
                // a tower highlights its trap/barricade placement range (but without the range limit).
                val supportObjectPlacementHighlightType: SupportObjectType? =
                    if (selectedSupportObject != null && supportObjectPlacementPositions.contains(position)) {
                        selectedSupportObject
                    } else {
                        null
                    }

                // True when a fief support token is selected and this tile is a valid placement target.
                val supportFiefPlacementHighlight: Boolean =
                    selectedSupportFief != null && supportFiefPlacementPositions.contains(position)

                // Memoize the event-handler lambdas so Compose's strong-skipping can work correctly.
                //
                // Without memoization, `{ onCellClick(position) }` and `{ localHoveredPosition = ... }`
                // create NEW Function0/Function1 objects on every content-lambda invocation (6,400 per
                // GameGrid recomposition).  Compose compares GridCell parameters by identity for
                // function types, so new objects always differ → GridCell body runs for all 6,400 cells.
                //
                // With remember(position):
                //   • The same lambda object is returned on every subsequent recomposition (same position).
                //   • All other per-cell parameters are already stable (Booleans, enums, or stable data).
                //   • Result: Compose correctly skips GridCells whose parameters didn't change.
                //
                // onCellClickState (rememberUpdatedState) gives a stable State<> reference captured once;
                // reading .value inside the lambda avoids stale-closure issues if onCellClick ever changes.
                val cellOnClick = remember(position) { { onCellClickState.value(position) } }
                val cellOnHoverChange =
                    remember(position) {
                        { isHoveringChange: Boolean ->
                            localHoveredPosition = if (isHoveringChange) position else null
                        }
                    }

                // Sandbox: a tile repainted at runtime must show its new tile image even when the
                // original map is rendered from a single pre-rendered image. For such tiles we force a
                // non-transparent (opaque tile-image) background so the new type overlays the old map.
                val sandboxPaintedType =
                    if (gameState.level.isSandbox) gameState.sandboxPaintedTiles[position] else null
                val sandboxPaintedRiverTile =
                    if (gameState.level.isSandbox) gameState.sandboxPaintedRiverTiles[position] else null

                GridCell(
                    position = position,
                    gameState = gameState,
                    defender = defendersByPosition[position],
                    attacker = activeAttackersByPosition[position],
                    isDangerous = dangerousAttackerPositions.contains(position),
                    selectedDefender = selectedDefenderForGrid,
                    isHovering = isHovering,
                    isInPreviewRange = isInPreviewRange,
                    showPlacementPreview = showPlacementPreview,
                    isBuildableAndEmpty = isBuildableAndEmpty,
                    canBeUsedAsTowerBase = canBeUsedAsTowerBase,
                    previewDefenderType = previewDefenderType,
                    isKeyboardPlacementCursor = isKeyboardPlacementCursor,
                    supportObjectPreviewType = supportObjectPreviewType,
                    supportObjectPlacementHighlightType = supportObjectPlacementHighlightType,
                    supportFiefPlacementHighlight = supportFiefPlacementHighlight,
                    // NOTE: the null guard on selectedDefenderId/selectedTargetId is critical for
                    // correctness AND performance.  Without it, `null?.id == null` evaluates to
                    // `null == null = true`, so every cell without a defender/attacker becomes
                    // "selected" (yellow borders + diagonal stripes on 6,000+ tiles) and any
                    // transition from null→id triggers a mass recomposition of all 6,000+ cells.
                    isDefenderSelected =
                        selectedDefenderId != null &&
                            defendersByPosition[position]?.id == selectedDefenderId,
                    isTargetSelected =
                        selectedTargetId != null &&
                            activeAttackersByPosition[position]?.id == selectedTargetId,
                    selectedDefenderId = selectedDefenderId,
                    selectedMineAction = selectedMineAction,
                    selectedWizardAction = selectedWizardAction,
                    selectedBarricadeAction = selectedBarricadeAction,
                    targetCircleInfo = spellAreaCircleMap[position] ?: targetCircleMap[position] ?: placedBombCircleMap[position],
                    isSelectedAttackTarget = targetCircleMap[position] != null,
                    onClick = cellOnClick,
                    hexSize = hexSize,
                    onHoverChange = cellOnHoverChange,
                    healingEffect = healingEffectsByPosition[position],
                    damageEffect = damageEffectsByPosition[position],
                    deathEffect = defeatedEnemyEffectsByPosition[position],
                    coinGainEffect = coinGainEffectsByPosition[position],
                    towerAttackEffect = towerAttackEffectsByTargetPosition[position],
                    fieldEffect = fieldEffectsByPosition[position],
                    trap = trapsByPositionMap[position],
                    barricade = barricadesByPositionMap[position],
                    fief = fiefsByPositionMap[position],
                    mushroom = mushroomsByPositionMap[position],
                    constructionCompleteEffect = constructionCompleteEffectsByPosition[position],
                    enemySpawnEffect = enemySpawnEffectsByPosition[position],
                    trapTriggerEffect = trapTriggerEffectsByPosition[position],
                    enemyMoveEffect = enemyMoveEffectsByPosition[position],
                    dragonLevelChangeEffect = dragonLevelChangeEffectsByPosition[position],
                    mineDigEffect = mineDigEffectsByPosition[position],
                    arrowAttackEffect = arrowAttackEffectsByAffectedPosition[position],
                    isArrowTargetTile = arrowAttackTargetPositions.contains(position),
                    isBallistaTargetTile = ballistaAttackTargetPositions.contains(position),
                    isBowTargetTile = bowAttackTargetPositions.contains(position),
                    isSpearTargetTile = spearAttackTargetPositions.contains(position),
                    isPikeTargetTile = pikeAttackTargetPositions.contains(position),
                    isWizardTargetTile = wizardAttackTargetPositions.contains(position),
                    isAlchemyTargetTile = alchemyAttackTargetPositions.contains(position),
                    isInWizardAttackArea = wizardAttackAreaPositions.contains(position),
                    isInAlchemyAttackArea = alchemyAttackAreaPositions.contains(position),
                    useTransparentBackground = hasMapImage && sandboxPaintedType == null,
                    sandboxPaintedType = sandboxPaintedType,
                    sandboxPaintedRiverTile = sandboxPaintedRiverTile,
                )
            }

            MapControls(
                mapControlState =
                    MapControlState(
                        zoomLevel = scale,
                        offsetX = offsetX,
                        offsetY = offsetY,
                    ),
                onStateChange = { newState ->
                    val newScale = newState.zoomLevel
                    val (constrainedX, constrainedY) =
                        constrainMapOffsets(
                            newState.offsetX,
                            newState.offsetY,
                            newScale,
                            containerSize,
                            contentSize,
                        )
                    scale = newScale
                    offsetX = constrainedX
                    offsetY = constrainedY
                },
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (AppSettings.showMapSizeOverlay.value && contentSize.width > 0 && contentSize.height > 0) {
                        val contentWidthPx = contentSize.width
                        val contentHeightPx = contentSize.height
                        val realWidthPx = hexMapSizePx.first
                        val realHeightPx = hexMapSizePx.second
                        val viewportWidthPx = containerSize.width
                        val viewportHeightPx = containerSize.height
                        val contentWidthDp = with(density) { contentWidthPx.toDp() }
                        val contentHeightDp = with(density) { contentHeightPx.toDp() }
                        val realWidthDp = with(density) { realWidthPx.toDp() }
                        val realHeightDp = with(density) { realHeightPx.toDp() }
                        val viewportWidthDp = with(density) { viewportWidthPx.toDp() }
                        val viewportHeightDp = with(density) { viewportHeightPx.toDp() }
                        Surface(
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Text(
                                text =
                                    stringResource(
                                        Res.string.debug_map_size_overlay,
                                        realWidthDp.value.roundToInt(),
                                        realHeightDp.value.roundToInt(),
                                        realWidthPx,
                                        realHeightPx,
                                        contentWidthDp.value.roundToInt(),
                                        contentHeightDp.value.roundToInt(),
                                        contentWidthPx,
                                        contentHeightPx,
                                        viewportWidthDp.value.roundToInt(),
                                        viewportHeightDp.value.roundToInt(),
                                        viewportWidthPx,
                                        viewportHeightPx,
                                        offsetX.roundToInt(),
                                        offsetY.roundToInt(),
                                    ),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }

                    // Minimap
                    Box(
                        modifier = Modifier.size(120.dp),
                    ) {
                        HexagonMinimap(
                            level = gameState.level,
                            config =
                                MinimapConfig(
                                    showSpawnPoints = true,
                                    showTarget = true,
                                    showTowers = true,
                                    showEnemies = true,
                                    showViewport = true,
                                    minimapSizeDp = 120f,
                                ),
                            gameState = gameState,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            containerSize = containerSize,
                            contentSize = contentSize,
                            modifier = Modifier.fillMaxSize(),
                            onViewportDrag = { newOffsetX, newOffsetY ->
                                offsetX = newOffsetX
                                offsetY = newOffsetY
                            },
                        )
                    }
                }
            }
        } // end else !isLoadingMapImage
    }
}

@Composable
fun GridCell(
    position: Position,
    gameState: GameState,
    defender: Defender?,
    attacker: Attacker?,
    isDangerous: Boolean = false,
    selectedDefender: Defender?,
    // isHovering and isInPreviewRange replace the old hoveredPosition: Position? and
    // hoveredPositionIsBuildable: Boolean parameters.  Passing per-cell Booleans means only
    // the 1-2 cells whose hover state actually changed receive a different value and need to
    // recompose; the remaining 6,398 cells keep the same `false` value and are skipped.
    isHovering: Boolean,
    isInPreviewRange: Boolean,
    // showPlacementPreview, isBuildableAndEmpty, canBeUsedAsTowerBase, and previewDefenderType
    // replace selectedDefenderType: DefenderType?.  Pre-computing per-cell Booleans in GameGrid's
    // content lambda means non-buildable cells (which stay `false`) are skipped by Compose when
    // selectedDefenderType changes (buy button click) — only ~10 % of cells recompose.
    showPlacementPreview: Boolean,
    isBuildableAndEmpty: Boolean,
    canBeUsedAsTowerBase: Boolean,
    // previewDefenderType is non-null only for the 1 cell showing the placement preview icon.
    // All other cells receive null and are not marked for recomposition when the selection changes.
    previewDefenderType: DefenderType?,
    // True only for the single tile under the keyboard placement/targeting cursor (support object
    // placement or spell targeting). Renders a distinct bright cursor border/tint so keyboard users
    // can see which tile the place key will act on.
    isKeyboardPlacementCursor: Boolean = false,
    // Non-null only for the single hovered tile that is a valid placement target for the currently
    // selected support object. Drives the ghost preview so support-placed barricades/traps show the
    // same preview as their tower-placed counterparts.
    supportObjectPreviewType: SupportObjectType? = null,
    // Non-null for every tile that is a valid placement target for the currently selected support
    // object (barricade / dwarven trap / magical trap). Drives the same range-style tile highlight
    // (border + diagonal stripes) that a tower shows when placing the equivalent item, but without
    // the tower's range restriction.
    supportObjectPlacementHighlightType: SupportObjectType? = null,
    // True when a fief support token is selected and this tile is a valid placement target.
    supportFiefPlacementHighlight: Boolean = false,
    isDefenderSelected: Boolean,
    isTargetSelected: Boolean,
    selectedDefenderId: Int?,
    selectedMineAction: MineAction?,
    selectedWizardAction: WizardAction? = null,
    selectedBarricadeAction: BarricadeAction? = null,
    targetCircleInfo: TargetCircleInfo?,
    // True when this tile is affected by the currently selected tower attack (the selected target
    // for single-target attacks, or a tile within the blast area for AREA/LASTING attacks). Used to
    // gate the enemy attack damage/lethality/immunity preview (issue #591).
    isSelectedAttackTarget: Boolean = false,
    onClick: () -> Unit,
    hexSize: androidx.compose.ui.unit.Dp = 48.dp,
    onHoverChange: ((Boolean) -> Unit)? = null,
    useTransparentBackground: Boolean = false,
    // Sandbox runtime map edits mutate the non-observable `level` field in place, so Compose cannot
    // detect them by reading gameState.level. These two parameters carry the repainted tile type and
    // river flow for this exact cell; when either changes (e.g. river -> different river flow, or
    // river -> NO_PLAY) the parameter comparison differs and Compose recomposes this cell immediately,
    // re-reading the updated level. Without them a repaint that leaves other parameters unchanged
    // (notably repainting one river tile over another) would be skipped and never re-render.
    sandboxPaintedType: de.egril.defender.editor.TileType? = null,
    sandboxPaintedRiverTile: RiverTile? = null,
    // Pre-resolved effect values passed from GameGrid (O(1) map lookups instead of O(N) searches)
    healingEffect: HealingEffect? = null,
    damageEffect: DamageEffect? = null,
    deathEffect: EnemyDeathEffect? = null,
    coinGainEffect: CoinGainEffect? = null,
    towerAttackEffect: TowerAttackEffect? = null,
    fieldEffect: FieldEffect? = null,
    trap: Trap? = null,
    barricade: Barricade? = null,
    fief: de.egril.defender.model.Fief? = null,
    mushroom: de.egril.defender.model.Mushroom? = null,
    constructionCompleteEffect: TowerConstructionEffect? = null,
    enemySpawnEffect: EnemySpawnEffect? = null,
    trapTriggerEffect: TrapTriggerEffect? = null,
    enemyMoveEffect: EnemyMoveEffect? = null,
    dragonLevelChangeEffect: DragonLevelChangeEffect? = null,
    mineDigEffect: MineDigEffect? = null,
    arrowAttackEffect: ArrowAttackEffect? = null,
    isArrowTargetTile: Boolean = false,
    isBallistaTargetTile: Boolean = false,
    isBowTargetTile: Boolean = false,
    isSpearTargetTile: Boolean = false,
    isPikeTargetTile: Boolean = false,
    isWizardTargetTile: Boolean = false,
    isAlchemyTargetTile: Boolean = false,
    isInWizardAttackArea: Boolean = false,
    isInAlchemyAttackArea: Boolean = false,
) {
    val isDarkMode = de.egril.defender.ui.settings.AppSettings.isDarkMode.value

    val isSpawnPoint = gameState.level.isSpawnPoint(position)
    val isTarget = gameState.level.isTargetPosition(position)
    val isOnPath = gameState.level.isOnPath(position)
    val isBuildArea = gameState.level.isBuildArea(position)
    val isRiverTile = gameState.level.isRiverTile(position)
    // Shorthand combinations used for attack targeting and spell area checks
    val isEnemyTraversable = isOnPath || isSpawnPoint
    val isEnemyOccupiable = isOnPath || isSpawnPoint || isRiverTile
    // defender and attacker are now passed as parameters (pre-computed in GameGrid)

    // Check if a dragon is targeting the mine at this position
    val dragonIsTargetingMine =
        defender != null &&
            defender.type == DefenderType.DWARVEN_MINE &&
            gameState.attackers.any { it.targetMineId.value == defender.id && !it.isDefeated.value }

    // Determine the tile type for background image loading. Prefer the sandbox-painted river tile so
    // a runtime repaint (which mutates the non-observable level in place) is reflected immediately.
    val riverTile = displayedRiverTile(gameState.level.getRiverTile(position), sandboxPaintedRiverTile)
    val isMaelstrom = riverTile?.flowDirection == RiverFlow.MAELSTROM

    val tileType =
        sandboxPaintedType
            ?: when {
                isSpawnPoint -> de.egril.defender.editor.TileType.SPAWN_POINT
                isTarget -> de.egril.defender.editor.TileType.TARGET
                isRiverTile -> de.egril.defender.editor.TileType.RIVER
                isOnPath -> de.egril.defender.editor.TileType.PATH
                isBuildArea -> de.egril.defender.editor.TileType.BUILD_AREA
                else -> de.egril.defender.editor.TileType.NO_PLAY
            }

    // Get tile background painter (will be null if images are disabled or not available)
    // Suppress tile image when unit backgrounds are ON so the colored background is visible:
    // - For enemy tiles: the red background must not be covered by the tile texture.
    // - For ready towers on build areas: use the colored tower background instead.
    // When unit backgrounds are OFF (transparent), always show the tile image so units blend into terrain.
    val shouldShowTileImage =
        !(
            AppSettings.showUnitTowerBackground.value &&
                (attacker != null || (defender != null && defender.isReady && isBuildArea))
        )
    val tilePainter =
        if (shouldShowTileImage && (!useTransparentBackground || isMaelstrom)) {
            TileImageProvider.getTilePainter(tileType, isMaelstrom = isMaelstrom)
        } else {
            null
        }

    // When animations are enabled, delay showing fireball / acid field-effect markers on the tile
    // until the attack animation has finished (projectile flight + impact flash).
    // When animations are off, or when the effect is from a prior turn (no matching attack overlay
    // is active), show it immediately.
    val animate = AppSettings.enableAnimations.value
    var showFieldEffect by remember { mutableStateOf(false) }
    if (fieldEffect != null || isInWizardAttackArea || isInAlchemyAttackArea) {
        LaunchedEffect(
            fieldEffect?.position?.x,
            fieldEffect?.position?.y,
            fieldEffect?.type,
            fieldEffect?.defenderId,
            isInWizardAttackArea,
            isInAlchemyAttackArea,
            animate,
        ) {
            when {
                fieldEffect == null -> showFieldEffect = false
                animate && isInWizardAttackArea -> {
                    showFieldEffect = false
                    kotlinx.coroutines.delay(
                        GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS +
                            GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS,
                    )
                    showFieldEffect = true
                }
                animate && isInAlchemyAttackArea -> {
                    showFieldEffect = false
                    kotlinx.coroutines.delay(
                        GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS +
                            GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS,
                    )
                    showFieldEffect = true
                }
                else -> showFieldEffect = true
            }
        }
    } else {
        SideEffect { showFieldEffect = false }
    }
    // Use effectiveFieldEffect for all visual rendering; raw fieldEffect is still used for
    // gameplay logic (trap placement detection etc.) so the game state stays accurate.
    val effectiveFieldEffect: FieldEffect? = if (showFieldEffect) fieldEffect else null

    // Check if this tile is in a cooling spell area (show snowflake on affected path tiles)
    val isInCoolingArea =
        isEnemyTraversable &&
            gameState.activeSpellEffects.any { effect ->
                effect.spell == SpellType.COOLING_SPELL &&
                    effect.position != null &&
                    position.hexDistanceTo(effect.position) <= 2
            }

    // Cooling area turns remaining (for active cooling effects on this tile)
    val coolingAreaTurnsRemaining: Int? =
        if (isInCoolingArea) {
            gameState.activeSpellEffects
                .filter { effect ->
                    effect.spell == SpellType.COOLING_SPELL &&
                        effect.position != null &&
                        position.hexDistanceTo(effect.position) <= 2
                }.minOfOrNull { it.turnsRemaining }
        } else {
            null
        }

    // Is this tile part of a cooling spell placement preview?
    val isCoolingSpellPreview =
        targetCircleInfo != null &&
            gameState.spellTargeting.value?.activeSpell == SpellType.COOLING_SPELL

    // Check for active bomb spell effect at this position
    val bombEffect =
        gameState.activeSpellEffects.find {
            it.spell == SpellType.BOMB && it.position == position
        }

    // Check for bomb explosion visual effect at this position
    val bombExplosion =
        gameState.bombExplosionEffects.find { explosion ->
            explosion.center == position || explosion.affectedPositions.contains(position)
        }
    // Check if this tile is a valid spell target
    val spellTargeting = gameState.spellTargeting.value
    val isValidSpellTarget =
        if (spellTargeting != null) {
            when (spellTargeting.activeSpell.targetType) {
                de.egril.defender.model.SpellTargetType.ENEMY -> {
                    val enemyHere = gameState.attackers.find { it.position.value == position && !it.isDefeated.value }
                    enemyHere != null && spellTargeting.validTargets.contains(enemyHere)
                }
                de.egril.defender.model.SpellTargetType.TOWER -> {
                    val towerHere = gameState.defenders.find { it.position.value == position }
                    towerHere != null && spellTargeting.validTargets.contains(towerHere)
                }
                de.egril.defender.model.SpellTargetType.POSITION -> {
                    spellTargeting.validTargets.contains(position)
                }
                else -> false
            }
        } else {
            false
        }
    val selectedDefenderForRange = selectedDefender
    val hasDoubleReachBuff =
        selectedDefenderForRange?.let { sel ->
            gameState.activeSpellEffects.any { it.spell == SpellType.DOUBLE_TOWER_REACH && it.defenderId == sel.id }
        } ?: false
    val cellIsInRange =
        selectedDefenderForRange?.let { sel ->
            if (sel.position.value == position) {
                false // Don't highlight the defender's own cell
            } else {
                val distance = sel.position.value.distanceTo(position)
                val effectiveRange = gameState.effectiveRange(sel)
                distance >= sel.type.minRange && distance <= effectiveRange
            }
        } ?: false
    // Tiles that are in range ONLY because of the double-reach spell (beyond normal range)
    val cellIsInDoubleReachOnlyRange =
        if (hasDoubleReachBuff) {
            val sel = selectedDefenderForRange
            if (sel.position.value == position) {
                false
            } else {
                val distance = sel.position.value.distanceTo(position)
                distance >= sel.type.minRange && distance > sel.range && distance <= sel.range * 2
            }
        } else {
            false
        }

    // Attack damage / lethality / immunity preview shown at the left border of an enemy that is
    // affected by the currently selected tower attack: the selected target for single-target
    // attacks, or any enemy within the blast area for AREA/LASTING attacks. Only shown when no
    // build/trap/barricade placement mode is active (issue #591). Computed here (where
    // selectedDefender is known) and passed into GridCellContent for rendering.
    val attackPreview: EnemyAttackPreview? =
        run {
            val sel = selectedDefender
            if (attacker == null ||
                sel == null ||
                sel.type.attackType == AttackType.NONE ||
                !sel.isReady ||
                sel.actionsRemaining.value <= 0 ||
                !isSelectedAttackTarget ||
                selectedMineAction != null ||
                selectedWizardAction != null ||
                selectedBarricadeAction != null
            ) {
                null
            } else {
                val hasDoubleLevelBuff =
                    gameState.activeSpellEffects.any {
                        it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == sel.id
                    }
                enemyAttackPreview(attacker, sel, hasDoubleLevelBuff)
            }
        }

    // Calculate hover preview for trap placement
    val isHoveringForTrapPreview = isHovering
    val isTrapPlacementMode = selectedMineAction == MineAction.BUILD_TRAP || selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP

    // Check if this tile is valid for trap placement (on path, in range, no enemy, no existing trap, no field effects)
    val isValidTrapPlacement =
        if (isTrapPlacementMode && isHoveringForTrapPreview && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                val distance = sel.position.value.distanceTo(position)
                val hasEnemy = attacker != null
                val hasTrap = trap != null
                val hasFieldEffect = fieldEffect != null
                val hasFief = fief != null
                val hasMushroom = mushroom != null
                isOnPath && distance <= sel.range && !hasEnemy && !hasTrap && !hasFieldEffect && !hasFief && !hasMushroom
            } ?: false
        } else {
            false
        }

    val showTrapPreview = isValidTrapPlacement

    // showPlacementPreview, isBuildableAndEmpty, and canBeUsedAsTowerBase are pre-computed
    // in GameGrid's content lambda and passed as parameters (see GameGrid { position -> } block).

    // Barricade placement range detection (3 tiles, yellow borders for empty path tiles)
    val isBarricadePlacement = selectedBarricadeAction == BarricadeAction.BUILD_BARRICADE
    val cellIsInBarricadeRange =
        if (isBarricadePlacement && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                // Check if within 3 tiles range
                val distance = sel.position.value.distanceTo(position)
                val isInRange = distance > 0 && distance <= 3
                // Check if empty path tile (no defender, no attacker, can have existing barricade for reinforcement)
                val isEmptyPath = isOnPath && defender == null && attacker == null && fief == null && mushroom == null
                isInRange && isEmptyPath
            } ?: false
        } else {
            false
        }

    // Show barricade preview when hovering over valid barricade placement tile
    val showBarricadePreview = isBarricadePlacement && isHovering && cellIsInBarricadeRange

    // Mine trap placement range detection (path tiles within range of selected mine)
    val isMineTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP
    val cellIsValidForMineTrapPlacement =
        if (isMineTrapPlacement && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                val distance = sel.position.value.distanceTo(position)
                val isInRange = distance > 0 && distance <= sel.range
                val isEmptyPath = isOnPath && attacker == null && trap == null && fieldEffect == null && fief == null && mushroom == null
                isInRange && isEmptyPath
            } ?: false
        } else {
            false
        }

    // Magical trap placement range detection (path tiles within range of selected wizard)
    val isMagicalTrapPlacement = selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    val cellIsValidForMagicalTrapPlacement =
        if (isMagicalTrapPlacement && selectedDefenderId != null) {
            selectedDefender?.let { sel ->
                val distance = sel.position.value.distanceTo(position)
                val isInRange = distance > 0 && distance <= sel.range
                val isEmptyPath = isOnPath && attacker == null && trap == null && fieldEffect == null && fief == null && mushroom == null
                isInRange && isEmptyPath
            } ?: false
        } else {
            false
        }

    // Support-object placement highlight (barricade / dwarven trap / magical trap placed from the
    // support bar). Mirrors the tower-placement range highlight above, but the set of valid tiles
    // is supplied by the caller (supportObjectPlacementHighlightType) and has no range restriction.
    val cellIsSupportBarricadePlacement = supportObjectPlacementHighlightType == SupportObjectType.BARRICADE
    val cellIsSupportDwarvenTrapPlacement = supportObjectPlacementHighlightType == SupportObjectType.DWARVEN_TRAP
    val cellIsSupportMagicalTrapPlacement = supportObjectPlacementHighlightType == SupportObjectType.MAGICAL_TRAP

    // Base background color based on area type - ALWAYS visible
    // Build areas adjacent to path allow tower placement
    val baseBackgroundColor =
        when {
            isBuildArea -> GamePlayColors.BuildStrip // Medium green for strips adjacent to path
            isOnPath -> GamePlayColors.Path // Cream/beige for enemy path
            isRiverTile -> GamePlayColors.River // Blue for river tiles
            else -> GamePlayColors.NonPlayable // Light gray for off-path areas (non-playable)
        }

    // Check if attacker on this tile is frozen (freeze spell)
    val attackerIsFrozen =
        attacker != null &&
            gameState.activeSpellEffects.any {
                it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
            }

    // Check if cooling spell reduces this attacker's movement to 0
    val coolingReducesAttackerToZero =
        attacker != null &&
            isInCoolingArea &&
            run {
                val penalizedSpeed = maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
                maxOf(0, penalizedSpeed - 1) == 0
            }

    // Monotonic attack trigger counter: increments on every individual attack, even when the same
    // tile is attacked multiple times in a turn (bypasses the per-tile deduplication in
    // towerAttackEffects).  Using this as a LaunchedEffect key guarantees the suppression
    // coroutine re-fires for every new attack, not just the first one per tile.
    val towerAttackCount =
        if (animate) {
            gameState.attackTriggerCount.value
        } else {
            0
        }

    // Suppress the red background on enemy tiles during attack animations.
    // * Directly targeted / AoE tiles: suppress for the precise projectile flight + impact window.
    // * All other live enemy tiles: suppress for the maximum possible animation duration so all
    //   enemies visually "de-highlight" while any attack is running, then restore automatically.
    //   towerAttackCount is used as a key so this fires for EVERY new attack (unlike a boolean
    //   which only changes from false → true once per turn).
    val anyTowerAttackActive = towerAttackCount > 0
    var suppressEnemyBackground by remember { mutableStateOf(false) }
    if (attacker != null) {
        LaunchedEffect(
            towerAttackEffect?.turnNumber,
            towerAttackEffect?.targetPosition,
            isInWizardAttackArea,
            isInAlchemyAttackArea,
            towerAttackCount,
            animate,
        ) {
            if (!animate) {
                suppressEnemyBackground = false
                return@LaunchedEffect
            }
            val hasDirectAttack = towerAttackEffect != null
            val hasAoEAttack = isInWizardAttackArea || isInAlchemyAttackArea
            if (hasDirectAttack || hasAoEAttack) {
                // Directly targeted or AoE tile: use precise per-tower-type timing.
                suppressEnemyBackground = true
                val flightDelay: Long =
                    when {
                        towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                        towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        else -> 0L
                    }
                kotlinx.coroutines.delay(flightDelay + GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS)
                suppressEnemyBackground = false
            } else if (anyTowerAttackActive) {
                // Non-targeted live enemy tile: suppress for the maximum possible animation duration
                // so it de-highlights with all other enemies, then restores automatically.
                suppressEnemyBackground = true
                kotlinx.coroutines.delay(
                    GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS +
                        GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS,
                )
                suppressEnemyBackground = false
            } else {
                suppressEnemyBackground = false
            }
        }
    } else {
        SideEffect {
            suppressEnemyBackground = false
        }
    }

    val isDeathEffectActive = deathEffect != null && attacker == null
    val enemyBgSuppressed = suppressEnemyBackground

    // Apply slight tint for selection states, but keep base color visible
    // Override with red background for enemy units and colored background for defenders
    // During INITIAL_BUILDING phase, don't apply any selection tints
    // Field effects also modify the background color
    // Special case: Keep river background visible for defenders on rafts
    val backgroundColor =
        when {
            // Keyboard placement/targeting cursor — bright cyan tint so the active tile stands out.
            isKeyboardPlacementCursor -> Color(0xFF00E5FF).copy(alpha = 0.45f)
            attackerIsFrozen || coolingReducesAttackerToZero -> TargetCircleConstants.COOLING_SPELL_COLOR.copy(alpha = 0.5f) // Turquoise background for frozen/cooled-to-zero enemies
            attacker != null && enemyBgSuppressed -> if (useTransparentBackground) Color.Transparent else baseBackgroundColor
            attacker != null ->
                if (AppSettings.showUnitTowerBackground.value) {
                    GamePlayColors.Error
                } else if (useTransparentBackground) {
                    Color.Transparent
                } else {
                    baseBackgroundColor
                }
            defender != null && isRiverTile -> {
                // Keep river blue background visible for defenders on rafts only when unit backgrounds are enabled.
                // When unit backgrounds are off: transparent if level map is shown (shows through), otherwise river color.
                if (!AppSettings.showUnitTowerBackground.value && useTransparentBackground) Color.Transparent else GamePlayColors.River
            }
            defender != null ->
                if (AppSettings.showUnitTowerBackground.value) {
                    when {
                        !defender.isReady -> GamePlayColors.Building
                        defender.actionsRemaining.value > 0 -> GamePlayColors.Info
                        else -> GamePlayColors.InfoLight
                    }
                } else {
                    // When unit backgrounds are off: transparent if level map is shown (level map visible through tile),
                    // otherwise use terrain color so the tower tile blends with its surroundings instead of showing
                    // the Material theme's white surface color.
                    if (useTransparentBackground) Color.Transparent else baseBackgroundColor
                }

            effectiveFieldEffect != null -> {
                when (effectiveFieldEffect.type) {
                    FieldEffectType.FIREBALL -> GamePlayColors.Warning.copy(alpha = 0.5f) // Orange tint for fireball
                    FieldEffectType.ACID -> GamePlayColors.Success.copy(alpha = 0.6f) // Green tint for acid
                    FieldEffectType.WEB -> Color(0xFF8E7CC3).copy(alpha = 0.5f) // Violet tint for spider web
                    FieldEffectType.BURNING_TILE -> Color(0xFFFF4500).copy(alpha = 0.55f) // Red-orange for burning tile
                    FieldEffectType.SHADOW_FOG -> Color(0xFF2A003A).copy(alpha = 0.65f) // Black-violet fog
                }
            }

            trap != null -> GamePlayColors.Trap.copy(alpha = 0.6f) // Brown tint for trap

            barricade != null -> Color(0xFF795548).copy(alpha = 0.5f) // Brown tint for barricade

            // Bomb explosion overlay - bright orange/red when explosion is happening
            bombExplosion != null -> Color(0xFFFF3D00).copy(alpha = 0.7f) // Bright red-orange for explosion

            // Active bomb on tile - dark red/amber tint with countdown
            bombEffect != null -> Color(0xFFFF6F00).copy(alpha = 0.4f) // Amber tint for bomb

            // Barricade placement range - yellow tint for tiles in range
            cellIsInBarricadeRange || cellIsSupportBarricadePlacement -> GamePlayColors.Yellow.copy(alpha = 0.3f) // Light yellow for barricade placement range

            // Tower placement preview - highlight the hovered build tile differently than range tiles
            showPlacementPreview -> GamePlayColors.Yellow.copy(alpha = 0.4f) // Light yellow for the build tile being hovered
            isInPreviewRange -> GamePlayColors.Success.copy(alpha = 0.2f) // Very light green for range preview tiles

            // Spell targeting highlight - purple tint for valid spell target position tiles
            // Not shown for fear spells (target circles provide the visual indicator)
            isValidSpellTarget &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> Color(0xFF9C27B0).copy(alpha = 0.25f) // Light purple for valid spell target positions

            isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.7f)
            isTargetSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> baseBackgroundColor.copy(alpha = 0.8f)
            else -> baseBackgroundColor // No selection highlighting during placement or in initial phase
        }

    val finalBackgroundColor =
        if (useTransparentBackground &&
            attacker == null &&
            defender == null &&
            effectiveFieldEffect == null &&
            trap == null &&
            barricade == null &&
            bombEffect == null &&
            bombExplosion == null
        ) {
            Color.Transparent
        } else {
            backgroundColor
        }

    // Border color - use borders to indicate entities instead of background
    // For range visualization, show green border on path tiles OR river tiles in range (only if tower has actions)
    val showRange =
        if (selectedDefenderId != null) {
            selectedDefender?.isReady == true && selectedDefender.actionsRemaining.value > 0
        } else {
            false
        }

    // When placing trap, don't show green border on tiles with enemies
    val isTrapPlacement = selectedMineAction == MineAction.BUILD_TRAP || selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP
    val hasEnemy = attacker != null
    val canPlaceTrapHere = !hasEnemy || !isTrapPlacement

    // Check if defender has area attack capability
    val hasAreaAttack =
        if (selectedDefenderId != null) {
            selectedDefender?.type?.attackType == AttackType.AREA || selectedDefender?.type?.attackType == AttackType.LASTING
        } else {
            false
        }

    // Enemy-occupiable tiles are valid targets for area attacks; enemy-traversable for single-target
    val isValidTargetTile =
        if (hasAreaAttack) {
            isEnemyOccupiable
        } else {
            isEnemyTraversable
        }

    val borderColor =
        when {
            // Keyboard placement/targeting cursor — bright cyan border for the active tile.
            isKeyboardPlacementCursor -> Color(0xFF00B8D4)
            // Tower placement preview - dashed borders for preview (we'll handle this with Canvas later)
            showPlacementPreview -> GamePlayColors.Yellow // Yellow border for hovered build tile
            isInPreviewRange -> GamePlayColors.Success // Green border for range preview tiles

            // Barricade and trap placement range - brown borders (light brown diagonal stripes)
            cellIsInBarricadeRange ||
                cellIsValidForMineTrapPlacement ||
                cellIsSupportBarricadePlacement ||
                cellIsSupportDwarvenTrapPlacement -> GamePlayColors.TrapPlacementHighlight // Brown border for barricade/trap placement range

            // Magical trap placement range - lilac borders
            cellIsValidForMagicalTrapPlacement || cellIsSupportMagicalTrapPlacement -> GamePlayColors.MagicalTrapPlacementHighlight // Lilac border for magical trap placement range

            // Buildable tile highlighting - lighter green borders with dashed line when tower type is selected
            isBuildableAndEmpty || canBeUsedAsTowerBase -> GamePlayColors.BuildableHighlight // Lighter green border for buildable tiles and tower bases

            // Double-reach-only tiles: thin purple solid border
            cellIsInDoubleReachOnlyRange && isValidTargetTile && showRange && canPlaceTrapHere -> SpellDoubleReachColor

            cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> GamePlayColors.Success // Green border for tiles in range (path or river for area attacks)
            isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> GamePlayColors.Yellow // Yellow border for selected defender (not during initial building)

            // Spell targeting highlight - purple border for valid spell targets (enemies, towers, positions)
            // Not shown for fear spells (target circles provide the visual indicator)
            isValidSpellTarget &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> Color(0xFF9C27B0) // Purple border for valid spell targets
            isDangerous -> GamePlayColors.Error

            isSpawnPoint -> GamePlayColors.WarningDark // Darker orange border for spawn in dark mode
            isTarget -> GamePlayColors.Success // Green border for target (adapts to dark mode automatically)
            attacker != null && !enemyBgSuppressed -> if (AppSettings.showUnitTowerBackground.value) GamePlayColors.ErrorDark else Color.Transparent // Darker red border for enemies (only when background enabled)
            defender != null ->
                if (AppSettings.showUnitTowerBackground.value) {
                    if (defender.isReady) GamePlayColors.InfoDark else GamePlayColors.Building
                } else {
                    Color.Transparent
                } // Darker blue/gray border for towers (only when background enabled)
            effectiveFieldEffect != null -> {
                when (effectiveFieldEffect.type) {
                    FieldEffectType.FIREBALL -> GamePlayColors.WarningDeep // Deep orange border for fireball
                    FieldEffectType.ACID -> GamePlayColors.Success // Green border for acid
                    FieldEffectType.WEB -> Color(0xFF5E35B1) // Deep violet border for spider web
                    FieldEffectType.BURNING_TILE -> Color(0xFFCC2200) // Deep red border for burning tile
                    FieldEffectType.SHADOW_FOG -> Color(0xFF4A148C) // Deep purple border for shadow fog
                }
            }

            trap != null -> GamePlayColors.Trap // Brown border for trap
            barricade != null -> Color(0xFF795548) // Brown border for barricade
            else -> Color.Transparent // No borders for empty cells
        }

    // Thicker borders for important elements
    val borderWidth =
        when {
            isKeyboardPlacementCursor -> 6.dp // Prominent border for the keyboard placement/targeting cursor
            showPlacementPreview -> 6.dp // Double thickness for hovered build tile
            isInPreviewRange -> 3.dp // Medium border for range preview
            cellIsInBarricadeRange ||
                cellIsValidForMineTrapPlacement ||
                cellIsValidForMagicalTrapPlacement ||
                cellIsSupportBarricadePlacement ||
                cellIsSupportDwarvenTrapPlacement ||
                cellIsSupportMagicalTrapPlacement -> 3.dp // Medium border for trap/barricade placement range
            isBuildableAndEmpty || canBeUsedAsTowerBase -> 3.dp // Medium border for buildable tiles and tower bases
            isDefenderSelected && gameState.phase.value != GamePhase.INITIAL_BUILDING -> 5.dp // Extra thick border for selected defender (not during initial building)
            cellIsInDoubleReachOnlyRange && isValidTargetTile && showRange && canPlaceTrapHere -> 2.dp // Thin purple border for double-reach-only tiles
            cellIsInRange && isValidTargetTile && showRange && canPlaceTrapHere -> 4.dp // Thick border for cells in range (path or river for area attacks)
            isValidSpellTarget &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL &&
                spellTargeting?.activeSpell != SpellType.FEAR_SPELL_AREA -> 4.dp // Thick purple border for valid spell targets
            isDangerous -> 4.dp
            isSpawnPoint || isTarget -> 3.dp
            (attacker != null || defender != null) && AppSettings.showUnitTowerBackground.value -> 3.dp
            effectiveFieldEffect != null -> 3.dp // Thick border for field effects
            trap != null -> 3.dp // Thick border for trap
            barricade != null -> 3.dp // Thick border for barricade
            else -> 0.dp // No border for empty cells
        }

    // Flag to indicate dashed border (for preview and buildable tiles)
    val useDashedBorder =
        showPlacementPreview ||
            isInPreviewRange ||
            isBuildableAndEmpty ||
            canBeUsedAsTowerBase ||
            cellIsInBarricadeRange ||
            cellIsValidForMineTrapPlacement ||
            cellIsValidForMagicalTrapPlacement ||
            cellIsSupportBarricadePlacement ||
            cellIsSupportDwarvenTrapPlacement ||
            cellIsSupportMagicalTrapPlacement

    val showDiagonalStripes =
        isBuildableAndEmpty ||
            canBeUsedAsTowerBase ||
            cellIsInBarricadeRange ||
            cellIsValidForMineTrapPlacement ||
            cellIsValidForMagicalTrapPlacement ||
            cellIsSupportBarricadePlacement ||
            cellIsSupportDwarvenTrapPlacement ||
            cellIsSupportMagicalTrapPlacement

    // Determine if we should use gradient blending
    val useTileImages = de.egril.defender.ui.settings.AppSettings.useTileImages.value
    val useTileSmoothTransitions = de.egril.defender.ui.settings.AppSettings.useTileSmoothTransitions.value
    val shouldUseGradientBlending = useTileImages && useTileSmoothTransitions && tilePainter != null

    // Helper function to get tile type for a position
    val getNeighborTileType: (Position) -> de.egril.defender.editor.TileType? = { pos ->
        if (pos.x < 0 ||
            pos.x >= gameState.level.gridWidth ||
            pos.y < 0 ||
            pos.y >= gameState.level.gridHeight
        ) {
            null
        } else {
            when {
                gameState.level.isSpawnPoint(pos) -> de.egril.defender.editor.TileType.SPAWN_POINT
                gameState.level.isTargetPosition(pos) -> de.egril.defender.editor.TileType.TARGET
                gameState.level.isRiverTile(pos) -> de.egril.defender.editor.TileType.RIVER
                gameState.level.isOnPath(pos) -> de.egril.defender.editor.TileType.PATH
                gameState.level.isBuildArea(pos) -> de.egril.defender.editor.TileType.BUILD_AREA
                else -> de.egril.defender.editor.TileType.NO_PLAY
            }
        }
    }

    // Pre-compute neighbor tile types for gradient blending
    val neighborTileTypes =
        remember(position, gameState.defenders.size, gameState.level) {
            if (!shouldUseGradientBlending) {
                emptyMap()
            } else {
                val neighbors = position.getHexNeighbors()
                neighbors
                    .mapNotNull { neighborPos ->
                        val neighborType = getNeighborTileType(neighborPos)
                        if (neighborType != null) {
                            neighborPos to neighborType
                        } else {
                            null
                        }
                    }.toMap()
            }
        }

    // Get the actual painters for neighbors (must be done in @Composable context)
    val neighborPainters =
        neighborTileTypes.mapValues { (pos, type) ->
            // Check if there's a ready defender on this tile (build area)
            val neighborDefender = gameState.defenders.find { it.position.value == pos }
            val neighborIsReady = neighborDefender?.isReady == true
            val neighborIsBuildArea = gameState.level.isBuildArea(pos)
            val shouldShowNeighborTile = !(neighborDefender != null && neighborIsReady && neighborIsBuildArea)

            if (shouldShowNeighborTile) {
                val neighborRiverTile = gameState.level.getRiverTile(pos)
                val neighborIsMaelstrom = neighborRiverTile?.flowDirection == RiverFlow.MAELSTROM
                TileImageProvider.getTilePainter(type, isMaelstrom = neighborIsMaelstrom)
            } else {
                null
            }
        }

    if (shouldUseGradientBlending) {
        GradientBlendedTileCell(
            hexSize = hexSize,
            position = position,
            tileType = tileType,
            backgroundColor = finalBackgroundColor,
            borderColor = if (useDashedBorder) Color.Transparent else borderColor,
            borderWidth = if (useDashedBorder) 0.dp else borderWidth,
            backgroundPainter = tilePainter,
            onClick = onClick,
            onHover = onHoverChange,
            getNeighborTileType = getNeighborTileType,
            getNeighborTilePainter = { pos, _ -> neighborPainters[pos] },
        ) {
            GridCellContent(
                position = position,
                gameState = gameState,
                attacker = attacker,
                healingEffect = healingEffect,
                damageEffect = damageEffect,
                defender = defender,
                riverTile = riverTile,
                fieldEffect = effectiveFieldEffect,
                trap = trap,
                barricade = barricade,
                fief = fief,
                mushroom = mushroom,
                isSpawnPoint = isSpawnPoint,
                isTarget = isTarget,
                isRiverTile = isRiverTile,
                showPlacementPreview = showPlacementPreview,
                showBarricadePreview = showBarricadePreview,
                supportObjectPreviewType = supportObjectPreviewType,
                previewDefenderType = previewDefenderType,
                targetCircleInfo = targetCircleInfo,
                useDashedBorder = useDashedBorder,
                borderColor = borderColor,
                borderWidth = borderWidth,
                hexSize = hexSize,
                showTrapPreview = showTrapPreview,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                isBuildableAndEmpty = isBuildableAndEmpty,
                canBeUsedAsTowerBase = canBeUsedAsTowerBase,
                showDiagonalStripes = showDiagonalStripes,
                isInCoolingArea = isInCoolingArea,
                coolingAreaTurnsRemaining = coolingAreaTurnsRemaining,
                isCoolingSpellPreview = isCoolingSpellPreview,
                bombEffect = bombEffect,
                bombExplosion = bombExplosion,
                deathEffect = deathEffect,
                coinGainEffect = coinGainEffect,
                towerAttackEffect = towerAttackEffect,
                constructionCompleteEffect = constructionCompleteEffect,
                enemySpawnEffect = enemySpawnEffect,
                trapTriggerEffect = trapTriggerEffect,
                enemyMoveEffect = enemyMoveEffect,
                dragonLevelChangeEffect = dragonLevelChangeEffect,
                mineDigEffect = mineDigEffect,
                arrowAttackEffect = arrowAttackEffect,
                isArrowTargetTile = isArrowTargetTile,
                isBallistaTargetTile = isBallistaTargetTile,
                isBowTargetTile = isBowTargetTile,
                isSpearTargetTile = isSpearTargetTile,
                isPikeTargetTile = isPikeTargetTile,
                isWizardTargetTile = isWizardTargetTile,
                isAlchemyTargetTile = isAlchemyTargetTile,
                isInWizardAttackArea = isInWizardAttackArea,
                isInAlchemyAttackArea = isInAlchemyAttackArea,
                dragonIsTargetingMine = dragonIsTargetingMine,
                suppressEnemyBackground = suppressEnemyBackground,
                attackPreview = attackPreview,
                isDangerous = isDangerous,
            )
        }
    } else {
        BaseGridCell(
            hexSize = hexSize,
            backgroundColor = finalBackgroundColor,
            borderColor = if (useDashedBorder) Color.Transparent else borderColor,
            borderWidth = if (useDashedBorder) 0.dp else borderWidth,
            backgroundPainter = tilePainter,
            onClick = onClick,
            onHover = onHoverChange,
        ) {
            GridCellContent(
                position = position,
                gameState = gameState,
                attacker = attacker,
                healingEffect = healingEffect,
                damageEffect = damageEffect,
                defender = defender,
                riverTile = riverTile,
                fieldEffect = effectiveFieldEffect,
                trap = trap,
                barricade = barricade,
                fief = fief,
                mushroom = mushroom,
                isSpawnPoint = isSpawnPoint,
                isTarget = isTarget,
                isRiverTile = isRiverTile,
                showPlacementPreview = showPlacementPreview,
                showBarricadePreview = showBarricadePreview,
                supportObjectPreviewType = supportObjectPreviewType,
                previewDefenderType = previewDefenderType,
                targetCircleInfo = targetCircleInfo,
                useDashedBorder = useDashedBorder,
                borderColor = borderColor,
                borderWidth = borderWidth,
                hexSize = hexSize,
                showTrapPreview = showTrapPreview,
                selectedMineAction = selectedMineAction,
                selectedWizardAction = selectedWizardAction,
                isBuildableAndEmpty = isBuildableAndEmpty,
                canBeUsedAsTowerBase = canBeUsedAsTowerBase,
                showDiagonalStripes = showDiagonalStripes,
                isInCoolingArea = isInCoolingArea,
                coolingAreaTurnsRemaining = coolingAreaTurnsRemaining,
                isCoolingSpellPreview = isCoolingSpellPreview,
                bombEffect = bombEffect,
                bombExplosion = bombExplosion,
                deathEffect = deathEffect,
                coinGainEffect = coinGainEffect,
                towerAttackEffect = towerAttackEffect,
                constructionCompleteEffect = constructionCompleteEffect,
                enemySpawnEffect = enemySpawnEffect,
                trapTriggerEffect = trapTriggerEffect,
                enemyMoveEffect = enemyMoveEffect,
                dragonLevelChangeEffect = dragonLevelChangeEffect,
                mineDigEffect = mineDigEffect,
                arrowAttackEffect = arrowAttackEffect,
                isArrowTargetTile = isArrowTargetTile,
                isBallistaTargetTile = isBallistaTargetTile,
                isBowTargetTile = isBowTargetTile,
                isSpearTargetTile = isSpearTargetTile,
                isPikeTargetTile = isPikeTargetTile,
                isWizardTargetTile = isWizardTargetTile,
                isAlchemyTargetTile = isAlchemyTargetTile,
                isInWizardAttackArea = isInWizardAttackArea,
                isInAlchemyAttackArea = isInAlchemyAttackArea,
                dragonIsTargetingMine = dragonIsTargetingMine,
                suppressEnemyBackground = suppressEnemyBackground,
                attackPreview = attackPreview,
                isDangerous = isDangerous,
            )
        }
    }
}

/**
 * Content displayed inside a grid cell (separated for reuse between BaseGridCell and GradientBlendedTileCell)
 */
@Composable
private fun BoxScope.GridCellContent(
    position: Position,
    gameState: GameState,
    attacker: Attacker?,
    isDangerous: Boolean = false,
    healingEffect: HealingEffect?,
    damageEffect: DamageEffect?,
    defender: Defender?,
    riverTile: RiverTile?,
    fieldEffect: FieldEffect?,
    trap: Trap?,
    barricade: Barricade?,
    fief: de.egril.defender.model.Fief? = null,
    mushroom: de.egril.defender.model.Mushroom? = null,
    isSpawnPoint: Boolean,
    isTarget: Boolean,
    isRiverTile: Boolean,
    showPlacementPreview: Boolean,
    showBarricadePreview: Boolean,
    // Non-null only for the single hovered tile that is a valid placement target for the selected
    // support object; renders the same ghost preview used for tower-placed barricades/traps.
    supportObjectPreviewType: SupportObjectType? = null,
    // previewDefenderType replaces selectedDefenderType: non-null only for the 1 cell showing
    // the placement preview icon, so buy-button clicks don't cascade to all GridCellContent instances.
    previewDefenderType: DefenderType?,
    targetCircleInfo: TargetCircleInfo?,
    useDashedBorder: Boolean,
    borderColor: Color,
    borderWidth: Dp,
    hexSize: Dp,
    showTrapPreview: Boolean = false,
    selectedMineAction: MineAction? = null,
    selectedWizardAction: WizardAction? = null,
    isBuildableAndEmpty: Boolean = false,
    canBeUsedAsTowerBase: Boolean = false,
    showDiagonalStripes: Boolean = false,
    isInCoolingArea: Boolean = false,
    coolingAreaTurnsRemaining: Int? = null,
    isCoolingSpellPreview: Boolean = false,
    bombEffect: ActiveSpellEffect? = null,
    bombExplosion: BombExplosionEffect? = null,
    deathEffect: EnemyDeathEffect? = null,
    coinGainEffect: CoinGainEffect? = null,
    towerAttackEffect: TowerAttackEffect? = null,
    constructionCompleteEffect: TowerConstructionEffect? = null,
    enemySpawnEffect: EnemySpawnEffect? = null,
    trapTriggerEffect: TrapTriggerEffect? = null,
    enemyMoveEffect: EnemyMoveEffect? = null,
    dragonLevelChangeEffect: DragonLevelChangeEffect? = null,
    mineDigEffect: MineDigEffect? = null,
    arrowAttackEffect: ArrowAttackEffect? = null,
    isArrowTargetTile: Boolean = false,
    isBallistaTargetTile: Boolean = false,
    isBowTargetTile: Boolean = false,
    isSpearTargetTile: Boolean = false,
    isPikeTargetTile: Boolean = false,
    isWizardTargetTile: Boolean = false,
    isAlchemyTargetTile: Boolean = false,
    isInWizardAttackArea: Boolean = false,
    isInAlchemyAttackArea: Boolean = false,
    dragonIsTargetingMine: Boolean = false,
    suppressEnemyBackground: Boolean = false,
    // Precomputed attack damage/lethality/immunity preview for the enemy on this tile (issue #591).
    // Non-null only when a defender is selected that could attack this enemy.
    attackPreview: EnemyAttackPreview? = null,
) {
    // When animations are enabled, delay updating the enemy's displayed health value until
    // the attack animation (projectile flight + impact flash) has completed.
    // This way the health number on the icon only changes after the impact flash, matching
    // the visual sequence of: projectile arrives → flash → damage visible.
    // When animations are off, or when the health changes without a tower attack (e.g. acid
    // DOT ticks during the enemy turn), the displayed value updates immediately.
    // For AoE attacks (wizard fireball, alchemy acid) we must also delay tiles that are in
    // the blast area but not the exact target position — those tiles have towerAttackEffect == null
    // but their enemy HP still changes as part of the AoE damage.
    val isDeathEffectActive = deathEffect != null && attacker == null
    // suppressEnemyBackground already covers all cases (targeted, AoE, and non-targeted tiles).
    val enemyBgSuppressed = suppressEnemyBackground
    val animationsEnabled = AppSettings.enableAnimations.value
    var displayedHealth by remember { mutableStateOf(attacker?.currentHealth?.value ?: 0) }
    if (attacker != null) {
        LaunchedEffect(
            attacker.id,
            attacker.currentHealth.value,
            towerAttackEffect?.turnNumber,
            isInWizardAttackArea,
            isInAlchemyAttackArea,
            animationsEnabled,
        ) {
            val currentHealth = attacker.currentHealth.value
            val flightDelay: Long =
                when {
                    !animationsEnabled -> 0L
                    // Direct-target tiles: use the specific flight delay for that tower type
                    towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                    towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    // AoE surrounding tiles: towerAttackEffect is null for them, but still delay
                    isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    else -> 0L
                }
            if (flightDelay > 0L) {
                kotlinx.coroutines.delay(flightDelay + GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS)
            }
            displayedHealth = currentHealth
        }
    }

    when {
        attacker != null -> {
            // Use graphical icon for enemy units
            // Key by id, position, level, and movementPenalty to force recomposition when any changes.
            // currentHealth is intentionally omitted from the key so that the delayed displayedHealth
            // state (see above) is not discarded when health changes during an attack animation.
            key(
                attacker.id,
                attacker.position.value.x,
                attacker.position.value.y,
                attacker.level,
                attacker.movementPenalty.value,
            ) {
                // Detect freeze effect before Box so it can be used in modifier for outline
                val freezeEffect =
                    gameState.activeSpellEffects.find {
                        it.spell == SpellType.FREEZE_SPELL && it.attackerId == attacker.id
                    }
                // Detect if cooling spell reduces this enemy's movement to 0
                val coolingReducesToZero =
                    isInCoolingArea &&
                        run {
                            val barbsSpeed = maxOf(1, attacker.type.speed - attacker.movementPenalty.value)
                            maxOf(0, barbsSpeed - 1) == 0
                        }
                // Compute the actual tile background color so the icon can derive the correct outline color.
                // When unit backgrounds are OFF (transparent), pass Color.White so the icon always uses
                // dark outlines – matching the light-mode appearance regardless of the current theme.
                // When a freeze/cooling effect is active, that color takes priority.
                val attackerTileBackground =
                    when {
                        freezeEffect != null || coolingReducesToZero -> TargetCircleConstants.COOLING_SPELL_COLOR.copy(alpha = 0.5f)
                        !AppSettings.showUnitTowerBackground.value -> Color.White
                        else -> null
                    }
                // Derive health/level text color from effective background:
                // - Freeze/cooling (turquoise) or unit backgrounds ON (red) → dark enough for white text
                // - Unit backgrounds OFF in dark mode → dark terrain bg → white text
                // - Unit backgrounds OFF in light mode → light terrain bg → dark text
                val healthTextColor =
                    when {
                        freezeEffect != null || coolingReducesToZero -> Color.White
                        AppSettings.showUnitTowerBackground.value -> Color.White
                        AppSettings.isDarkMode.value -> Color.White
                        else -> Color.Black
                    }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        if (freezeEffect != null || coolingReducesToZero) {
                            Modifier.border(2.dp, TargetCircleConstants.COOLING_SPELL_COLOR, RoundedCornerShape(4.dp))
                        } else {
                            Modifier
                        },
                ) {
                    EnemyIcon(
                        attacker = attacker,
                        backgroundColor = attackerTileBackground,
                        healthTextColor = healthTextColor,
                        healthOverride = displayedHealth,
                        showWaaghGlow = gameState.waaghFrenzyActive.value && attacker.type in setOf(AttackerType.GOBLIN, AttackerType.ORK, AttackerType.OGRE, AttackerType.SNOTLING),
                    )
                    if (isDangerous) {
                        Text(
                            text = "!",
                            color = GamePlayColors.Error,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp),
                        )
                    }
                    // Show healing effect overlay if present
                    val shouldPlayHealingAnimation =
                        rememberShouldPlayOneShotTileAnimation(
                            gameState,
                            healingEffect?.let {
                                oneShotTileAnimationKey("green_witch_healing", it.position, it.turnNumber)
                            },
                        )
                    if (healingEffect != null && shouldPlayHealingAnimation) {
                        GreenWitchHealingAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show freeze effect overlay
                    if (freezeEffect != null) {
                        FreezeSpellAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                            animationKey = freezeEffect.attackerId,
                        )
                    }
                    // Show fear effect overlay (black scribble cloud at top of icon)
                    val fearEffect =
                        gameState.activeSpellEffects.find { effect ->
                            (effect.spell == SpellType.FEAR_SPELL && effect.attackerId == attacker.id) ||
                                (
                                    effect.spell == SpellType.FEAR_SPELL_AREA &&
                                        effect.position != null &&
                                        attacker.position.value.hexDistanceTo(effect.position) <= 2
                                )
                        }
                    if (fearEffect != null) {
                        FearSpellAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show barb effect indicators if affected (show up to 5 arrows in center)
                    if (attacker.movementPenalty.value > 0) {
                        val barbCount = minOf(attacker.movementPenalty.value, 5)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                repeat(barbCount) {
                                    Box(
                                        modifier =
                                            Modifier.graphicsLayer {
                                                rotationZ = 10f // Tilt +10 degrees
                                            },
                                    ) {
                                        de.egril.defender.ui.icon.DownArrowIcon(
                                            size = 12.dp,
                                            tint = Color.Red,
                                        )
                                    }
                                }
                            }
                        }
                        // Show mushroom buff overlay when the enemy is under mushroom buff
                        if (attacker.mushroomTurnsRemaining.value > 0) {
                            MushroomBuffAnimation(
                                animate = AppSettings.enableAnimations.value,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                    // Attack damage / lethality / immunity preview at the left border
                    if (attackPreview != null) {
                        EnemyAttackPreviewIcon(
                            damage = attackPreview.damage,
                            isLethal = attackPreview.isLethal,
                            isImmune = attackPreview.isImmune,
                            modifier =
                                Modifier
                                    .align(Alignment.CenterStart)
                                    .offset(x = 10.dp),
                        )
                    }
                }
            }
        }

        defender != null -> {
            // Use graphical icon for towers
            // Key by id, position, level and actionsRemaining to force recomposition when these change
            val doubleLevelActive =
                gameState.activeSpellEffects.any {
                    it.spell == SpellType.DOUBLE_TOWER_LEVEL && it.defenderId == defender.id
                }
            key(
                defender.id,
                defender.position.value.x,
                defender.position.value.y,
                defender.level.value,
                defender.actionsRemaining.value,
                defender.buildTimeRemaining.value,
                defender.isDisabled.value,
                defender.disabledTurnsRemaining.value,
                doubleLevelActive,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    TowerIcon(defender = defender, gameState = gameState)
                    // Show pulsing blue glow when tower is ready to act
                    if (defender.isReady && defender.actionsRemaining.value > 0) {
                        TowerReadyPulseAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show construction complete sparkle when tower just finished building
                    val shouldPlayConstructionCompleteAnimation =
                        rememberShouldPlayOneShotTileAnimation(
                            gameState,
                            constructionCompleteEffect?.let {
                                oneShotTileAnimationKey("tower_construction_complete", it.position, it.turnNumber)
                            },
                        )
                    if (constructionCompleteEffect != null && shouldPlayConstructionCompleteAnimation) {
                        TowerConstructionCompleteAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show idle ambient animation for wizard and alchemy towers (when built)
                    if (defender.buildTimeRemaining.value == 0) {
                        when (defender.type) {
                            // Wizard idle glows only while the tower still has actions this turn
                            DefenderType.WIZARD_TOWER ->
                                if (defender.actionsRemaining.value > 0) {
                                    WizardIdleAnimation(
                                        animate = AppSettings.enableAnimations.value,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            DefenderType.ALCHEMY_TOWER ->
                                AlchemyIdleAnimation(
                                    animate = AppSettings.enableAnimations.value,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            DefenderType.DWARVEN_MINE -> {
                                // Show dig animation on mine tile when it was just dug
                                val shouldPlayMineDigAnimation =
                                    rememberShouldPlayOneShotTileAnimation(
                                        gameState,
                                        mineDigEffect?.let {
                                            oneShotTileAnimationKey("mine_dig", it.position, it.turnNumber)
                                        },
                                    )
                                if (mineDigEffect != null && shouldPlayMineDigAnimation) {
                                    MineDigAnimation(
                                        animate = AppSettings.enableAnimations.value,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }
                            else -> Unit
                        }
                    }
                    // Show Double Tower Level spell animation overlay (same animation as instant tower)
                    if (doubleLevelActive) {
                        InstantTowerSpellAnimation(
                            animate = AppSettings.enableAnimations.value,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    // Show red "XT" overlay if tower is disabled by Red Witch
                    if (defender.isDisabled.value && defender.disabledTurnsRemaining.value > 0) {
                        Text(
                            "${defender.disabledTurnsRemaining.value}T",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        fieldEffect != null -> {
            // Show field effect info
            when (fieldEffect.type) {
                FieldEffectType.FIREBALL -> {
                    // Show fireball symbol
                    ExplosionIcon(size = 28.dp)
                }

                FieldEffectType.ACID -> {
                    // Show acid splash with damage and duration
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TestTubeIcon(size = 20.dp)
                        Text(
                            "-${fieldEffect.damage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Yellow,
                        )
                    }
                }

                FieldEffectType.WEB -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        WebIcon(size = 20.dp)
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Yellow,
                        )
                    }
                }

                FieldEffectType.BURNING_TILE -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ExplosionIcon(size = 22.dp)
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = GamePlayColors.Yellow,
                        )
                    }
                }
                FieldEffectType.SHADOW_FOG -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(GamePlayConstants.TileIconSizes.ShadowFogOverlay)
                                    .background(Color(0xB020002A), RoundedCornerShape(50)),
                        )
                        Text(
                            "${fieldEffect.turnsRemaining}T",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE1BEE7),
                        )
                    }
                }
            }
        }

        fief != null -> {
            // Show fief image, type name, and coin income
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val fiefName =
                    when (fief.type) {
                        de.egril.defender.model.FiefType.FISHER ->
                            stringResource(Res.string.fief_type_fisher)
                        de.egril.defender.model.FiefType.WOODCUTTER ->
                            stringResource(Res.string.fief_type_woodcutter)
                        de.egril.defender.model.FiefType.QUARRY ->
                            stringResource(Res.string.fief_type_quarry)
                        de.egril.defender.model.FiefType.MARKETPLACE ->
                            stringResource(Res.string.fief_type_marketplace)
                    }

                when (fief.type) {
                    de.egril.defender.model.FiefType.FISHER -> {
                        val rodRotation = getFisherRodRotationDegrees(position, gameState.level)
                        Box(
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(Res.drawable.fief_fisher_hut),
                                contentDescription = fiefName,
                                modifier = Modifier.fillMaxSize(),
                            )
                            Image(
                                painter = painterResource(Res.drawable.fief_fishing_rod),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            rotationZ = rodRotation,
                                            transformOrigin = TransformOrigin(0.38f, 0.56f),
                                        ),
                            )
                        }
                    }
                    de.egril.defender.model.FiefType.WOODCUTTER -> {
                        Image(
                            painter = painterResource(Res.drawable.fief_woodcutter),
                            contentDescription = fiefName,
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                        )
                    }
                    de.egril.defender.model.FiefType.QUARRY -> {
                        Image(
                            painter = painterResource(Res.drawable.fief_quarry),
                            contentDescription = fiefName,
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                        )
                    }
                    de.egril.defender.model.FiefType.MARKETPLACE -> {
                        Image(
                            painter = painterResource(Res.drawable.fief_marketplace),
                            contentDescription = fiefName,
                            modifier = Modifier.size(GamePlayConstants.TileIconSizes.Fief),
                        )
                    }
                }
                Text(
                    fiefName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-3).dp),
                )
                Text(
                    "+${fief.type.incomePerTurn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GamePlayColors.Yellow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-3).dp),
                )
            }
        }

        mushroom != null -> {
            // Show mushroom icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MushroomIcon(size = GamePlayConstants.TileIconSizes.Mushroom)
            }
        }

        trap != null -> {
            // Show trap icon based on trap type
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (trap.type) {
                    TrapType.MAGICAL -> {
                        // Magical trap - show pentagram (no damage display)
                        PentagramIcon(size = GamePlayConstants.TileIconSizes.Trap)
                    }

                    TrapType.DWARVEN -> {
                        // Dwarven trap - show trap icon with damage
                        TrapIcon(size = GamePlayConstants.TileIconSizes.Trap)
                        Text(
                            "-${trap.damage}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-6).dp),
                        )
                    }
                }
            }
        }

        barricade != null -> {
            // Show barricade with HP
            val barricadeLocale = com.hyperether.resources.currentLanguage.value
            Box(contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // Show wood/barricade symbol or gate icon with brown color.
                    // When the barricade also serves as a tower base (HP >= 100), overlay the
                    // wooden tower-base platform so it is distinguishable from a plain barricade.
                    Box(contentAlignment = Alignment.Center) {
                        if (barricade.isGate) {
                            GateIcon(
                                modifier = Modifier.offset(y = 10.dp),
                                size = GamePlayConstants.TileIconSizes.Barricade,
                            )
                        } else {
                            WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                        }
                        if (barricade.canSupportTower()) {
                            TowerBasePlatformIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                        }
                    }

                    // Show gate/barricade name (2 lines) then HP (1 line, bold)
                    val barricadeDisplayName =
                        barricade.name
                            ?.takeIf { it.isNotBlank() }
                            ?.let { localizeEntityName(it, barricadeLocale) }
                    if (!barricadeDisplayName.isNullOrBlank()) {
                        Text(
                            text =
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Color.White)) {
                                        appendLine(barricadeDisplayName)
                                    }
                                    withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                                        appendLine("${barricade.healthPoints.value} HP")
                                    }
                                },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            minLines = 3,
                            maxLines = 3,
                            overflow = TextOverflow.Visible,
                            modifier =
                                Modifier
                                    .widthIn(max = 50.dp)
                                    .offset(y = (-32).dp),
                        )
                    } else {
                        Text(
                            "${barricade.healthPoints.value} HP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(y = (-12).dp),
                        )
                    }
                }
                // Show damage effect overlay if present
                if (damageEffect != null) {
                    BarricadeDamageAnimation(
                        animate = AppSettings.enableAnimations.value,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        showBarricadePreview || supportObjectPreviewType == SupportObjectType.BARRICADE -> {
            // Show see-through barricade preview when hovering
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.graphicsLayer(alpha = 0.5f), // Semi-transparent
            ) {
                // Show wood/barricade symbol with brown color
                WoodIcon(size = GamePlayConstants.TileIconSizes.Barricade)
                // Show "NEW" text for new barricade preview
                Text(
                    stringResource(Res.string.barricade),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF795548), // Brown color
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        bombEffect != null -> {
            // Show bomb icon with countdown number overlaid prominently
            Box(contentAlignment = Alignment.Center) {
                BombIcon(size = 36.dp)
                // Countdown badge in bottom-right corner of icon
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .background(
                                color = Color(0xFFCC0000),
                                shape = androidx.compose.foundation.shape.CircleShape,
                            ).padding(horizontal = 4.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${bombEffect.turnsRemaining}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = MaterialTheme.typography.labelMedium.fontSize,
                    )
                }
            }
        }

        isSpawnPoint -> {
            // Show spawn indicator when cell is empty
            Text(
                stringResource(Res.string.spawn),
                style = MaterialTheme.typography.labelSmall,
                color = GamePlayColors.Warning,
            )
        }

        isTarget -> {
            // Show target name (if set) or fallback to generic "Target" label
            // Well-known names are translated; \n in the string gives multi-line tile display
            // Taken targets (SINGLE_HIT) show with a red cross overlay
            val locale = com.hyperether.resources.currentLanguage.value
            val isTaken = gameState.takenTargets.contains(position)
            val rawName =
                gameState.level.targetInfoMap[position]
                    ?.name
                    ?.takeIf { it.isNotBlank() }
            val targetName =
                if (rawName != null) {
                    localizeEntityName(rawName, locale)
                } else {
                    stringResource(Res.string.target)
                }
            if (isTaken) {
                // Show dimmed name with a red X cross on top
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = targetName,
                        style = MaterialTheme.typography.labelSmall,
                        color = GamePlayColors.Success.copy(alpha = 0.3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 50.dp),
                    )
                    CrossIcon(size = 20.dp, tint = Color.Red)
                }
            } else {
                Text(
                    text = targetName,
                    style = MaterialTheme.typography.labelSmall,
                    color = GamePlayColors.Success,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 50.dp),
                )
            }
        }

        isRiverTile -> {
            // Check if there's a bridge at this position
            val bridge = gameState.getBridgeAt(position)
            if (bridge != null) {
                // Show bridge over river
                BridgeVisualization(bridge = bridge)
            } else {
                // Show river flow direction arrows
                if (riverTile != null) {
                    // Don't show trap icon on maelstrom when tile images are enabled
                    // (the tile_river_maelstrom.png image already shows the maelstrom visually)
                    val useTileImages = AppSettings.useTileImages.value
                    val isMaelstromWithTileImage = riverTile.flowDirection == RiverFlow.MAELSTROM && useTileImages
                    // Don't show dot symbol on NONE (still water) tiles when the level map image is enabled
                    val useLevelMapImage = AppSettings.useLevelMapImage.value
                    val isNoneWithMapImage = riverTile.flowDirection == RiverFlow.NONE && useLevelMapImage

                    // Show water flow animation when animations are enabled (not for NONE/MAELSTROM)
                    val enableAnimations = AppSettings.enableAnimations.value
                    val showWaterAnimation =
                        enableAnimations &&
                            riverTile.flowDirection != RiverFlow.NONE &&
                            riverTile.flowDirection != RiverFlow.MAELSTROM
                    if (showWaterAnimation) {
                        WaterFlowAnimation(
                            flowDirection = riverTile.flowDirection,
                            flowSpeed = riverTile.flowSpeed,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    if (!isMaelstromWithTileImage && !isNoneWithMapImage) {
                        RiverFlowIndicator(
                            flowDirection = riverTile.flowDirection,
                            flowSpeed = riverTile.flowSpeed,
                            size = 28.dp,
                        )
                    }
                }
            }
        }
    }

    // Show cooling spell snowflake animation on affected tiles
    if (isInCoolingArea) {
        CoolingAreaAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize(),
        )
    }

    // Show turns count for cooling spell (placement preview: 3, active effect: actual remaining)
    val coolingTurns = coolingAreaTurnsRemaining ?: if (isCoolingSpellPreview) 3 else null
    if (coolingTurns != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                "${coolingTurns}T",
                style = MaterialTheme.typography.labelSmall,
                color = TargetCircleConstants.COOLING_SPELL_COLOR,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }

    // Show half-transparent tower icon on hovered build tile.
    // previewDefenderType is non-null only for the 1 cell showing the placement preview
    // (pre-computed in GameGrid to avoid passing selectedDefenderType to all cells).
    if (previewDefenderType != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),
            // 50% transparency
            contentAlignment = Alignment.Center,
        ) {
            TowerTypeIcon(
                defenderType = previewDefenderType,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // Show half-transparent trap icon on hovered path tile (when in trap placement mode)
    if (showTrapPreview ||
        supportObjectPreviewType == SupportObjectType.DWARVEN_TRAP ||
        supportObjectPreviewType == SupportObjectType.MAGICAL_TRAP
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.5f),
            // 50% transparency
            contentAlignment = Alignment.Center,
        ) {
            // Show different icon based on trap type
            when {
                selectedMineAction == MineAction.BUILD_TRAP ||
                    supportObjectPreviewType == SupportObjectType.DWARVEN_TRAP -> {
                    // Dwarven trap - show trap icon
                    TrapIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)
                }
                selectedWizardAction == WizardAction.PLACE_MAGICAL_TRAP ||
                    supportObjectPreviewType == SupportObjectType.MAGICAL_TRAP -> {
                    // Magical trap - show pentagram icon
                    PentagramIcon(size = GamePlayConstants.TileIconSizes.TrapPreview)
                }
            }
        }
    }

    // Show bomb explosion animation overlay on affected tiles (highest priority, above everything)
    val shouldPlayBombExplosionAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            bombExplosion?.let {
                oneShotTileAnimationKey("bomb_explosion", position, it.turnNumber, "${it.center.x},${it.center.y}")
            },
        )
    if (bombExplosion != null && shouldPlayBombExplosionAnimation) {
        BombExplosionAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(20f),
        )
    }

    // Show enemy death animation overlay when an enemy was just defeated here.
    // The `attacker == null` guard avoids overlapping the death animation with a live enemy
    // that may have moved to this tile in the same turn.
    // When a tower attack was also recorded for this tile, delay the death animation until
    // after the impact animation plays (~670ms, plus ~900ms arrow flight for ranged attacks),
    // so the sequence is: pre-death icon with red bg → attack → impact → ghost → death anim.
    //
    // Ghost rendering: while the death effect is present (and before the death animation
    // finishes), show the enemy unit icon without a health bar so the player can see which
    // unit was killed.  The ghost disappears once the death animation has completed so that
    // the coin-gain animation plays on a clean tile.
    //
    val deathAnimationKey =
        deathEffect?.let {
            oneShotTileAnimationKey("enemy_death", it.position, it.turnNumber)
        }
    val towerImpactAnimationKey =
        towerAttackEffect?.let {
            oneShotTileAnimationKey("tower_attack_impact", it.targetPosition, it.turnNumber)
        }
    val shouldPlayDeathAnimation = rememberShouldPlayOneShotTileAnimation(gameState, deathAnimationKey)
    var showGhost by remember(deathAnimationKey, towerImpactAnimationKey, shouldPlayDeathAnimation) {
        mutableStateOf(isDeathEffectActive && shouldPlayDeathAnimation)
    }
    if (deathEffect != null) {
        LaunchedEffect(deathAnimationKey, towerImpactAnimationKey, shouldPlayDeathAnimation) {
            if (isDeathEffectActive && shouldPlayDeathAnimation) {
                showGhost = true
                val arrowDelay =
                    when {
                        towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                        towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        // AoE secondary targets: delay matches the fireball/acid flight time
                        isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        else -> 0L
                    }
                val impactDelay =
                    if (towerAttackEffect != null ||
                        isInWizardAttackArea ||
                        isInAlchemyAttackArea
                    ) {
                        GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS
                    } else {
                        0L
                    }
                kotlinx.coroutines.delay(arrowDelay + impactDelay + GamePlayConstants.AnimationTimings.ENEMY_DEATH_ANIMATION_DURATION_MS)
                showGhost = false
            } else {
                showGhost = false
            }
        }
    } else {
        SideEffect { showGhost = false }
    }
    if (showGhost && isDeathEffectActive) {
        EnemyTypeIcon(
            attackerType = deathEffect.attackerType,
            swarmHealthOverride = ghostSwarmCount(deathEffect.attackerType, displayedHealth),
            modifier = Modifier.fillMaxSize().zIndex(15f),
        )
        // Show level badge on top of the ghost icon when level > 1
        if (deathEffect.attackerLevel > 1) {
            Box(
                modifier = Modifier.fillMaxSize().zIndex(15f),
                contentAlignment = Alignment.TopCenter,
            ) {
                Text(
                    text = "${deathEffect.attackerLevel}",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    var showDeathAnimation by remember(deathAnimationKey, towerImpactAnimationKey) {
        mutableStateOf(false)
    }
    if (deathEffect != null) {
        LaunchedEffect(deathAnimationKey, towerImpactAnimationKey, shouldPlayDeathAnimation) {
            if (isDeathEffectActive && shouldPlayDeathAnimation) {
                // Wait for both the projectile flight and the impact flash to finish before
                // starting the death animation.  Also handles AoE secondary targets where
                // towerAttackEffect is null but the tile is in the fireball/acid blast area.
                val flightDelay =
                    when {
                        towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                        towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                        isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                        else -> 0L
                    }
                val impactDelay =
                    if (towerAttackEffect != null ||
                        isInWizardAttackArea ||
                        isInAlchemyAttackArea
                    ) {
                        GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS
                    } else {
                        0L
                    }
                if (flightDelay > 0L || impactDelay > 0L) {
                    kotlinx.coroutines.delay(flightDelay + impactDelay)
                }
                showDeathAnimation = true
                GlobalSoundManager.playSound(SoundEvent.ENEMY_DESTROYED)
            } else {
                showDeathAnimation = false
            }
        }
    } else {
        SideEffect { showDeathAnimation = false }
    }
    if (showDeathAnimation && isDeathEffectActive) {
        EnemyDeathAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(18f),
        )
    }

    // Show coin gain animation overlay after the full death-animation sequence has finished:
    // arrowDelay + impactDelay + deathDuration + post-death pause.
    val coinAnimationKey =
        coinGainEffect?.let {
            oneShotTileAnimationKey("coin_gain", it.position, it.turnNumber)
        }
    val shouldPlayCoinAnimation = rememberShouldPlayOneShotTileAnimation(gameState, coinAnimationKey)
    var showCoinAnimation by remember(coinAnimationKey, towerImpactAnimationKey) {
        mutableStateOf(false)
    }
    // Track where this tile's coin-gain "bubbling" animation ends, in root coordinates, so a
    // coin-flight animation can start from there. The rising coins finish near the top of the tile
    // (see COIN_BUBBLE_END_HEIGHT_FRACTION), horizontally centered.
    // Keyed on the (stable) tile grid position so it is captured once and not reset to null when
    // a new coin-gain effect appears on the same tile.
    var coinFlightStartPosition by remember(position) {
        mutableStateOf<Offset?>(null)
    }
    // Diameter (px) of the coin-gain "bubbling" coins on this tile, so the fly-to-counter coins can
    // be launched at the same visible size. The Lottie (coin_gain.json) is a 100x100 viewport with
    // 14-unit coins, fitted (ContentScale.Fit) to the tile box, so the coin diameter on screen is
    // COIN_BUBBLE_COIN_SIZE_FRACTION of the tile's smaller dimension.
    var flyingCoinSizePx by remember(position) {
        mutableStateOf(CoinFlightController.DEFAULT_COIN_SIZE_PX)
    }
    if (coinGainEffect != null) {
        Box(
            modifier =
                Modifier.matchParentSize().onGloballyPositioned { coords ->
                    // Captured fresh whenever a coin-gain effect is present on this tile (this Box
                    // only exists then) and re-fired while the map pans/zooms, so the value used at
                    // launch time reflects the tile's current on-screen position.
                    val topLeft = coords.positionInRoot()
                    coinFlightStartPosition =
                        Offset(
                            x = topLeft.x + coords.size.width / 2f,
                            y = topLeft.y + coords.size.height * COIN_BUBBLE_END_HEIGHT_FRACTION,
                        )
                    flyingCoinSizePx =
                        minOf(coords.size.width, coords.size.height) * COIN_BUBBLE_COIN_SIZE_FRACTION
                },
        )
    }
    if (coinGainEffect != null) {
        LaunchedEffect(coinAnimationKey, towerImpactAnimationKey, shouldPlayCoinAnimation) {
            if (!shouldPlayCoinAnimation) {
                showCoinAnimation = false
                return@LaunchedEffect
            }
            val arrowDelay =
                when {
                    towerAttackEffect != null && isArrowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBallistaTargetTile -> GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isBowTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isSpearTargetTile -> GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isPikeTargetTile -> GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS
                    towerAttackEffect != null && isWizardTargetTile -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    towerAttackEffect != null && isAlchemyTargetTile -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    // AoE secondary targets have no direct towerAttackEffect but are in the blast area
                    isInWizardAttackArea -> GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS
                    isInAlchemyAttackArea -> GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS
                    else -> 0L
                }
            val impactDelay =
                if (towerAttackEffect != null ||
                    isInWizardAttackArea ||
                    isInAlchemyAttackArea
                ) {
                    GamePlayConstants.AnimationTimings.ATTACK_IMPACT_DURATION_MS
                } else {
                    0L
                }
            kotlinx.coroutines.delay(
                arrowDelay + impactDelay +
                    GamePlayConstants.AnimationTimings.ENEMY_DEATH_ANIMATION_DURATION_MS +
                    GamePlayConstants.AnimationTimings.COIN_GAIN_DELAY_AFTER_DEATH_MS,
            )
            // Add coins to the player's total in sync with the animation so the counter
            // visually increases when the coin animation plays, not before the attack runs.
            showCoinAnimation = true
            // Launch the "coin fly-to-counter" animation only after the coin-gain (coins bubbling
            // up) animation has played, so the flying coins appear to peel off the end of that
            // animation. The counter is then updated as those coins reach it (via the launch
            // callback), so the number and the arriving coins stay in step.
            if (AppSettings.enableAnimations.value) {
                kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.COIN_GAIN_ANIMATION_DURATION_MS)
            }
            // Credit the coins that are still pending for this reward (guard against the safety
            // flush in completeEnemyTurn() having already credited them). Reserve them out of
            // pending immediately before launching so the flush can't also credit them; the flying
            // coins then add them to the visible total as they land.
            val toAdd = minOf(coinGainEffect.amount, gameState.pendingCoinGains.value)
            if (toAdd > 0) {
                gameState.pendingCoinGains.value -= toAdd
                val source = coinFlightStartPosition
                val launched =
                    if (AppSettings.enableAnimations.value && source != null) {
                        CoinFlightController.launch(
                            source = source,
                            amount = coinGainEffect.amount,
                            coinSizePx = flyingCoinSizePx,
                            creditAmount = toAdd,
                        ) { arrived ->
                            gameState.coins.value += arrived
                        }
                    } else {
                        0
                    }
                // No flying coins launched (animations off, no source position, or the queue is
                // full): credit the reserved coins immediately so the reward is never lost.
                if (launched == 0) {
                    gameState.coins.value += toAdd
                }
            }
        }
    } else {
        SideEffect { showCoinAnimation = false }
    }
    if (showCoinAnimation && coinGainEffect != null) {
        CoinGainAnimation(
            amount = coinGainEffect.amount,
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(19f),
        )
    }

    // Show tower attack impact overlay when this tile was attacked.
    // When a projectile is targeting this tile the hit animation is delayed
    // so the projectile visibly arrives before the impact flash.
    val shouldPlayTowerImpactAnimation =
        rememberShouldPlayOneShotTileAnimation(gameState, towerImpactAnimationKey)
    var showHitAnimation by remember(towerImpactAnimationKey) {
        mutableStateOf(
            shouldPlayTowerImpactAnimation &&
                towerAttackEffect != null &&
                !isArrowTargetTile &&
                !isBallistaTargetTile &&
                !isBowTargetTile &&
                !isSpearTargetTile &&
                !isPikeTargetTile &&
                !isWizardTargetTile &&
                !isAlchemyTargetTile,
        )
    }
    if (towerAttackEffect != null) {
        LaunchedEffect(
            towerImpactAnimationKey,
            isArrowTargetTile,
            isBallistaTargetTile,
            isBowTargetTile,
            isSpearTargetTile,
            isPikeTargetTile,
            isWizardTargetTile,
            isAlchemyTargetTile,
            shouldPlayTowerImpactAnimation,
        ) {
            if (!shouldPlayTowerImpactAnimation) {
                showHitAnimation = false
                return@LaunchedEffect
            }
            when {
                isArrowTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isBallistaTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.BALLISTA_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isBowTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isSpearTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ARROW_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isPikeTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.PIKE_EXTEND_DELAY_MS)
                    showHitAnimation = true
                }
                isWizardTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.WIZARD_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                isAlchemyTargetTile -> {
                    kotlinx.coroutines.delay(GamePlayConstants.AnimationTimings.ALCHEMY_FLIGHT_DELAY_MS)
                    showHitAnimation = true
                }
                else -> {
                    showHitAnimation = true
                }
            }
        }
    } else {
        SideEffect { showHitAnimation = false }
    }
    if (showHitAnimation && towerAttackEffect != null) {
        TowerAttackImpactAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(17f),
        )
    }

    // Show enemy spawn portal overlay when an enemy just appeared at this position
    val shouldPlayEnemySpawnAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            enemySpawnEffect?.let {
                oneShotTileAnimationKey("enemy_spawn", it.position, it.turnNumber)
            },
        )
    if (enemySpawnEffect != null && shouldPlayEnemySpawnAnimation && enemySpawnEffect.suppressPortalAnimation != true) {
        EnemySpawnAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(16f),
        )
    }

    // Show trap trigger overlay when a trap was triggered at this position.
    // Uses a high z-index (21) to be visible even when the death animation is also showing
    // (which happens when the trap kills the enemy in the same turn).
    val shouldPlayTrapTriggerAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            trapTriggerEffect?.let {
                oneShotTileAnimationKey("trap_trigger", it.position, it.turnNumber)
            },
        )
    if (trapTriggerEffect != null && shouldPlayTrapTriggerAnimation) {
        TrapTriggerAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(21f),
        )
    }

    // Show enemy movement trail when an enemy just left this tile during the enemy turn
    val shouldPlayEnemyMoveAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            enemyMoveEffect?.let {
                oneShotTileAnimationKey("enemy_move", it.position, it.turnNumber)
            },
        )
    if (enemyMoveEffect != null && attacker == null && shouldPlayEnemyMoveAnimation) {
        EnemyMoveAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(13f),
        )
    }

    // Show dragon level change flash on the dragon's tile when its level changed
    val shouldPlayDragonLevelChangeAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            dragonLevelChangeEffect?.let {
                oneShotTileAnimationKey("dragon_level_change", it.position, it.turnNumber, if (it.isLevelUp) "up" else "down")
            },
        )
    if (dragonLevelChangeEffect != null && shouldPlayDragonLevelChangeAnimation) {
        DragonLevelChangeAnimation(
            animate = AppSettings.enableAnimations.value,
            isLevelUp = dragonLevelChangeEffect.isLevelUp,
            modifier = Modifier.fillMaxSize().zIndex(14f),
        )
    }

    // Show arrow/bolt projectile on the source tower tile for ranged attacks
    val shouldPlayArrowAttackAnimation =
        rememberShouldPlayOneShotTileAnimation(
            gameState,
            arrowAttackEffect?.let {
                oneShotTileAnimationKey(
                    animationName = "arrow_attack",
                    position = it.sourcePosition,
                    turnNumber = it.turnNumber,
                    suffix = "${it.targetPosition.x},${it.targetPosition.y}",
                )
            },
        )
    if (arrowAttackEffect != null && shouldPlayArrowAttackAnimation) {
        val dx = (arrowAttackEffect.targetPosition.x - arrowAttackEffect.sourcePosition.x).toFloat()
        val dy = (arrowAttackEffect.targetPosition.y - arrowAttackEffect.sourcePosition.y).toFloat()
        val angle =
            if (dx == 0f && dy == 0f) {
                0f
            } else {
                (atan2(dy.toDouble(), dx.toDouble()) * GamePlayConstants.AnimationTimings.RADIANS_TO_DEGREES).toFloat()
            }
        ArrowAttackAnimation(
            animate = AppSettings.enableAnimations.value,
            directionAngle = angle,
            isTargetTile = isArrowTargetTile,
            modifier = Modifier.fillMaxSize().zIndex(18f),
        )
    }

    // Show per-tile Lottie arrow animation at the bow attack target tile when the volley arrives.
    // Uses showHitAnimation as the delay trigger (already delayed by ARROW_FLIGHT_DELAY_MS) so
    // the arrow lands visually at the same time the impact flash fires.
    if (showHitAnimation && isBowTargetTile) {
        val bowEffect = gameState.bowAttackEffects.find { it.targetPosition == position }
        if (bowEffect != null) {
            val dx = (bowEffect.targetPosition.x - bowEffect.sourcePosition.x).toFloat()
            val dy = (bowEffect.targetPosition.y - bowEffect.sourcePosition.y).toFloat()
            val angle =
                if (dx == 0f && dy == 0f) {
                    0f
                } else {
                    (atan2(dy.toDouble(), dx.toDouble()) * GamePlayConstants.AnimationTimings.RADIANS_TO_DEGREES).toFloat()
                }
            ArrowAttackAnimation(
                animate = AppSettings.enableAnimations.value,
                directionAngle = angle,
                isTargetTile = true,
                modifier = Modifier.fillMaxSize().zIndex(18f),
            )
        }
    }

    // Show dragon-targeting warning animation on mines that a dragon is approaching
    if (dragonIsTargetingMine) {
        DragonTargetAnimation(
            animate = AppSettings.enableAnimations.value,
            modifier = Modifier.fillMaxSize().zIndex(12f),
        )
    }

    // Show small bomb countdown overlay when an enemy is on the same bomb tile
    if (bombEffect != null && attacker != null) {
        Box(
            modifier = Modifier.fillMaxSize().zIndex(15f),
            contentAlignment = Alignment.BottomEnd,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(2.dp)
                        .background(
                            color = Color(0xFFCC0000),
                            shape = androidx.compose.foundation.shape.CircleShape,
                        ).padding(horizontal = 3.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${bombEffect.turnsRemaining}",
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }
    }

    // Draw target circles AFTER other content so they appear on top
    // Inner circles on central target tile, outer ring segments on neighbor tiles
    targetCircleInfo?.let { info ->
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .zIndex(11f),
        ) {
            when (info) {
                is TargetCircleInfo.CentralTarget -> {
                    // Draw 3 inner circles on the central target tile
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val center = Offset(centerX, centerY)

                    // Filled inner circle
                    drawCircle(
                        color = info.color,
                        radius = TargetCircleConstants.INNER_CIRCLE_1_RADIUS,
                        center = center,
                    )

                    // Two stroke circles
                    drawCircle(
                        color = info.color,
                        radius = TargetCircleConstants.INNER_CIRCLE_2_RADIUS,
                        center = center,
                        style =
                            Stroke(
                                width = TargetCircleConstants.INNER_CIRCLE_STROKE_WIDTH,
                            ),
                    )

                    drawCircle(
                        color = info.color,
                        radius = TargetCircleConstants.INNER_CIRCLE_3_RADIUS,
                        center = center,
                        style =
                            Stroke(
                                width = TargetCircleConstants.INNER_CIRCLE_STROKE_WIDTH,
                            ),
                    )
                }

                is TargetCircleInfo.NeighborTarget -> {
                    // Draw outer ring segments on neighbor tiles (only for AREA and LASTING)
                    if (info.attackType == AttackType.AREA || info.attackType == AttackType.LASTING) {
                        // Use different radii based on distance from center
                        // Distance 2 (extended area for level 20+) uses larger radii
                        val radius1 =
                            if (info.distanceFromCenter >= 2) {
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_1_RADIUS
                            } else {
                                TargetCircleConstants.OUTER_CIRCLE_1_RADIUS
                            }
                        val radius2 =
                            if (info.distanceFromCenter >= 2) {
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_2_RADIUS
                            } else {
                                TargetCircleConstants.OUTER_CIRCLE_2_RADIUS
                            }
                        val radius3 =
                            if (info.distanceFromCenter >= 2) {
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_3_RADIUS
                            } else {
                                TargetCircleConstants.OUTER_CIRCLE_3_RADIUS
                            }

                        // Draw 3 concentric arc segments
                        CircularSegmentDrawer.drawArcSegment(
                            drawScope = this,
                            color = info.color,
                            radius = radius1,
                            strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                            centerPos = info.centerPosition,
                            neighborPos = info.thisPosition,
                            hexSize = hexSize.value,
                        )

                        CircularSegmentDrawer.drawArcSegment(
                            drawScope = this,
                            color = info.color,
                            radius = radius2,
                            strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                            centerPos = info.centerPosition,
                            neighborPos = info.thisPosition,
                            hexSize = hexSize.value,
                        )

                        CircularSegmentDrawer.drawArcSegment(
                            drawScope = this,
                            color = info.color,
                            radius = radius3,
                            strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                            centerPos = info.centerPosition,
                            neighborPos = info.thisPosition,
                            hexSize = hexSize.value,
                        )
                    }
                }
            }
        }
    }

    // Draw dashed border for tower placement preview
    if (useDashedBorder) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .zIndex(12f),
        ) {
            val sqrt3 = sqrt(3.0).toFloat()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f

            // Create hexagon path
            val path =
                Path().apply {
                    // Top point
                    moveTo(centerX, centerY - radius)
                    // Top-right
                    lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
                    // Bottom-right
                    lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
                    // Bottom point
                    lineTo(centerX, centerY + radius)
                    // Bottom-left
                    lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
                    // Top-left
                    lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
                    // Close the path
                    close()
                }

            // Draw dashed border
            drawPath(
                path = path,
                color = borderColor,
                style =
                    Stroke(
                        width = borderWidth.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(
                                intervals = floatArrayOf(10f, 5f), // 10px dash, 5px gap
                                phase = 0f,
                            ),
                    ),
            )
        }
    }

    // Draw diagonal stripes for buildable tiles, tower bases, and placement tiles (trap/barricade/magical trap)
    if (showDiagonalStripes) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .zIndex(11f), // Below the dashed border
        ) {
            val sqrt3 = sqrt(3.0).toFloat()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f

            // Create hexagon clip path
            val hexPath =
                Path().apply {
                    moveTo(centerX, centerY - radius)
                    lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
                    lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
                    lineTo(centerX, centerY + radius)
                    lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
                    lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
                    close()
                }

            // Draw diagonal stripes with clipping
            drawContext.canvas.save()
            drawContext.canvas.clipPath(hexPath)

            // Draw diagonal stripes
            val stripeWidth = 8f
            val stripeSpacing = 16f
            val totalSpacing = stripeWidth + stripeSpacing
            val diagonalLength = size.width + size.height

            // Start from top-right, go to bottom-left (90 degree rotation)
            var offset = -diagonalLength
            while (offset < diagonalLength) {
                drawLine(
                    color = borderColor.copy(alpha = 0.8f), // 80% opacity
                    start = Offset(size.width - offset, 0f),
                    end = Offset(size.width - offset - size.height, size.height),
                    strokeWidth = stripeWidth,
                )
                offset += totalSpacing
            }

            drawContext.canvas.restore()
        }
    }

    // Debug overlay: tile borders by type
    if (AppSettings.showTileBorders.value) {
        val debugBorderColor =
            when {
                isSpawnPoint -> Color(0xFFFF4400)
                isTarget -> Color(0xFF00DD00)
                isRiverTile -> Color(0xFF0066FF)
                gameState.level.isOnPath(position) -> Color(0xFFFFAA00)
                gameState.level.isBuildArea(position) -> Color(0xFF44BB44)
                else -> Color(0xFF888888)
            }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .border(2.dp, debugBorderColor, HexagonShape()),
        )
    }

    // Debug overlay: tile position text
    if (AppSettings.showTilePositions.value) {
        Box(
            modifier =
                Modifier
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${position.x},${position.y}",
                fontSize = 8.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Visualize a bridge over a river tile
 */
@Composable
fun BridgeVisualization(bridge: Bridge) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Draw bridge arc
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val arcWidth = size.width * 0.8f
            val arcHeight = size.height * 0.4f

            // Bridge color based on type
            val bridgeColor =
                when (bridge.type) {
                    BridgeType.WOODEN -> Color(0xFF8B4513) // Brown
                    BridgeType.STONE -> Color(0xFF808080) // Gray
                    BridgeType.MAGICAL -> Color(0xFFFF00FF) // Magenta/purple for magical
                }

            // Draw half arc (bridge shape) - opening at bottom
            drawArc(
                color = bridgeColor,
                startAngle = 180f, // Start from bottom-left
                sweepAngle = 180f, // Draw top half
                useCenter = false,
                topLeft =
                    androidx.compose.ui.geometry.Offset(
                        centerX - arcWidth / 2,
                        centerY - arcHeight / 2,
                    ),
                size =
                    androidx.compose.ui.geometry
                        .Size(arcWidth, arcHeight),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 6f),
            )

            // For magical bridges, add sparkle effect above the arc
            if (bridge.type == BridgeType.MAGICAL) {
                // Draw sparkles around the arc (top side)
                val sparklePositions =
                    listOf(
                        androidx.compose.ui.geometry
                            .Offset(centerX - arcWidth / 3, centerY - arcHeight / 2 + 5),
                        androidx.compose.ui.geometry
                            .Offset(centerX, centerY - arcHeight / 2),
                        androidx.compose.ui.geometry
                            .Offset(centerX + arcWidth / 3, centerY - arcHeight / 2 + 5),
                    )
                sparklePositions.forEach { pos ->
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = pos,
                    )
                }
            }
        }

        // Display health or turn count below the arc
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 4.dp),
        ) {
            when (bridge.type) {
                BridgeType.WOODEN, BridgeType.STONE -> {
                    // Show remaining health
                    Text(
                        text = "${bridge.currentHealth.value}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                BridgeType.MAGICAL -> {
                    // Show remaining turns
                    Text(
                        text = "${bridge.turnsRemaining.value}T",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 13.sp,
                        color = Color(0xFFFFFF00), // Yellow
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Returns true if [pos] lies on the straight-line path between [source] and [target]
 * (excluding the source and target tiles themselves).
 * Uses linear interpolation over grid coordinates to determine intermediate tiles.
 */
private fun isOnArrowLinePath(
    source: Position,
    target: Position,
    pos: Position,
): Boolean {
    val dx = target.x - source.x
    val dy = target.y - source.y
    val steps = maxOf(abs(dx), abs(dy))
    if (steps <= 1) return false
    for (step in 1 until steps) {
        val t = step.toFloat() / steps
        val ix = (source.x + dx * t).roundToInt()
        val iy = (source.y + dy * t).roundToInt()
        if (ix == pos.x && iy == pos.y) return true
    }
    return false
}
