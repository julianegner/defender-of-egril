package de.egril.defender.utils

/**
 * JVM implementation: Deep linking via web URLs not supported.
 * Universal Links would be handled separately if needed.
 */
actual fun getCurrentPathname(): String? {
    return null
}

actual fun updateBrowserUrl(path: String) {
    // No-op on JVM/Desktop
}

actual fun setInfoPageActive(active: Boolean) {
    // No-op on JVM/Desktop
}

actual fun detectSupportedLanguage(): String = "en"
