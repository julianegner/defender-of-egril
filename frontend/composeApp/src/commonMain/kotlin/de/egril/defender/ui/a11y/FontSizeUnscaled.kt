package de.egril.defender.ui.a11y

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import de.egril.defender.ui.settings.AppSettings

/**
 * Wraps content so that the accessibility font size scaling is not applied.
 * Use this for components that should retain their original text size
 * (e.g., game title, level header, defender buttons).
 */
@Composable
fun FontSizeUnscaled(content: @Composable () -> Unit) {
    val currentDensity = LocalDensity.current
    val fontSizeScale = AppSettings.fontSize.value.scale
    if (fontSizeScale != 1.0f) {
        val unscaledDensity = Density(
            density = currentDensity.density,
            fontScale = currentDensity.fontScale / fontSizeScale
        )
        CompositionLocalProvider(LocalDensity provides unscaledDensity) {
            content()
        }
    } else {
        content()
    }
}
