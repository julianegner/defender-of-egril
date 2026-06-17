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

actual fun observeBrowserPathChanges(onPathChanged: (String) -> Unit): () -> Unit = { }

actual fun setInfoPageActive(active: Boolean) {
    // No-op on JVM/Desktop
}

actual fun detectSupportedLanguage(): String = "en"

actual fun setMobileOrientationOverlayMode(mode: MobileOrientationOverlayMode) {
    // No-op on this platform
}
