package de.egril.defender.ui.a11y

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccessibilityVisualFilterTest {
    @Test
    fun createAccessibilityColorMatrixReturnsNullWhenNoVisualAdjustmentIsEnabled() {
        val matrix =
            createAccessibilityColorMatrix(
                highContrastEnabled = false,
                colorBlindPalette = ColorBlindPalette.OFF,
            )
        assertNull(matrix)
    }

    @Test
    fun createAccessibilityColorMatrixReturnsMatrixForHighContrast() {
        val matrix =
            createAccessibilityColorMatrix(
                highContrastEnabled = true,
                colorBlindPalette = ColorBlindPalette.OFF,
            )
        assertNotNull(matrix)
    }

    @Test
    fun createAccessibilityColorMatrixReturnsMatrixForColorBlindModes() {
        ColorBlindPalette.entries
            .filter { it != ColorBlindPalette.OFF }
            .forEach { palette ->
                val matrix =
                    createAccessibilityColorMatrix(
                        highContrastEnabled = false,
                        colorBlindPalette = palette,
                    )
                assertNotNull(matrix)
            }
    }
}
