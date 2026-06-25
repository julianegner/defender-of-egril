package de.egril.defender.ui.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.settings.FontSize

/**
 * Wraps content so that the font size is always the maximum (HUGE) scale,
 * regardless of the user's accessibility font size setting.
 * Use this for accessibility-critical UI that must always be maximally readable,
 * e.g. the AccessibilityBanner shown on first run.
 */
@Composable
fun FontSizeMaximum(content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    val currentScale = AppSettings.fontSize.value.scale
    val maxScale = FontSize.HUGE.scale
    if (currentScale != maxScale) {
        val scaledDensity =
            Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale / currentScale * maxScale,
            )
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
            content()
        }
    } else {
        content()
    }
}
