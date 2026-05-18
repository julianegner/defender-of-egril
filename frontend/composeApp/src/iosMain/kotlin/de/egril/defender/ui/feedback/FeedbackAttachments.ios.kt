package de.egril.defender.ui.feedback

/**
 * iOS implementation of screenshot capture and file picking.
 *
 * Screenshot: Not available on iOS currently — returns null.
 * Would require UIGraphicsImageRenderer with the UIWindow reference.
 *
 * File picker: Not available on iOS currently — returns empty list.
 * Would require UIDocumentPickerViewController integration.
 * Users can describe the issue in the message text instead.
 */
actual suspend fun captureScreenshot(): FeedbackAttachment? = null

actual suspend fun pickFeedbackFiles(): List<FeedbackAttachment> = emptyList()
