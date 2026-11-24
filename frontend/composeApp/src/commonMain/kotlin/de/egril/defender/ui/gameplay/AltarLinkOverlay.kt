package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import de.egril.defender.model.AltarLink
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.sqrt

/**
 * Distance from the tile centre up to the green orb of an altar, in units of the hex size.
 * Derived from the position of the green orb in `altar_active.png`.
 */
private const val ALTAR_HEAD_OFFSET_IN_HEX_SIZES = 0.68f

/** Visible width of the line (the glow stroke), in units of the hex size. */
private const val LINE_WIDTH_IN_HEX_SIZES = 0.22f

private val AltarLinkGreen = Color(0xFF6CFF6C)

/**
 * Map-level overlay that draws a green line between the heads of two altars for every
 * [AltarLink] shown by a scripted event. The line is drawn above the map, inside the map's
 * pan/zoom transform, like the other map-level overlays.
 */
@Composable
internal fun AltarLinkMapOverlay(
    links: List<AltarLink>,
    hexSizeDp: Float,
    contentSize: IntSize,
) {
    if (links.isEmpty()) return

    val density = LocalDensity.current.density
    val hexSizePx = hexSizeDp * density
    val hexWidthPx = hexSizePx * sqrt(3f)
    val hexHeightPx = hexSizePx * 2f
    val rowSpacingPx =
        hexHeightPx * 0.75f - hexHeightPx + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT * density
    val colSpacingPx = HexagonalGridConstants.HORIZONTAL_SPACING * density
    val oddOffsetPx = hexWidthPx * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO

    fun altarHeadPx(pos: Position): Offset {
        val oddRowOffset = if (pos.y % 2 == 1) oddOffsetPx else 0f
        // Shifted right by the visible line width so the line runs through the middle of the head.
        val centerX = pos.x * (hexWidthPx + colSpacingPx) + hexWidthPx / 2f + oddRowOffset + hexSizePx * LINE_WIDTH_IN_HEX_SIZES
        // Same per-row visual correction as the map's tile rows.
        val centerY = pos.y * (hexHeightPx + rowSpacingPx) + hexHeightPx / 2f - (pos.y - 1) * density
        return Offset(centerX, centerY - ALTAR_HEAD_OFFSET_IN_HEX_SIZES * hexSizePx)
    }

    val contentWidthDp = (contentSize.width / density).dp
    val contentHeightDp = (contentSize.height / density).dp

    Canvas(Modifier.requiredSize(contentWidthDp, contentHeightDp)) {
        for (link in links) {
            val start = altarHeadPx(link.from)
            val end = altarHeadPx(link.to)
            // A wide translucent stroke gives the line a soft glow; the thin core keeps it crisp.
            drawLine(
                color = AltarLinkGreen.copy(alpha = 0.35f),
                start = start,
                end = end,
                strokeWidth = hexSizePx * LINE_WIDTH_IN_HEX_SIZES,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = AltarLinkGreen,
                start = start,
                end = end,
                strokeWidth = hexSizePx * 0.07f,
                cap = StrokeCap.Round,
            )
        }
    }
}
