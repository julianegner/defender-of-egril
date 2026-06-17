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

actual fun observeBrowserPathChanges(onPathChanged: (String) -> Unit): () -> Unit = { }

actual fun setInfoPageActive(active: Boolean) {
    // No-op on Android
}

actual fun detectSupportedLanguage(): String = "en"

actual fun setMobileOrientationOverlayMode(mode: MobileOrientationOverlayMode) {
    // No-op on this platform
}

actual fun consumeSpaDeepLinkPath(): String? = null
