@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.ui.feedback

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.coroutines.resume

/**
 * Captures the Compose canvas element as a PNG screenshot.
 * The Compose viewport renders into a <canvas> element in the DOM.
 */
actual suspend fun captureScreenshot(): FeedbackAttachment? {
    return try {
        // Compose for Web renders into a canvas element inside the body
        val canvas = document.querySelector("canvas") as? HTMLCanvasElement
        if (canvas != null) {
            val dataUrl = canvas.toDataURL("image/png")
            // dataUrl is "data:image/png;base64,<base64data>"
            val base64 = dataUrl.substringAfter("base64,")
            FeedbackAttachment(
                filename = "screenshot.png",
                mimeType = "image/png",
                base64Content = base64
            )
        } else {
            null
        }
    } catch (e: Exception) {
        println("Screenshot capture failed: ${e.message}")
        null
    }
}

/**
 * Opens a browser file input dialog for selecting images, text, and JSON files.
 */
actual suspend fun pickFeedbackFiles(): List<FeedbackAttachment> = suspendCancellableCoroutine { continuation ->
    try {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.multiple = true
        input.accept = "image/png,image/jpeg,image/gif,text/plain,application/json"
        input.style.display = "none"

        input.onchange = {
            val files = input.files
            if (files != null && files.length > 0) {
                val attachments = mutableListOf<FeedbackAttachment>()
                var processed = 0
                val total = files.length

                for (i in 0 until total) {
                    val file = files.item(i)
                    if (file != null) {
                        val filename = file.name
                        val mimeType = file.type.ifBlank {
                            guessMimeType(filename)
                        }
                        val reader = FileReader()
                        reader.onload = {
                            try {
                                val dataUrl = reader.result?.toString() ?: ""
                                val base64 = if (dataUrl.contains("base64,")) {
                                    dataUrl.substringAfter("base64,")
                                } else {
                                    ""
                                }
                                if (base64.isNotEmpty()) {
                                    attachments.add(FeedbackAttachment(filename, mimeType, base64))
                                }
                            } catch (e: Exception) {
                                println("Error reading file $filename: ${e.message}")
                            }
                            processed++
                            if (processed == total) {
                                document.body?.removeChild(input)
                                continuation.resume(attachments)
                            }
                        }
                        reader.onerror = {
                            processed++
                            if (processed == total) {
                                document.body?.removeChild(input)
                                continuation.resume(attachments)
                            }
                        }
                        reader.readAsDataURL(file)
                    } else {
                        processed++
                        if (processed == total) {
                            document.body?.removeChild(input)
                            continuation.resume(attachments)
                        }
                    }
                }
            } else {
                document.body?.removeChild(input)
                continuation.resume(emptyList())
            }
        }

        input.oncancel = {
            document.body?.removeChild(input)
            continuation.resume(emptyList())
        }

        document.body?.appendChild(input)
        input.click()
    } catch (e: Exception) {
        println("File pick failed: ${e.message}")
        continuation.resume(emptyList())
    }
}

private fun guessMimeType(filename: String): String {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "json" -> "application/json"
        "txt" -> "text/plain"
        else -> "application/octet-stream"
    }
}
