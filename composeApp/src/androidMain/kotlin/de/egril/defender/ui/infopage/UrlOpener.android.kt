package de.egril.defender.ui.infopage

/**
 * Opens a URL in the browser - Android implementation (no-op).
 */
actual fun openUrl(url: String) {
    // No-op on Android - impressum won't be shown anyway
}
