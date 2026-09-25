package de.egril.defender.mapgen

import kotlin.test.Test
import kotlin.test.assertEquals

class MapImageGeneratorTest {
    @Test
    fun nearestTileIndexReturnsClosestCenter() {
        val cX = doubleArrayOf(10.0, 50.0, 100.0)
        val cY = doubleArrayOf(10.0, 50.0, 100.0)

        assertEquals(0, MapImageGenerator.nearestTileIndex(12.0, 9.0, cX, cY))
        assertEquals(1, MapImageGenerator.nearestTileIndex(55.0, 60.0, cX, cY))
        assertEquals(2, MapImageGenerator.nearestTileIndex(120.0, 90.0, cX, cY))
    }
}
