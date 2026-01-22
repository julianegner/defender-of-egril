package de.egril.defender.ui.infopage

/**
 * Opens a URL in the browser - iOS implementation (no-op).
 */
actual fun openUrl(url: String) {
    // No-op on iOS - impressum won't be shown anyway
}
