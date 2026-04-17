package de.egril.defender.ui

import kotlinx.browser.window

actual fun isEditorAvailable(): Boolean = false

actual fun getGameplayUIScale(): Float = 1.0f

actual fun exitApplication() {
    // On web, close the window (may not work depending on browser security)
    window.close()
}
