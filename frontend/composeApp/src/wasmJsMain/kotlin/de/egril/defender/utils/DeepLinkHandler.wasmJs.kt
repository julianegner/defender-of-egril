@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.utils

import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.JsFun

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
 * WASM implementation (legacy helper): map old info-page active flag to the
 * new orientation overlay mode.
 */
actual fun setInfoPageActive(active: Boolean) {
    setMobileOrientationOverlayMode(
        if (active) MobileOrientationOverlayMode.PORTRAIT_REQUIRED else MobileOrientationOverlayMode.NONE
    )
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

@JsFun(
    "(mode) => { if (typeof window !== 'undefined' && typeof window.setOrientationOverlayMode === 'function') { window.setOrientationOverlayMode(mode); } }"
)
private external fun setOrientationOverlayModeJs(mode: String)

actual fun setMobileOrientationOverlayMode(mode: MobileOrientationOverlayMode) {
    val jsMode = when (mode) {
        MobileOrientationOverlayMode.NONE -> "none"
        MobileOrientationOverlayMode.LANDSCAPE_REQUIRED -> "landscape"
        MobileOrientationOverlayMode.PORTRAIT_REQUIRED -> "portrait"
    }
    try {
        setOrientationOverlayModeJs(jsMode)
    } catch (e: Exception) {
        // Ignore
    }
}
