@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
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

actual fun detectSupportedLanguage(): String {
    val supported = setOf("en", "de", "fr", "es", "it")
    val browserLangs = window.navigator.languages
    val lang = (0 until browserLangs.length)
        .mapNotNull { idx ->
            val value = browserLangs[idx]
            if (value != null) value.toString() else null
        }
        .map { it.substringBefore('-').lowercase() }
        .firstOrNull { it in supported }
    return lang ?: "en"
}
