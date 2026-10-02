package de.egril.defender.ui.gameplay

import de.egril.defender.model.EventMapImage
import de.egril.defender.ui.hexagon.HexagonalGridConstants
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class EventMapImageBoundsTest {
    @Test
    fun decimalGeometryUsesGridSpacingAndFullTileSize() {
        val bounds = eventMapImageBounds(EventMapImage("image", "image.png", 2.5f, 3.5f, 4.5f, 2f), 40f)
        val tileWidth = 40f * sqrt(3f)
        assertEquals(2.5f * (tileWidth + HexagonalGridConstants.HORIZONTAL_SPACING), bounds.offset.x)
        assertEquals(3.5f * (60f + HexagonalGridConstants.VERTICAL_SPACING_ADJUSTMENT - 1f) + 1f, bounds.offset.y)
        assertEquals(4.5f * tileWidth, bounds.size.width)
        assertEquals(160f, bounds.size.height)
    }

    @Test
    fun changingHexSizeScalesImageDimensions() {
        val image = EventMapImage("image", "image.png", width = 3f, height = 2.5f)
        val small = eventMapImageBounds(image, 20f)
        val large = eventMapImageBounds(image, 40f)
        assertEquals(small.size.width * 2f, large.size.width)
        assertEquals(small.size.height * 2f, large.size.height)
        assertEquals(0f, large.offset.x)
        assertEquals(1f, large.offset.y)
    }
}
