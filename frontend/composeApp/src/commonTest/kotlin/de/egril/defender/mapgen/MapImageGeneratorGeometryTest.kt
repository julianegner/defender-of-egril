package de.egril.defender.mapgen

import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class MapImageGeneratorGeometryTest {
    @Test
    fun imageSizeMatchesGameplayGridFormulaForLargeMap() {
        val gridWidth = 45
        val gridHeight = 60

        val (actualWidth, actualHeight) = MapImageGenerator.imageSize(gridWidth, gridHeight)

        val hexSize = 40.0
        val hexWidth = hexSize * sqrt(3.0)
        val hexHeight = hexSize * 2.0
        val verticalSpacing = hexHeight * 0.75
        val rowStep = verticalSpacing + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT
        val oddOffset = hexWidth * HexagonalGridConstants.ODD_ROW_OFFSET_RATIO
        val expectedWidth =
            ceil(
                gridWidth * hexWidth +
                    (gridWidth - 1) * HexagonalGridConstants.HORIZONTAL_SPACING +
                    oddOffset,
            ).toInt()
        val expectedHeight = ceil((gridHeight - 1) * rowStep + hexHeight).toInt()

        assertEquals(expectedWidth, actualWidth)
        assertEquals(expectedHeight, actualHeight)
    }

    @Test
    fun imageSizeUsesMaxOddRowOffsetIndependentOfLastRowParity() {
        val width = 40
        val (sizeWidthTwoRows, _) = MapImageGenerator.imageSize(width, 2)
        val (sizeWidthThreeRows, _) = MapImageGenerator.imageSize(width, 3)

        // Width should use the maximum possible odd-row offset as soon as at least one odd row exists.
        assertEquals(sizeWidthTwoRows, sizeWidthThreeRows)
    }

    @Test
    fun hexCenterUsesVisualRowStepFromGameplayLayout() {
        val (_, row0Y) = MapImageGenerator.hexCenter(0, 0)
        val (_, row1Y) = MapImageGenerator.hexCenter(0, 1)
        val (_, row35Y) = MapImageGenerator.hexCenter(0, 35)
        val (_, row36Y) = MapImageGenerator.hexCenter(0, 36)

        assertEquals(41.0, row0Y)
        assertEquals(52.0, row1Y - row0Y)
        assertEquals(52.0, row36Y - row35Y)
    }
}
