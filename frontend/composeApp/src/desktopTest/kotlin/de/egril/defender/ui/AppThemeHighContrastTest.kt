package de.egril.defender.ui

import androidx.compose.ui.graphics.Color
import org.junit.Test
import kotlin.test.assertEquals

class AppThemeHighContrastTest {

    @Test
    fun highContrastLightSchemeUsesMaxContrastBackgroundAndText() {
        assertEquals(Color(0xFFFFFFFF), AppTheme.highContrastLightColorScheme.background)
        assertEquals(Color(0xFF000000), AppTheme.highContrastLightColorScheme.onBackground)
        assertEquals(Color(0xFFFFFFFF), AppTheme.highContrastLightColorScheme.surface)
        assertEquals(Color(0xFF000000), AppTheme.highContrastLightColorScheme.onSurface)
    }

    @Test
    fun highContrastDarkSchemeUsesMaxContrastBackgroundAndText() {
        assertEquals(Color(0xFF000000), AppTheme.highContrastDarkColorScheme.background)
        assertEquals(Color(0xFFFFFFFF), AppTheme.highContrastDarkColorScheme.onBackground)
        assertEquals(Color(0xFF000000), AppTheme.highContrastDarkColorScheme.surface)
        assertEquals(Color(0xFFFFFFFF), AppTheme.highContrastDarkColorScheme.onSurface)
    }
}

