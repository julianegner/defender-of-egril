package de.egril.defender.utils

import com.hyperether.resources.AppLocale
import de.egril.defender.ui.infopage.InfoTab

/**
 * Represents a deep link parsed from a URL.
 * Supports data-privacy, info-page, and tutorial routes on web platform.
 */
sealed class DeepLink {
    data class DataPrivacy(val language: AppLocale) : DeepLink()
    data class InfoPage(val tab: InfoTab) : DeepLink()
    object Tutorial : DeepLink()
    object None : DeepLink()
}

/**
 * Platform-specific function to get the current URL pathname.
 * On WASM/Web: returns window.location.pathname
 * On other platforms: returns null
 */
expect fun getCurrentPathname(): String?

/**
 * Updates the browser URL via history.pushState (WASM/Web only).
 * No-op on other platforms.
 */
expect fun updateBrowserUrl(path: String)

/**
 * Adds or removes a CSS class on <body> to suppress the portrait-rotation overlay
 * while an info page is shown (WASM/Web only).
 * No-op on other platforms.
 */
expect fun setInfoPageActive(active: Boolean)

/**
 * Maps an InfoTab to its URL slug.
 */
fun InfoTab.toUrlSlug(): String = when (this) {
    InfoTab.INSTALLATION -> "installation"
    InfoTab.HOW_TO_PLAY -> "how-to-play"
    InfoTab.AUDIO_LICENSES -> "audio-licenses"
    InfoTab.LICENSE -> "license"
    InfoTab.KEYBOARD_SHORTCUTS -> "keyboard-shortcuts"
    InfoTab.BACKEND -> "backend"
    InfoTab.EDITOR_HOWTO -> "editor-howto"
    InfoTab.DOWNLOAD -> "download"
}

/**
 * Parses a URL slug back into an InfoTab, or returns null for unknown slugs.
 * "data-privacy" is accepted as an alias for the backend/account-privacy tab.
 */
fun infoTabFromSlug(slug: String): InfoTab? = when (slug.lowercase()) {
    "installation" -> InfoTab.INSTALLATION
    "how-to-play" -> InfoTab.HOW_TO_PLAY
    "audio-licenses" -> InfoTab.AUDIO_LICENSES
    "license" -> InfoTab.LICENSE
    "keyboard-shortcuts" -> InfoTab.KEYBOARD_SHORTCUTS
    "backend", "data-privacy" -> InfoTab.BACKEND
    "editor-howto" -> InfoTab.EDITOR_HOWTO
    "download" -> InfoTab.DOWNLOAD
    else -> null
}

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
 * Supported formats:
 *   /data-privacy/{language}   → DataPrivacy deep link
 *   /info/{tab-slug}           → InfoPage deep link (e.g. /info/installation)
 *   /info                      → InfoPage deep link (defaults to INSTALLATION tab)
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
    } else if (trimmedPath.startsWith("data-privacy")) {
            val lang = detectSupportedLanguage()
            val locale = parseLanguageFromCode(lang)
            if (locale != null) {
                return DeepLink.DataPrivacy(locale)
            }
    }

    // Check if it's the tutorial deep link
    if (trimmedPath == "tutorial") {
        return DeepLink.Tutorial
    }

    // Check if it's an info page route: /info or /info/{tab-slug}
    if (trimmedPath == "info") {
        return DeepLink.InfoPage(InfoTab.INSTALLATION)
    }
    if (trimmedPath.startsWith("info/")) {
        val parts = trimmedPath.split("/")
        if (parts.size >= 2) {
            val tab = infoTabFromSlug(parts[1])
            if (tab != null) {
                return DeepLink.InfoPage(tab)
            }
        }
        // Unknown tab slug — still navigate to info, defaulting to INSTALLATION
        return DeepLink.InfoPage(InfoTab.INSTALLATION)
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
