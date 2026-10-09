package de.egril.defender.editor

/**
 * Opens a platform-specific image file picker and returns the selected image as bytes,
 * or null if the user cancelled or the platform does not support it.
 */
expect suspend fun pickBackgroundImageBytes(): ByteArray?
