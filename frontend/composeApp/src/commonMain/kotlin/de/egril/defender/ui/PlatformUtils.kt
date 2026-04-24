package de.egril.defender.ui

/**
 * Platform-specific flag for editor availability
 */
expect fun isEditorAvailable(): Boolean

/**
 * Platform-specific UI scale factor for the gameplay screen
 * Returns a value less than 1.0 for mobile platforms to zoom out the UI,
 * and 1.0 for desktop platforms.
 */
expect fun getGameplayUIScale(): Float

/**
 * Returns true if the app is running in a web browser on a mobile device.
 * Always returns false on non-WASM platforms.
 */
expect fun isMobileWebBrowser(): Boolean

/**
 * Platform-specific function to exit the application
 */
expect fun exitApplication()
