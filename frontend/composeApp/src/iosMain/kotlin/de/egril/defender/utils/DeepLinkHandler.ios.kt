package de.egril.defender.utils

/**
 * iOS implementation: Deep linking via web URLs not supported.
 * Universal Links would be handled separately if needed.
 */
actual fun getCurrentPathname(): String? {
    return null
}

actual fun updateBrowserUrl(path: String) {
    // No-op on iOS
}

actual fun setInfoPageActive(active: Boolean) {
    // No-op on iOS
}

actual fun detectSupportedLanguage(): String = "en"

actual fun setMobileOrientationOverlayMode(mode: MobileOrientationOverlayMode) {
    // No-op on this platform
}
