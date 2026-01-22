package de.egril.defender.ui.infopage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Impressum wrapper that shows/hides the impressum content
 * Platform-specific implementation
 */
@Composable
expect fun ImpressumWrapper(rowModifier: Modifier = Modifier)
