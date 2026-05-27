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
            AppSettings.saveShortcutCenterSelectedTower("T")
            AppSettings.saveShortcutCenterNextSpawnPoint("Y")

            val settingsMap = AppSettings.toSettingsMap()

            assertEquals("true", settingsMap["high_contrast"])
            assertEquals("PROTANOPIA", settingsMap["color_blind_palette"])
            assertEquals("true", settingsMap["captions_enabled"])
            assertEquals("true", settingsMap["hold_to_confirm"])
            assertEquals("T", settingsMap["shortcut_center_selected_tower"])
            assertEquals("Y", settingsMap["shortcut_center_next_spawn_point"])
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
                    "enable_animations" to "false",
                    "shortcut_center_selected_tower" to "U",
                    "shortcut_center_next_spawn_point" to "I"
                )
            )

            val preferences = AppSettings.getAccessibilityPreferences()
            assertTrue(preferences.highContrastEnabled)
            assertEquals(ColorBlindPalette.TRITANOPIA, preferences.colorBlindPalette)
            assertTrue(preferences.captionsEnabled)
            assertTrue(preferences.holdToConfirmEnabled)
            assertTrue(preferences.reduceMotionEnabled)
            assertEquals("U", AppSettings.shortcutCenterSelectedTower.value)
            assertEquals("I", AppSettings.shortcutCenterNextSpawnPoint.value)
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
        AppSettings.saveShortcutCenterSelectedTower("Z")
        AppSettings.saveShortcutCenterNextSpawnPoint("X")
        AppSettings.saveEnableAnimations(false)

        AppSettings.resetToDefaults()

        val preferences = AppSettings.getAccessibilityPreferences()
        assertFalse(preferences.highContrastEnabled)
        assertEquals(ColorBlindPalette.OFF, preferences.colorBlindPalette)
        assertFalse(preferences.captionsEnabled)
        assertFalse(preferences.holdToConfirmEnabled)
        assertFalse(preferences.reduceMotionEnabled)
        assertEquals("R", AppSettings.shortcutCenterSelectedTower.value)
        assertEquals("G", AppSettings.shortcutCenterNextSpawnPoint.value)
    }
}
