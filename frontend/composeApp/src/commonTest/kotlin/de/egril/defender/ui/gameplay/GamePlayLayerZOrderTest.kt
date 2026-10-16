package de.egril.defender.ui.gameplay

import kotlin.test.Test
import kotlin.test.assertTrue

class GamePlayLayerZOrderTest {
    @Test
    fun gamepadRendersAboveRiverArrows() {
        assertTrue(
            GamePlayConstants.LayerZ.Gamepad > GamePlayConstants.LayerZ.RiverArrows,
            "Gamepad z-index must be higher than river arrows to prevent arrows from rendering above controls",
        )
    }
}
