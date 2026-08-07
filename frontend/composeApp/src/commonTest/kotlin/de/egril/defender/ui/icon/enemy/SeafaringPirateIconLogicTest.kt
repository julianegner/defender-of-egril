package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.graphics.Color
import de.egril.defender.model.AttackerType
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun pirateAndRoderichKeepWhiteOutlineOnAllBackgrounds() {
        assertEquals(Color.White, attackerOutlineColor(AttackerType.PIRATE, Color.Black))
        assertEquals(Color.White, attackerOutlineColor(AttackerType.CAPTAIN_RODERICH, Color.Black))
        assertEquals(Color.Black, attackerOutlineColor(AttackerType.GOBLIN, Color.Black))
    }
}
