@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.utils

import kotlinx.browser.document
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

/**
 * WASM implementation: Push a new path into the browser history without reloading.
 */
actual fun updateBrowserUrl(path: String) {
    try {
        window.history.pushState(null, "", path)
    } catch (e: Exception) {
        // Ignore – e.g. when running in a sandboxed iframe
    }
}

/**
 * WASM implementation: Add/remove the "info-page-active" class on <body> so that
 * the CSS portrait-rotation overlay is suppressed while an info page is shown.
 */
actual fun setInfoPageActive(active: Boolean) {
    try {
        val body = document.body ?: return
        if (active) {
            body.classList.add("info-page-active")
        } else {
            body.classList.remove("info-page-active")
        }
    } catch (e: Exception) {
        // Ignore
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
