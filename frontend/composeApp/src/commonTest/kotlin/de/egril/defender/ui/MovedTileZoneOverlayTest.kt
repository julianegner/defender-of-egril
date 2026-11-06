package de.egril.defender.ui

import de.egril.defender.model.Position
import de.egril.defender.model.RegionMove
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MovedTileZoneOverlayTest {
    private val asymmetricRegion =
        listOf(
            Position(4, 4), Position(5, 4), Position(4, 5), Position(3, 5),
            Position(5, 3), Position(6, 3), Position(7, 4), Position(6, 5),
        )

    private fun movedPairs(move: RegionMove): List<Pair<Position, Position>> {
        val mapper = move.mapperFor(asymmetricRegion)
        return asymmetricRegion.map { home -> mapper(home) to home }
    }

    @Test
    fun imageTransformReproducesEveryMovedTileWithinAFewPixels() {
        for (steps in 0..5) {
            for ((dx, dy) in listOf(0 to 0, 3 to -2, -4 to 1)) {
                val transform = assertNotNull(movedZoneImageTransform(movedPairs(RegionMove(steps, dx, dy))))
                assertTrue(transform.maxResidual < 3.0, "steps=$steps dx=$dx dy=$dy residual=${transform.maxResidual}")
            }
        }
    }

    @Test
    fun asymmetricRegionRotationIsRecoveredAsNonZeroAngle() {
        val transform = assertNotNull(movedZoneImageTransform(movedPairs(RegionMove(rotationSteps = 1))))
        assertTrue(transform.degrees != 0.0, "A one-step rotation must not be fitted as a pure translation")
    }
}
