package de.egril.defender.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.egril.defender.utils.getPlatform

/**
 * Applies Android TV-specific modifiers for better accessibility and focus indication.
 * 
 * This function adds:
 * - A yellow border when the element is selected on Android TV devices
 * - Semantic content description for screen readers
 * - Focusable modifier for D-pad navigation support
 *
 * @param baseModifier The base modifier to apply before Android TV-specific modifiers
 * @param isSelected Whether the element is currently selected
 * @param description The content description for accessibility/screen readers
 * @return A Modifier with Android TV accessibility enhancements applied
 */
@Composable
fun Modifier.androidTVModifier(
    baseModifier: Modifier = Modifier,
    isSelected: Boolean,
    description: String
): Modifier {
    val isAndroidTV = remember { getPlatform().isAndroidTV }
    
    return baseModifier
        .then(this)
        .then(
            if (isAndroidTV && isSelected) {
                Modifier.border(4.dp, Color.Yellow, MaterialTheme.shapes.small)
            } else {
                Modifier
            }
        )
        .semantics {
            contentDescription = description
        }
        .focusable()
}
