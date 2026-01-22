package de.egril.defender.ui.infopage

/**
 * Opens a URL in the browser - WASM implementation.
 */
actual fun openUrl(url: String) {
    kotlinx.browser.window.open(url, "_blank")
}
