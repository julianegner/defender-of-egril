package de.egril.defender.ui

import androidx.compose.ui.graphics.Color
import de.egril.defender.ui.a11y.ColorBlindPalette
import de.egril.defender.ui.gameplay.GamePlayColors
import org.junit.Test
import kotlin.test.assertTrue

class AppThemeContrastAuditTest {
    @Test
    fun keyThemeTextPairsMeetAaContrast() {
        val schemes =
            listOf(
                "light" to AppTheme.lightColorScheme,
                "dark" to AppTheme.darkColorScheme,
                "highContrastLight" to AppTheme.highContrastLightColorScheme,
                "highContrastDark" to AppTheme.highContrastDarkColorScheme,
                "lightDeuteranopia" to AppTheme.applyColorBlindPalette(AppTheme.lightColorScheme, ColorBlindPalette.DEUTERANOPIA),
                "lightProtanopia" to AppTheme.applyColorBlindPalette(AppTheme.lightColorScheme, ColorBlindPalette.PROTANOPIA),
                "lightTritanopia" to AppTheme.applyColorBlindPalette(AppTheme.lightColorScheme, ColorBlindPalette.TRITANOPIA),
                "darkDeuteranopia" to AppTheme.applyColorBlindPalette(AppTheme.darkColorScheme, ColorBlindPalette.DEUTERANOPIA),
                "darkProtanopia" to AppTheme.applyColorBlindPalette(AppTheme.darkColorScheme, ColorBlindPalette.PROTANOPIA),
                "darkTritanopia" to AppTheme.applyColorBlindPalette(AppTheme.darkColorScheme, ColorBlindPalette.TRITANOPIA),
            )

        schemes.forEach { (name, scheme) ->
            assertAa(name, "onBackground/background", scheme.onBackground, scheme.background)
            assertAa(name, "onSurface/surface", scheme.onSurface, scheme.surface)
            assertAa(name, "onPrimary/primary", scheme.onPrimary, scheme.primary)
            assertAa(name, "onSecondary/secondary", scheme.onSecondary, scheme.secondary)
            assertAa(name, "onTertiary/tertiary", scheme.onTertiary, scheme.tertiary)
            assertAa(name, "onError/error", scheme.onError, scheme.error)
        }
    }

    @Test
    fun gameplayAccentButtonsPickReadableContentColor() {
        val accentBackgrounds =
            listOf(
                Color(0xFFFF5722), // warning deep light
                Color(0xFF2196F3), // info light
                Color(0xFF4CAF50), // success light
                Color(0xFFEF6C00), // warning dark mode
                Color(0xFF0D47A1), // info dark mode
            )

        accentBackgrounds.forEach { background ->
            val content = GamePlayColors.readableContentColor(background)
            val ratio = GamePlayColors.contrastRatio(content, background)
            assertTrue(ratio >= 4.5, "Expected readable content color to meet AA on $background, got $ratio")
        }
    }

    private fun assertAa(
        schemeName: String,
        pairName: String,
        foreground: Color,
        background: Color,
    ) {
        val ratio = GamePlayColors.contrastRatio(foreground, background)
        assertTrue(ratio >= 4.5, "Expected AA contrast for $schemeName ($pairName), got $ratio")
    }
}
