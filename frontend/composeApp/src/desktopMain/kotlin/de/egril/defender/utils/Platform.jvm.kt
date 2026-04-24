package de.egril.defender.utils

import java.io.File
import java.util.Locale

/**
 * Returns `true` when the JVM is running on a Steam Deck in gaming mode (gamescope session).
 *
 * Detection logic:
 * 1. Verify the OS is Linux (Steam Deck only runs Linux).
 * 2. Read `/etc/os-release` and check for SteamOS/Steam Deck markers.
 * 3. Distinguish gaming mode from desktop mode via `XDG_CURRENT_DESKTOP`:
 *    - Gaming mode: variable is absent or set to "gamescope" (gamescope Wayland compositor).
 *    - Desktop mode: variable is "KDE" (KDE Plasma desktop).
 */
private fun detectSteamDeckGamingMode(): Boolean {
    val os = System.getProperty("os.name", "").lowercase()
    if (!os.startsWith("linux")) return false

    val isSteamDeck = try {
        File("/etc/os-release").readLines().any { line ->
            line.equals("ID=steamos", ignoreCase = true) ||
                line.startsWith("ID=steamos ", ignoreCase = true) ||
                line.startsWith("ID=\"steamos\"", ignoreCase = true) ||
                line.equals("VARIANT_ID=steamdeck", ignoreCase = true) ||
                line.startsWith("VARIANT_ID=steamdeck ", ignoreCase = true) ||
                line.startsWith("VARIANT_ID=\"steamdeck\"", ignoreCase = true)
        }
    } catch (_: Exception) {
        false
    }
    if (!isSteamDeck) return false

    // In gaming mode the session desktop is "gamescope" (or the variable is absent).
    // In desktop mode KDE Plasma sets XDG_CURRENT_DESKTOP=KDE.
    val xdgDesktop = System.getenv("XDG_CURRENT_DESKTOP")?.uppercase() ?: ""
    return xdgDesktop != "KDE"
}

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val isAndroidTV: Boolean = false
    override val isSteamDeckGamingMode: Boolean = detectSteamDeckGamingMode()
    override val platformExtended: String = run {
        val osName = System.getProperty("os.name") ?: "Unknown"
        val osVersion = System.getProperty("os.version") ?: ""
        if (osVersion.isBlank()) osName else "$osName $osVersion"
    }
    override val osName: String? = run {
        val osNameProp = System.getProperty("os.name") ?: return@run null
        if (osNameProp.lowercase().startsWith("linux")) {
            try {
                File("/etc/os-release").useLines { lines ->
                    lines.firstOrNull { it.startsWith("PRETTY_NAME=") }
                        ?.removePrefix("PRETTY_NAME=")
                        ?.trim('"')
                } ?: osNameProp
            } catch (_: Exception) {
                osNameProp
            }
        } else {
            val osVersion = System.getProperty("os.version") ?: ""
            if (osVersion.isBlank()) osNameProp else "$osNameProp $osVersion"
        }
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
