package com.defenderofegril.ui

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
