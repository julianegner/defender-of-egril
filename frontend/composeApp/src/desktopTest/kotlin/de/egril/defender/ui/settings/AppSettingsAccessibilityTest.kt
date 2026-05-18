package de.egril.defender.ui.settings

import de.egril.defender.ui.a11y.ColorBlindPalette
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsAccessibilityTest {

    @Test
    fun toSettingsMap_includesAccessibilitySettings() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.saveHighContrastEnabled(true)
            AppSettings.saveColorBlindPalette(ColorBlindPalette.PROTANOPIA)
            AppSettings.saveCaptionsEnabled(true)
            AppSettings.saveHoldToConfirmEnabled(true)

            val settingsMap = AppSettings.toSettingsMap()

            assertEquals("true", settingsMap["high_contrast"])
            assertEquals("PROTANOPIA", settingsMap["color_blind_palette"])
            assertEquals("true", settingsMap["captions_enabled"])
            assertEquals("true", settingsMap["hold_to_confirm"])
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun applyFromSettingsMap_appliesAccessibilitySettings() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.applyFromSettingsMap(
                mapOf(
                    "high_contrast" to "true",
                    "color_blind_palette" to "TRITANOPIA",
                    "captions_enabled" to "true",
                    "hold_to_confirm" to "true",
                    "enable_animations" to "false"
                )
            )

            val preferences = AppSettings.getAccessibilityPreferences()
            assertTrue(preferences.highContrastEnabled)
            assertEquals(ColorBlindPalette.TRITANOPIA, preferences.colorBlindPalette)
            assertTrue(preferences.captionsEnabled)
            assertTrue(preferences.holdToConfirmEnabled)
            assertTrue(preferences.reduceMotionEnabled)
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun resetToDefaults_resetsAccessibilitySettings() {
        AppSettings.saveHighContrastEnabled(true)
        AppSettings.saveColorBlindPalette(ColorBlindPalette.DEUTERANOPIA)
        AppSettings.saveCaptionsEnabled(true)
        AppSettings.saveHoldToConfirmEnabled(true)
        AppSettings.saveEnableAnimations(false)

        AppSettings.resetToDefaults()

        val preferences = AppSettings.getAccessibilityPreferences()
        assertFalse(preferences.highContrastEnabled)
        assertEquals(ColorBlindPalette.OFF, preferences.colorBlindPalette)
        assertFalse(preferences.captionsEnabled)
        assertFalse(preferences.holdToConfirmEnabled)
        assertFalse(preferences.reduceMotionEnabled)
    }
}
