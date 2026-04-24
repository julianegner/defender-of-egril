@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.ui

import kotlinx.browser.window

actual fun isEditorAvailable(): Boolean = false

/**
 * Detects if the browser is running on a mobile device by checking the user agent string.
 * Returns true for smartphones (Android phones, iPhones, etc.) to apply mobile UI scaling.
 */
@JsFun("""() => {
    try {
        return /Mobi|Android|iPhone|iPod|webOS|Opera Mini/i.test(navigator.userAgent);
    } catch (e) { return false; }
}""")
private external fun isMobileBrowser(): Boolean

actual fun getGameplayUIScale(): Float = if (isMobileBrowser()) 0.5f else 1.0f

actual fun isMobileWebBrowser(): Boolean = isMobileBrowser()

actual fun exitApplication() {
    // On web, close the window (may not work depending on browser security)
    window.close()
}
