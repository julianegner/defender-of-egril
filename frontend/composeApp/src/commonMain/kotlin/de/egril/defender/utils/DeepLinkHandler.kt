package de.egril.defender.utils

import com.hyperether.resources.AppLocale

/**
 * Represents a deep link parsed from a URL.
 * Currently supports data-privacy routes on web platform.
 */
sealed class DeepLink {
    data class DataPrivacy(val language: AppLocale) : DeepLink()
    object None : DeepLink()
}

/**
 * Platform-specific function to get the current URL pathname.
 * On WASM/Web: returns window.location.pathname
 * On other platforms: returns null
 */
expect fun getCurrentPathname(): String?

/**
 * Parses and validates language codes for deep links.
 * Supports: en, de, fr, es, it (matching AppLocale values)
 */
fun parseLanguageFromCode(code: String?): AppLocale? {
    return when (code?.lowercase()) {
        "en" -> AppLocale.DEFAULT
        "de" -> AppLocale.DE
        "fr" -> AppLocale.FR
        "es" -> AppLocale.ES
        "it" -> AppLocale.IT
        else -> AppLocale.DEFAULT
    }
}

/**
 * Parses a URL path into a DeepLink.
 * Expected format: /data-privacy/{language}
 * Example: /data-privacy/en, /data-privacy/de, /data-privacy/fr
 *
 * @param path The URL path to parse (e.g., from window.location.pathname)
 * @return The parsed DeepLink, or DeepLink.None if not a recognized route
 */
fun parseDeepLink(path: String): DeepLink {

    println("Parsing deep link from path: $path")

    val trimmedPath = path.trim('/').lowercase()

    // Check if it's a data-privacy route
    if (trimmedPath.startsWith("data-privacy/")) {
        val parts = trimmedPath.split("/")
        if (parts.size >= 2) {
            val languageCode = parts[1]
            val locale = parseLanguageFromCode(languageCode)
            if (locale != null) {
                return DeepLink.DataPrivacy(locale)
            }
        }
    } else
        if (trimmedPath.startsWith("data-privacy")) {
            val lang = detectSupportedLanguage()
            val locale = parseLanguageFromCode(lang)
            if (locale != null) {
                return DeepLink.DataPrivacy(locale)
            }
        }

    return DeepLink.None
}

/**
 * Extracts and returns the current deep link, if any.
 * Only processes deep links on platforms that support URL routing (WASM/Web).
 */
fun checkCurrentDeepLink(): DeepLink {
    val pathname = getCurrentPathname() ?: return DeepLink.None
    return parseDeepLink(pathname)
}

expect fun detectSupportedLanguage(): String
