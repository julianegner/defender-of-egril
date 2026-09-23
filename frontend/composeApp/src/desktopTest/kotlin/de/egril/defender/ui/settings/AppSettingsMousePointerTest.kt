package de.egril.defender.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsMousePointerTest {
    @Test
    fun toSettingsMap_includesMousePointerSettings() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.saveMousePointerSource(MousePointerSource.SYSTEM)
            AppSettings.saveMousePointerSize(MousePointerSize.HUGE)
            AppSettings.saveMousePointerDirection(MousePointerDirection.LEFT)
            AppSettings.saveMousePointerSkinBrightness(0.2f)

            val settingsMap = AppSettings.toSettingsMap()

            assertEquals("SYSTEM", settingsMap["mouse_pointer_source"])
            assertEquals("HUGE", settingsMap["mouse_pointer_size"])
            assertEquals("LEFT", settingsMap["mouse_pointer_direction"])
            assertEquals("0.2", settingsMap["mouse_pointer_skin_brightness"])
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun applyFromSettingsMap_appliesMousePointerSettings() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.applyFromSettingsMap(
                mapOf(
                    "mouse_pointer_source" to "SYSTEM",
                    "mouse_pointer_size" to "EXTRA_LARGE",
                    "mouse_pointer_direction" to "LEFT",
                    "mouse_pointer_skin_brightness" to "-0.15",
                ),
            )

            assertEquals(MousePointerSource.SYSTEM, AppSettings.mousePointerSource.value)
            assertEquals(MousePointerSize.EXTRA_LARGE, AppSettings.mousePointerSize.value)
            assertEquals(MousePointerDirection.LEFT, AppSettings.mousePointerDirection.value)
            assertEquals(-0.15f, AppSettings.mousePointerSkinBrightness.value)
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun mousePointerSkinBrightness_isClampedToSupportedRange() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.saveMousePointerSkinBrightness(1.0f)
            assertEquals(0.35f, AppSettings.mousePointerSkinBrightness.value)

            AppSettings.applyFromSettingsMap(
                mapOf(
                    "mouse_pointer_skin_brightness" to "-1.0",
                ),
            )
            assertEquals(-0.35f, AppSettings.mousePointerSkinBrightness.value)
        } finally {
            AppSettings.resetToDefaults()
        }
    }

    @Test
    fun applyFromSettingsMap_migratesLegacyMousePointerValues() {
        AppSettings.resetToDefaults()
        try {
            AppSettings.applyFromSettingsMap(
                mapOf(
                    "mouse_pointer_source" to "DEFAULT",
                    "mouse_pointer_size" to "DEFAULT",
                    "mouse_pointer_direction" to "DEFAULT",
                ),
            )

            assertEquals(MousePointerSource.GAME, AppSettings.mousePointerSource.value)
            assertEquals(MousePointerSize.DEFAULT, AppSettings.mousePointerSize.value)
            assertEquals(MousePointerDirection.DEFAULT, AppSettings.mousePointerDirection.value)
        } finally {
            AppSettings.resetToDefaults()
        }
    }
}
