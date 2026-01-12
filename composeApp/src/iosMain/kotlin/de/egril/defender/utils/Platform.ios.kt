package de.egril.defender.utils

import platform.UIKit.UIDevice
import platform.Foundation.NSLocale

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun getSystemLanguageCode(): String? {
    return try {
        NSLocale.currentLocale.languageCode?.lowercase()
    } catch (e: Exception) {
        null
    }
}
