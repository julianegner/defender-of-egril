package de.egril.defender.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Platform-specific vertical scrollbar for the player profile tab content.
 * On desktop: Shows an interactive vertical scrollbar.
 * On other platforms: No-op (native scrolling is used).
 */
@Composable
expect fun BoxScope.ProfileTabScrollbar(scrollState: ScrollState)
