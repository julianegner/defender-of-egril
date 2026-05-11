package de.egril.defender.utils

/**
 * Android implementation: Deep linking via web URLs not supported.
 * Intent-based deep linking would be handled separately if needed.
 */
actual fun getCurrentPathname(): String? {
    return null
}

actual fun updateBrowserUrl(path: String) {
    // No-op on Android
}

actual fun setInfoPageActive(active: Boolean) {
    // No-op on Android
}

actual fun detectSupportedLanguage(): String = "en"
