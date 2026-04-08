package de.egril.defender.utils

import java.util.Locale

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isAndroidTV: Boolean = false
    override val platformExtended: String = run {
        val osName = System.getProperty("os.name") ?: "Unknown"
        val osVersion = System.getProperty("os.version") ?: ""
        if (osVersion.isBlank()) osName else "$osName $osVersion"
    }
}

actual fun getPlatform(): Platform = JVMPlatform()

actual fun getSystemLanguageCode(): String? {
    return try {
        Locale.getDefault().language.lowercase()
    } catch (e: Exception) {
        null
    }
}

actual fun getCurrentUsername(): String {
    return try {
        System.getProperty("user.name") ?: ""
    } catch (e: Exception) {
        ""
    }
}
