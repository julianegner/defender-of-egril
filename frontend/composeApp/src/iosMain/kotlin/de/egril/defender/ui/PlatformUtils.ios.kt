package de.egril.defender.ui

import platform.Foundation.NSLog

actual fun isEditorAvailable(): Boolean = false

actual fun getGameplayUIScale(): Float = 0.5f

actual fun isMobileWebBrowser(): Boolean = false

actual fun exitApplication() {
    // On iOS, apps should not exit programmatically - this is handled by the system
    // Log a message for debugging purposes
    NSLog("Exit application requested - not supported on iOS")
}
