package de.egril.defender.ui

import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [computeVisibleTileRange] — the viewport-culling helper that determines which
 * hex-grid tile indices are visible given the current pan/zoom state.  These ensure the
 * function returns sensible ranges under a variety of zoom, pan, and size conditions.
 */
class ViewportTileCullingTest {
    // Hex geometry for a standard 40-dp hex (same values used by GameGrid / HexagonalMapView)
    private val hexSize = 40f
    private val sqrt3 = kotlin.math.sqrt(3.0).toFloat()
    private val hexWidth = hexSize * sqrt3 // ~69.28 dp
    private val hexHeight = hexSize * 2f // 80 dp
    private val verticalSpacing = hexHeight * 0.75f // 60 dp

    @Test
    fun returnsFullRangeWhenSizesAreZero() {
        val range =
            computeVisibleTileRange(
                containerWidth = 0,
                containerHeight = 0,
                contentWidth = 0,
                contentHeight = 0,
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = 40,
                gridHeight = 40,
            )
        assertEquals(0, range[0], "minX should be 0 when sizes are zero")
        assertEquals(39, range[1], "maxX should be gridWidth-1 when sizes are zero")
        assertEquals(0, range[2], "minY should be 0 when sizes are zero")
        assertEquals(39, range[3], "maxY should be gridHeight-1 when sizes are zero")
    }

    @Test
    fun rangeIsClampedToGridBounds() {
        // Small viewport, 1x scale, no offset — some tiles will be visible
        val range =
            computeVisibleTileRange(
                containerWidth = 800,
                containerHeight = 600,
                contentWidth = 2800,
                contentHeight = 2400,
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = 40,
                gridHeight = 40,
            )
        assertTrue(range[0] >= 0, "minX must be >= 0")
        assertTrue(range[1] <= 39, "maxX must be <= gridWidth-1")
        assertTrue(range[2] >= 0, "minY must be >= 0")
        assertTrue(range[3] <= 39, "maxY must be <= gridHeight-1")
        assertTrue(range[0] <= range[1], "minX must be <= maxX")
        assertTrue(range[2] <= range[3], "minY must be <= maxY")
    }

    @Test
    fun returnsFullRangeWhenZoomedOutToShowWholeMap() {
        // When the full content fits in the viewport, all tiles should be visible
        val gridWidth = 20
        val gridHeight = 20
        val contentWidth = (gridWidth * hexWidth).toInt()
        val contentHeight = (gridHeight * verticalSpacing).toInt()
        val range =
            computeVisibleTileRange(
                containerWidth = contentWidth + 100, // viewport larger than content
                containerHeight = contentHeight + 100,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                scale = 1f,
                offsetX = 0f,
                offsetY = 0f,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
            )
        assertEquals(0, range[0], "minX should be 0 when all tiles fit")
        assertEquals(gridWidth - 1, range[1], "maxX should be last column when all tiles fit")
        assertEquals(0, range[2], "minY should be 0 when all tiles fit")
        assertEquals(gridHeight - 1, range[3], "maxY should be last row when all tiles fit")
    }

    @Test
    fun visibleSubsetWhenZoomedIn() {
        // 2x zoom: only roughly half the tiles should be visible in each axis
        val gridWidth = 40
        val gridHeight = 40
        val contentWidth = (gridWidth * hexWidth).toInt()
        val contentHeight = (gridHeight * verticalSpacing).toInt()
        val containerWidth = 800
        val containerHeight = 600
        val range =
            computeVisibleTileRange(
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                scale = 2f,
                offsetX = 0f, // centered on the map
                offsetY = 0f,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
            )
        // With 2x zoom and centered offset, the visible area is half the content.
        // The visible tile count should be less than the full grid in both dimensions.
        val visibleColumns = range[1] - range[0] + 1
        val visibleRows = range[3] - range[2] + 1
        assertTrue(visibleColumns < gridWidth, "Fewer than all columns should be visible when zoomed in")
        assertTrue(visibleRows < gridHeight, "Fewer than all rows should be visible when zoomed in")
    }

    @Test
    fun bufferIsApplied() {
        // Without buffer, panning just 1px would pop tiles in; with buffer they're already included
        val gridWidth = 40
        val gridHeight = 40
        val contentWidth = (gridWidth * hexWidth).toInt()
        val contentHeight = (gridHeight * verticalSpacing).toInt()
        // Range with buffer=0
        val rangeNoBuffer =
            computeVisibleTileRange(
                containerWidth = 800,
                containerHeight = 600,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                scale = 2f,
                offsetX = 0f,
                offsetY = 0f,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                buffer = 0,
            )
        // Range with buffer=2 (default)
        val rangeWithBuffer =
            computeVisibleTileRange(
                containerWidth = 800,
                containerHeight = 600,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                scale = 2f,
                offsetX = 0f,
                offsetY = 0f,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
                buffer = 2,
            )
        // Buffer should expand the range in all four directions
        assertTrue(rangeWithBuffer[0] <= rangeNoBuffer[0], "Buffer should extend minX leftward")
        assertTrue(rangeWithBuffer[1] >= rangeNoBuffer[1], "Buffer should extend maxX rightward")
        assertTrue(rangeWithBuffer[2] <= rangeNoBuffer[2], "Buffer should extend minY upward")
        assertTrue(rangeWithBuffer[3] >= rangeNoBuffer[3], "Buffer should extend maxY downward")
    }

    @Test
    fun includesBottomRowWhenScrolledDownAndVisible() {
        val gridWidth = 40
        val gridHeight = 40
        val containerWidth = 800
        val containerHeight = 600
        val scale = 1.5f
        val offsetY = -1200f
        val contentWidth = (gridWidth * hexWidth).toInt()
        val contentHeight = (gridHeight * verticalSpacing).toInt()

        val range =
            computeVisibleTileRange(
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                scale = scale,
                offsetX = 0f,
                offsetY = offsetY,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
            )

        val contentTop = containerHeight / 2f - contentHeight * scale / 2f + offsetY
        val visTop = -contentTop / scale
        val visBottom = visTop + containerHeight / scale
        val effectiveRowStep = verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT - 1f
        val lastRow = gridHeight - 1
        val lastRowTop = lastRow * effectiveRowStep + 1f
        val lastRowBottom = lastRowTop + hexHeight
        val lastRowIntersectsViewport = lastRowBottom >= visTop && lastRowTop <= visBottom

        assertTrue(lastRowIntersectsViewport, "Test setup requires the bottom row to be visible")
        assertTrue(
            lastRow in range[2]..range[3],
            "Bottom row should be included in visible Y-range when it intersects the viewport",
        )
    }

    @Test
    fun includesRightColumnsWhenPannedToBottomRight() {
        val gridWidth = 40
        val gridHeight = 40
        val containerWidth = 800
        val containerHeight = 600
        val scale = 1.0f
        val offsetX = -805f
        val offsetY = -773f

        val horizontalStep = hexWidth + HexagonalGridConstants.HORIZONTAL_SPACING
        val oddRowOffset = hexWidth * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
        val contentWidth = ((gridWidth - 1) * horizontalStep + hexWidth + oddRowOffset).toInt()
        val contentHeight = (hexHeight + (gridHeight - 1) * (verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT)).toInt()

        val range =
            computeVisibleTileRange(
                containerWidth = containerWidth,
                containerHeight = containerHeight,
                contentWidth = contentWidth,
                contentHeight = contentHeight,
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                hexWidth = hexWidth,
                verticalSpacing = verticalSpacing,
                gridWidth = gridWidth,
                gridHeight = gridHeight,
            )

        val contentLeft = containerWidth / 2f - contentWidth * scale / 2f + offsetX
        val visLeft = -contentLeft / scale
        val visRight = visLeft + containerWidth / scale
        val targetColumn = 37
        val evenRow = 30
        val targetLeft = targetColumn * horizontalStep
        val targetRight = targetLeft + hexWidth
        val targetIntersectsViewport = targetRight >= visLeft && targetLeft <= visRight

        assertTrue(targetIntersectsViewport, "Test setup requires column 37 to intersect the viewport")
        assertTrue(
            targetColumn in range[0]..range[1],
            "Column 37 should be included in visible X-range when it intersects the viewport",
        )
        assertTrue(evenRow in range[2]..range[3], "Bottom-area rows should remain visible in this pan position")
    }

    @Test
    fun alwaysIncludesEveryVisibleTileAcrossPanAndZoomSamples() {
        val gridWidth = 40
        val gridHeight = 40
        val containerWidth = 800
        val containerHeight = 600
        val horizontalStep = hexWidth + HexagonalGridConstants.HORIZONTAL_SPACING
        val oddRowOffset = hexWidth * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
        val rowStep = verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT - 1f

        val evenRowWidth = gridWidth * hexWidth + (gridWidth - 1) * HexagonalGridConstants.HORIZONTAL_SPACING
        val oddRowWidth = oddRowOffset + evenRowWidth
        val contentWidth = maxOf(evenRowWidth, oddRowWidth).toInt()
        val contentHeight = (hexHeight + (gridHeight - 1) * (verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT)).toInt()

        val testScales = listOf(0.8f, 1.0f, 1.3f, 1.8f, 2.5f)
        for (scale in testScales) {
            val maxOffsetX = maxOf(0f, (contentWidth * scale - containerWidth) / 2f)
            val maxOffsetY = maxOf(0f, (contentHeight * scale - containerHeight) / 2f)
            val sampledOffsetX = listOf(-maxOffsetX, -maxOffsetX * 0.5f, 0f, maxOffsetX * 0.5f, maxOffsetX)
            val sampledOffsetY = listOf(-maxOffsetY, -maxOffsetY * 0.5f, 0f, maxOffsetY * 0.5f, maxOffsetY)

            for (offsetX in sampledOffsetX) {
                for (offsetY in sampledOffsetY) {
                    val range =
                        computeVisibleTileRange(
                            containerWidth = containerWidth,
                            containerHeight = containerHeight,
                            contentWidth = contentWidth,
                            contentHeight = contentHeight,
                            scale = scale,
                            offsetX = offsetX,
                            offsetY = offsetY,
                            hexWidth = hexWidth,
                            verticalSpacing = verticalSpacing,
                            gridWidth = gridWidth,
                            gridHeight = gridHeight,
                        )

                    val contentLeft = containerWidth / 2f - contentWidth * scale / 2f + offsetX
                    val contentTop = containerHeight / 2f - contentHeight * scale / 2f + offsetY
                    val visLeft = -contentLeft / scale
                    val visTop = -contentTop / scale
                    val visRight = visLeft + containerWidth / scale
                    val visBottom = visTop + containerHeight / scale

                    for (y in 0 until gridHeight) {
                        val rowTop = 1f + y * rowStep
                        val rowBottom = rowTop + hexHeight
                        val rowIntersects = rowBottom >= visTop && rowTop <= visBottom
                        if (!rowIntersects) {
                            continue
                        }

                        val rowOffset = if (y % 2 == 1) oddRowOffset else 0f
                        for (x in 0 until gridWidth) {
                            val tileLeft = rowOffset + x * horizontalStep
                            val tileRight = tileLeft + hexWidth
                            val intersects = tileRight >= visLeft && tileLeft <= visRight
                            if (!intersects) {
                                continue
                            }
                            assertTrue(
                                x in range[0]..range[1] && y in range[2]..range[3],
                                "Visible tile ($x,$y) must be included in culling range ${range.toList()} for scale=$scale offset=($offsetX,$offsetY)",
                            )
                        }
                    }
                }
            }
        }
    }
}
