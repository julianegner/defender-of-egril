package de.egril.defender.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * Android implementation of IconGridScrollbar.
 * Native scrolling is used on Android, so this is a no-op.
 */
@Composable
actual fun BoxScope.IconGridScrollbar(gridState: LazyGridState) {
    // No-op: Android uses native scroll behavior
}
