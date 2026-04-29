package de.egril.defender.utils

/**
 * iOS implementation: Deep linking via web URLs not supported.
 * Universal Links would be handled separately if needed.
 */
actual fun getCurrentPathname(): String? {
    return null
}

actual fun detectSupportedLanguage(): String = "en"
