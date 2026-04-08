package de.egril.defender.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * WASM implementation of IconGridScrollbar.
 * Native scrolling is used on WASM, so this is a no-op.
 */
@Composable
actual fun BoxScope.IconGridScrollbar(gridState: LazyGridState) {
    // No-op: WASM uses native scroll behavior
}
