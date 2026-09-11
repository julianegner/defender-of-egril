package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaaghMeterColorTest {
    @Test
    fun percentFormattingClampsToValidRange() {
        assertEquals("0%", formatWaaghMeterPercent(-5))
        assertEquals("35%", formatWaaghMeterPercent(35))
        assertEquals("100%", formatWaaghMeterPercent(120))
    }

    @Test
    fun colorStopsMatchExpectedEnds() {
        assertEquals(Color(0xFF5A6B7C), getWaaghMeterColor(0f))
        assertEquals(Color(0xFFFFEA00), getWaaghMeterColor(1f))
    }

    @Test
    fun colorGetsWarmerAsMeterIncreases() {
        val start = getWaaghMeterColor(0f)
        val middle = getWaaghMeterColor(0.5f)
        val end = getWaaghMeterColor(1f)

        assertTrue(middle.red >= start.red)
        assertTrue(end.red >= middle.red)
        assertTrue(middle.blue <= start.blue)
        assertTrue(end.blue <= middle.blue)
        assertTrue(middle.green >= start.green)
        assertTrue(end.green >= middle.green)
    }
}
