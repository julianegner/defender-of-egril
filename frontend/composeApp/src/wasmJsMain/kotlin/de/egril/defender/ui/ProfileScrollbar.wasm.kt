package de.egril.defender.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable

/**
 * WASM implementation of ProfileTabScrollbar.
 * Native scrolling is used on WASM, so this is a no-op.
 */
@Composable
actual fun BoxScope.ProfileTabScrollbar(scrollState: ScrollState) {
    // No-op: WASM uses native scroll behavior
}
