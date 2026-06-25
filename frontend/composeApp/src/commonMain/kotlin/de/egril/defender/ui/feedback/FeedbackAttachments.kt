package de.egril.defender.ui.feedback

/**
 * Represents a file attachment for the feedback form.
 * Can be an auto-captured screenshot or a user-selected file.
 */
data class FeedbackAttachment(
    val filename: String,
    val mimeType: String,
    val base64Content: String,
)

/**
 * Platform-specific file picker for feedback attachments.
 * Opens a native file chooser allowing the user to select images, text, or JSON files.
 *
 * @return list of selected attachments, or empty if user cancelled
 */
expect suspend fun pickFeedbackFiles(): List<FeedbackAttachment>

/**
 * Platform-specific screenshot capture.
 * Captures the current screen content as a PNG image.
 * Must be called BEFORE the feedback dialog is shown.
 *
 * On platforms where screen capture is not easily available, returns null.
 *
 * @return attachment containing the PNG screenshot, or null if capture is unavailable
 */
expect suspend fun captureScreenshot(): FeedbackAttachment?
