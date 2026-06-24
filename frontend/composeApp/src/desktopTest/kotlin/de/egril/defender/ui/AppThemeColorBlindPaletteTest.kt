package de.egril.defender.ui

import androidx.compose.ui.graphics.Color
import de.egril.defender.ui.a11y.ColorBlindPalette
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppThemeColorBlindPaletteTest {
    @Test
    fun colorBlindPaletteOffReturnsBaseScheme() {
        val base = AppTheme.lightColorScheme
        val adjusted = AppTheme.applyColorBlindPalette(base, ColorBlindPalette.OFF)

        assertEquals(base, adjusted)
        assertEquals(base.primary, adjusted.primary)
        assertEquals(base.secondary, adjusted.secondary)
        assertEquals(base.tertiary, adjusted.tertiary)
        assertEquals(base.background, adjusted.background)
        assertEquals(base.onBackground, adjusted.onBackground)
    }

    @Test
    fun protanopiaPaletteAdjustsPrimaryColor() {
        val adjusted = AppTheme.applyColorBlindPalette(AppTheme.lightColorScheme, ColorBlindPalette.PROTANOPIA)
        assertEquals(Color(0xFF005A9C), adjusted.primary)
        assertNotEquals(AppTheme.lightColorScheme.primary, adjusted.primary)
    }

    @Test
    fun tritanopiaPaletteAdjustsSecondaryColor() {
        val adjusted = AppTheme.applyColorBlindPalette(AppTheme.darkColorScheme, ColorBlindPalette.TRITANOPIA)
        assertEquals(Color(0xFF0A6E6E), adjusted.secondary)
        assertNotEquals(AppTheme.darkColorScheme.secondary, adjusted.secondary)
    }
}
