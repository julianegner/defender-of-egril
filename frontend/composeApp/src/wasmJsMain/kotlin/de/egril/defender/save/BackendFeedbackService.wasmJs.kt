@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import de.egril.defender.analytics.backendUrl
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume

actual object BackendFeedbackService {
    actual suspend fun submitFeedback(request: FeedbackSubmitRequest, token: String?): Int? =
        suspendCancellableCoroutine { continuation ->
            try {
                val xhr = XMLHttpRequest()
                xhr.open("POST", "$backendUrl/api/feedback", async = true)
                xhr.setRequestHeader("Content-Type", "application/json")
                if (!token.isNullOrBlank()) {
                    xhr.setRequestHeader("Authorization", "Bearer $token")
                }
                xhr.onload = {
                    val status = xhr.status.toInt()
                    continuation.resume(if (status in 200..299) null else status)
                }
                xhr.onerror = {
                    continuation.resume(-1)
                }
                xhr.send(buildFeedbackUploadJson(request))
            } catch (_: Exception) {
                continuation.resume(-1)
            }
        }
}
