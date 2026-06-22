package de.egril.defender.utils

import com.hyperether.resources.AppLocale
import de.egril.defender.ui.infopage.InfoTab

/**
 * Represents a deep link parsed from a URL.
 * Supports data-privacy, info-page, tutorial, demo, and settings routes on web platform.
 */
sealed class DeepLink {
    data class DataPrivacy(val language: AppLocale) : DeepLink()
    data class InfoPage(val tab: InfoTab) : DeepLink()
    object Tutorial : DeepLink()
    object Demo : DeepLink()
    object Settings : DeepLink()
    object None : DeepLink()
}

enum class MobileOrientationOverlayMode {
    NONE,
    LANDSCAPE_REQUIRED,
    PORTRAIT_REQUIRED
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
 * Registers a browser history listener (WASM/Web only).
 * Invokes [onPathChanged] when browser back/forward navigation changes the path.
 * Returns an unsubscribe function.
 */
expect fun observeBrowserPathChanges(onPathChanged: (String) -> Unit): () -> Unit

/**
 * Legacy helper kept for compatibility with existing call sites.
 * Internally maps to [setMobileOrientationOverlayMode] on WASM/Web.
 * No-op on other platforms.
 */
expect fun setInfoPageActive(active: Boolean)

/**
 * Controls the mobile orientation overlay mode (WASM/Web only).
 * No-op on other platforms.
 */
expect fun setMobileOrientationOverlayMode(mode: MobileOrientationOverlayMode)

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
    InfoTab.FEEDBACK -> "feedback"
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
    "feedback" -> InfoTab.FEEDBACK
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
 *   /download                  → InfoPage deep link (DOWNLOAD tab, shortcut for /info/download)
 *   /demo                      → Start demo mode
 *   /settings                  → Open main menu with settings dialog
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

    // Check if it's the demo deep link
    if (trimmedPath == "demo") {
        return DeepLink.Demo
    }

    // Check if it's the settings deep link
    if (trimmedPath == "settings") {
        return DeepLink.Settings
    }

    // Check if it's the direct download deep link
    if (trimmedPath == "download") {
        return DeepLink.InfoPage(InfoTab.DOWNLOAD)
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
 * Reads and clears the SPA deep link path stored by root index.html when the app
 * was reached via a /data-privacy/{lang} server-side redirect.
 * On WASM/Web: reads and clears window._spaDeepLinkPath.
 * On all other platforms: always returns null.
 */
expect fun consumeSpaDeepLinkPath(): String?

/**
 * Extracts and returns the current deep link, if any.
 * Checks the SPA deep link path stored by index.html first (used when navigating
 * from a /data-privacy/{lang} URL via the sessionStorage redirect mechanism), then falls
 * back to the current browser URL.
 * Only processes deep links on platforms that support URL routing (WASM/Web).
 */
fun checkCurrentDeepLink(): DeepLink {
    val pathname = consumeSpaDeepLinkPath() ?: getCurrentPathname() ?: return DeepLink.None
    return parseDeepLink(pathname)
}

expect fun detectSupportedLanguage(): String
