package de.egril.defender.utils

import kotlinx.browser.window

/**
 * WASM implementation: Extract current URL pathname from browser.
 */
actual fun getCurrentPathname(): String? {
    return try {
        window.location.pathname
    } catch (e: Exception) {
        null
    }
}
