package de.egril.defender.utils

import platform.Foundation.NSLocale
import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isAndroidTV: Boolean = false
    override val isSteamDeckGamingMode: Boolean = false
    override val platformExtended: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val osName: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getSystemLanguageCode(): String? =
    try {
        NSLocale.currentLocale.languageCode?.lowercase()
    } catch (e: Exception) {
        null
    }

actual fun getCurrentUsername(): String = ""

actual fun reloadApp() {
    // No-op for iOS - restoration is handled by RepositoryManager
}
