package de.egril.defender.utils

interface Platform {
    val name: String
    val isAndroidTV: Boolean
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

val isPlatformWasm = ("Web with Kotlin/Wasm" == getPlatform().name)
val isPlatformAndroid = getPlatform().name.startsWith("Android")
val isPlatformIos = getPlatform().name.startsWith("iOS")
val isPlatformDesktop = getPlatform().name.startsWith("Java")
val isPlatformMobile = isPlatformAndroid || isPlatformIos
