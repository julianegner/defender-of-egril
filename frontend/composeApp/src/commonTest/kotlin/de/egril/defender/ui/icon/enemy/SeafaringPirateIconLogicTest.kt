package de.egril.defender.ui.icon.enemy

import de.egril.defender.model.AttackerType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeafaringPirateIconLogicTest {
    @Test
    fun pirateAndRoderichShowBargeOnlyOnRiverTiles() {
        assertTrue(shouldShowSeafaringPirateBarge(AttackerType.PIRATE, isRiverTile = true))
        assertTrue(shouldShowSeafaringPirateBarge(AttackerType.CAPTAIN_RODERICH, isRiverTile = true))

        assertFalse(shouldShowSeafaringPirateBarge(AttackerType.PIRATE, isRiverTile = false))
        assertFalse(shouldShowSeafaringPirateBarge(AttackerType.CAPTAIN_RODERICH, isRiverTile = false))
        assertFalse(shouldShowSeafaringPirateBarge(AttackerType.GOBLIN, isRiverTile = true))
    }
}
