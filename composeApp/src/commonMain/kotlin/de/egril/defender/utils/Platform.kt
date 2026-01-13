package de.egril.defender.utils

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Get the system's default language code (e.g., "en", "de", "es", "fr", "it")
 * Returns null if the language cannot be determined
 */
expect fun getSystemLanguageCode(): String?

val isPlatformWasm = ("Web with Kotlin/Wasm" == getPlatform().name)
val isPlatformAndroid = getPlatform().name.startsWith("Android")
val isPlatformIos = getPlatform().name.startsWith("iOS")
val isPlatformDesktop = getPlatform().name.startsWith("Java")
val isPlatformMobile = isPlatformAndroid || isPlatformIos
