package de.egril.defender.ui

import androidx.compose.ui.unit.IntSize
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Constrains pan offsets to keep content visible within the viewport.
 * This ensures that the user can't pan beyond the edges of the content.
 *
 * @param newOffsetX The new horizontal offset
 * @param newOffsetY The new vertical offset
 * @param currentScale The current zoom scale
 * @param containerSize The size of the viewport container
 * @param contentSize The size of the actual content
 * @return A pair of constrained (offsetX, offsetY) values
 */
fun constrainMapOffsets(
    newOffsetX: Float,
    newOffsetY: Float,
    currentScale: Float,
    containerSize: IntSize,
    contentSize: IntSize,
): Pair<Float, Float> {
    // If content size hasn't been measured yet, don't constrain
    if (contentSize.width == 0 || contentSize.height == 0) {
        return Pair(newOffsetX, newOffsetY)
    }

    val contentWidth = contentSize.width * currentScale
    val contentHeight = contentSize.height * currentScale

    val maxOffsetX =
        if (contentWidth > containerSize.width) {
            (contentWidth - containerSize.width) / 2
        } else {
            (containerSize.width * (currentScale - 1) / 2).coerceAtLeast(0f)
        }

    val maxOffsetY =
        if (contentHeight > containerSize.height) {
            (contentHeight - containerSize.height) / 2
        } else {
            (containerSize.height * (currentScale - 1) / 2).coerceAtLeast(0f)
        }

    return Pair(
        newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
        newOffsetY.coerceIn(-maxOffsetY, maxOffsetY),
    )
}

/**
 * Computes the range of tile grid indices (x and y) that intersect the visible viewport.
 * Tiles outside this range can be replaced with lightweight empty composables to avoid
 * rendering full GridCell trees for off-screen tiles.
 *
 * A [buffer] tile margin is added on each side to prevent pop-in during panning.
 *
 * @param containerWidth  Viewport width in pixels (from onSizeChanged).
 * @param containerHeight Viewport height in pixels.
 * @param contentWidth    Full (unscaled) content width in pixels (from the layout pass).
 * @param contentHeight   Full (unscaled) content height in pixels.
 * @param scale           Current zoom scale factor.
 * @param offsetX         Horizontal pan offset in pixels (applied via graphicsLayer).
 * @param offsetY         Vertical pan offset in pixels.
 * @param hexWidth        Width of one hex tile in content pixels (hexSize * sqrt(3)).
 * @param verticalSpacing Base vertical distance between tile row centres in content pixels.
 * @param gridWidth       Total number of tile columns.
 * @param gridHeight      Total number of tile rows.
 * @param buffer          Extra tile margin added around the visible range (default 2).
 * @return IntArray of [minX, maxX, minY, maxY] (inclusive, clamped to grid bounds).
 *         Returns [0, gridWidth-1, 0, gridHeight-1] when sizes are zero (not yet measured).
 */
fun computeVisibleTileRange(
    containerWidth: Int,
    containerHeight: Int,
    contentWidth: Int,
    contentHeight: Int,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    hexWidth: Float,
    verticalSpacing: Float,
    gridWidth: Int,
    gridHeight: Int,
    buffer: Int = 2,
): IntArray {
    if (containerWidth == 0 || containerHeight == 0 || contentWidth == 0 || contentHeight == 0) {
        return intArrayOf(0, gridWidth - 1, 0, gridHeight - 1)
    }
    // The graphicsLayer in HexagonalMapView centres the content column and applies
    // translationX/Y = offsetX/Y.  The left edge of content in viewport px is:
    //   contentLeft = containerWidth / 2 - contentWidth * scale / 2 + offsetX
    // The visible content-coordinate range (before scale) is therefore:
    //   visLeft  = -contentLeft / scale
    //   visRight = visLeft + containerWidth / scale
    val contentLeft = containerWidth / 2f - contentWidth * scale / 2f + offsetX
    val contentTop = containerHeight / 2f - contentHeight * scale / 2f + offsetY
    val visLeft = -contentLeft / scale
    val visTop = -contentTop / scale
    val visRight = visLeft + containerWidth / scale
    val visBottom = visTop + containerHeight / scale

    // Use an exact intersection scan against the same tile placement math used in HexagonalMapView.
    // This is O(gridWidth * gridHeight) but still tiny versus composing full cell trees and avoids
    // approximation drift that can hide visible tiles (and their overlays/markers) near edges.
    val effectiveHorizontalColumnStep = hexWidth + HexagonalGridConstants.HORIZONTAL_SPACING
    val effectiveVerticalRowStep = verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT - 1f
    val oddRowOffset = hexWidth * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
    val hexHeight = hexWidth * 2f / sqrt(3f)

    var foundAnyVisibleTile = false
    var foundMinX = gridWidth - 1
    var foundMaxX = 0
    var foundMinY = gridHeight - 1
    var foundMaxY = 0

    for (y in 0 until gridHeight) {
        val rowTop = 1f + y * effectiveVerticalRowStep
        val rowBottom = rowTop + hexHeight
        val rowIntersectsViewport = rowBottom >= visTop && rowTop <= visBottom
        if (!rowIntersectsViewport) {
            continue
        }

        val rowXOffset = if (y % 2 == 1) oddRowOffset else 0f
        for (x in 0 until gridWidth) {
            val tileLeft = rowXOffset + x * effectiveHorizontalColumnStep
            val tileRight = tileLeft + hexWidth
            val tileIntersectsViewport = tileRight >= visLeft && tileLeft <= visRight
            if (!tileIntersectsViewport) {
                continue
            }

            foundAnyVisibleTile = true
            if (x < foundMinX) foundMinX = x
            if (x > foundMaxX) foundMaxX = x
            if (y < foundMinY) foundMinY = y
            if (y > foundMaxY) foundMaxY = y
        }
    }

    if (!foundAnyVisibleTile) {
        return intArrayOf(0, gridWidth - 1, 0, gridHeight - 1)
    }

    val minX = (foundMinX - buffer).coerceAtLeast(0)
    val maxX = (foundMaxX + buffer).coerceAtMost(gridWidth - 1)
    val minY = (foundMinY - buffer).coerceAtLeast(0)
    val maxY = (foundMaxY + buffer).coerceAtMost(gridHeight - 1)
    return intArrayOf(minX, maxX, minY, maxY)
}
