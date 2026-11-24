package de.egril.defender.ui.gameplay

import de.egril.defender.model.AttackerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GhostSwarmCountTest {
    @Test
    fun returnsDisplayedHealthForSwarmUnits() {
        assertEquals(7, ghostSwarmCount(AttackerType.SNOTLING, 7))
        assertEquals(4, ghostSwarmCount(AttackerType.SPIDERLING, 4))
        assertEquals(9, ghostSwarmCount(AttackerType.DEMONLING, 9))
    }

    @Test
    fun clampsSwarmCountToAtLeastOne() {
        assertEquals(1, ghostSwarmCount(AttackerType.SNOTLING, 0))
        assertEquals(1, ghostSwarmCount(AttackerType.SNOTLING, -3))
    }

    @Test
    fun returnsNullForNonSwarmUnits() {
        assertNull(ghostSwarmCount(AttackerType.GOBLIN, 5))
    }
}
