package de.egril.defender.utils

interface Platform {
    val name: String
    val isAndroidTV: Boolean
    /**
     * `true` when the app is running on a Steam Deck in **gaming mode** (gamescope session).
     * `false` on all other platforms and on a Steam Deck in desktop mode (KDE Plasma).
     */
    val isSteamDeckGamingMode: Boolean
    /**
     * Extended platform detail string.
     * Desktop: OS name and version (e.g. "Windows 11 10.0").
     * Android: friendly OS release (e.g. "Android 13").
     * iOS: system name and version (e.g. "iOS 17.0").
     * Web: browser name and version (e.g. "Chrome/120.0.6099.130").
     */
    val platformExtended: String
    /**
     * Human-readable operating system name and version.
     * Desktop Linux: pretty name from /etc/os-release (e.g. "Ubuntu 22.04.5 LTS").
     * Desktop Windows/macOS: OS name and version (e.g. "Windows 10", "Mac OS X 14.3.1").
     * Android: OS release version (e.g. "Android 14").
     * iOS: system name and version (e.g. "iOS 17.0").
     * Web: OS extracted from browser user agent (e.g. "Android 14", "Windows 10/11", "macOS 14.3.1").
     * Returns null if the OS cannot be determined.
     */
    val osName: String?
}

expect fun getPlatform(): Platform

/**
 * Get the system's default language code (e.g., "en", "de", "es", "fr", "it")
 * Returns null if the language cannot be determined
 */
expect fun getSystemLanguageCode(): String?

/**
 * Get the current OS user name (e.g., "alice").
 * Returns an empty string if the user name cannot be determined.
 */
expect fun getCurrentUsername(): String

val isPlatformWasm = getPlatform().name.startsWith("Web with Kotlin/Wasm")
val isPlatformAndroid = getPlatform().name.startsWith("Android")
val isPlatformIos = getPlatform().name.startsWith("iOS")
val isPlatformDesktop = getPlatform().name.startsWith("Java")
val isPlatformMobile = isPlatformAndroid || isPlatformIos

/**
 * `true` when the app is running on a device with limited input capabilities where a
 * browser-based OAuth2 Authorization Code flow is impractical.
 *
 * Currently covers:
 * - Steam Deck in **gaming mode** (gamescope session, no accessible desktop browser)
 * - Android TV devices (leanback UI, typically no keyboard/browser)
 *
 * On such devices the Device Authorization Grant (RFC 8628) is used instead of the
 * standard Authorization Code Grant with PKCE.
 */
val isLimitedInputDevice: Boolean
    get() = getPlatform().isAndroidTV || getPlatform().isSteamDeckGamingMode

/**
 * Returns a short platform identifier suitable for backend API calls and analytics.
 * Maps platform names to the canonical string values used throughout the backend.
 */
fun getClientPlatformName(): String = when {
    isPlatformWasm -> "WEB"
    isPlatformAndroid -> "ANDROID"
    isPlatformIos -> "IOS"
    isPlatformDesktop -> "DESKTOP"
    else -> "UNKNOWN"
}
