package de.egril.defender.utils

interface Platform {
    val name: String
    val isAndroidTV: Boolean
    /**
     * Extended platform detail string.
     * Desktop: OS name and version (e.g. "Windows 11 10.0").
     * Android: friendly OS release (e.g. "Android 13").
     * iOS: system name and version (e.g. "iOS 17.0").
     * Web: browser name and version (e.g. "Chrome/120.0.6099.130").
     */
    val platformExtended: String
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
