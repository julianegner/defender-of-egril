package de.egril.defender.ui.gameplay

import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameMapRiverTileSelectionTest {
    @Test
    fun sandboxPaintedRiverTileOverridesLevelRiverTile() {
        val position = Position(3, 4)
        val levelRiverTile = RiverTile(position = position, flowDirection = RiverFlow.EAST, flowSpeed = 1)
        val sandboxRiverTile = RiverTile(position = position, flowDirection = RiverFlow.SOUTH_WEST, flowSpeed = 2)

        assertEquals(sandboxRiverTile, displayedRiverTile(levelRiverTile, sandboxRiverTile))
    }

    @Test
    fun levelRiverTileIsUsedWhenSandboxHasNoOverride() {
        val position = Position(3, 4)
        val levelRiverTile = RiverTile(position = position, flowDirection = RiverFlow.EAST, flowSpeed = 1)

        assertEquals(levelRiverTile, displayedRiverTile(levelRiverTile, null))
    }

    @Test
    fun nullIsReturnedWhenNoRiverTileExists() {
        assertNull(displayedRiverTile(levelRiverTile = null, sandboxPaintedRiverTile = null))
    }
}
