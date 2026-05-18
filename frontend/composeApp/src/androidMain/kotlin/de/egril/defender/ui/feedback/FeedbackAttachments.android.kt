package de.egril.defender.ui.feedback

/**
 * Android implementation of screenshot capture and file picking.
 *
 * Screenshot: Not available on Android currently — returns null.
 * The View-based screenshot approach requires a Window reference that isn't
 * easily accessible from the Compose Multiplatform layer without tight Activity coupling.
 *
 * File picker: Not available on Android currently — returns empty list.
 * Would require registering ActivityResultLauncher in MainActivity.onCreate().
 * Users can describe the issue in the message text instead.
 */
actual suspend fun captureScreenshot(): FeedbackAttachment? = null

actual suspend fun pickFeedbackFiles(): List<FeedbackAttachment> = emptyList()
