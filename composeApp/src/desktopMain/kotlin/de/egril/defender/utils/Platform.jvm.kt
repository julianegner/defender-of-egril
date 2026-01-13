package de.egril.defender.utils

import java.util.Locale

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getSystemLanguageCode(): String? {
    return try {
        Locale.getDefault().language.lowercase()
    } catch (e: Exception) {
        null
    }
}
