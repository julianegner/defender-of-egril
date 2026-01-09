package de.egril.defender.utils

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val isAndroidTV: Boolean = false
}

actual fun getPlatform(): Platform = IOSPlatform()
