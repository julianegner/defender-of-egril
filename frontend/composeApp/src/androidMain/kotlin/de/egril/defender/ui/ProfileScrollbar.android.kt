package de.egril.defender.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Android implementation of ProfileTabScrollbar.
 * Native scrolling is used on Android, so this is a no-op.
 */
@Composable
actual fun BoxScope.ProfileTabScrollbar(scrollState: ScrollState) {
    // No-op: Android uses native scroll behavior
}
