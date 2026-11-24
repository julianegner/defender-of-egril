package de.egril.defender.ui.gameplay

import kotlin.test.Test
import kotlin.test.assertEquals

class VaelenBossHealthBarTest {
    @Test
    fun progressTracksClampedHealthRatio() {
        assertEquals(1f, bossHealthBarProgress(300, 300))
        assertEquals(0.5f, bossHealthBarProgress(150, 300))
        assertEquals(0f, bossHealthBarProgress(-1, 300))
        assertEquals(0f, bossHealthBarProgress(1, 0))
    }
}
