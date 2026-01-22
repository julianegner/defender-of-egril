package de.egril.defender.ui.infopage

/**
 * Opens a URL in the browser - Desktop implementation (no-op).
 */
actual fun openUrl(url: String) {
    // No-op on desktop - impressum won't be shown anyway
}
