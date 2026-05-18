package de.egril.defender.ui.a11y

enum class ColorBlindPalette {
    OFF,
    DEUTERANOPIA,
    PROTANOPIA,
    TRITANOPIA
}

data class AccessibilityPreferences(
    val highContrastEnabled: Boolean = false,
    val colorBlindPalette: ColorBlindPalette = ColorBlindPalette.OFF,
    val captionsEnabled: Boolean = false,
    val holdToConfirmEnabled: Boolean = false,
    val reduceMotionEnabled: Boolean = false
)
