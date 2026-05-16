package de.egril.defender.save

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import kotlin.coroutines.resume

private val feedbackBackendUrl: String
    get() = NSProcessInfo.processInfo.environment["ANALYTICS_BACKEND_URL"] as? String
        ?: "http://localhost:8080"

actual object BackendFeedbackService {
    actual suspend fun submitFeedback(request: FeedbackSubmitRequest, token: String?): Boolean =
        suspendCancellableCoroutine { continuation ->
            val url = NSURL.URLWithString("$feedbackBackendUrl/api/feedback")
            if (url == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            val httpRequest = NSMutableURLRequest.requestWithURL(url)
            httpRequest.HTTPMethod = "POST"
            httpRequest.setValue("application/json", forHTTPHeaderField = "Content-Type")
            if (!token.isNullOrBlank()) {
                httpRequest.setValue("Bearer $token", forHTTPHeaderField = "Authorization")
            }
            httpRequest.HTTPBody = NSString.create(string = buildFeedbackUploadJson(request))
                .dataUsingEncoding(NSUTF8StringEncoding)

            NSURLSession.sharedSession.dataTaskWithRequest(httpRequest) { _, response, _ ->
                val httpResponse = response as? NSHTTPURLResponse
                continuation.resume(httpResponse?.statusCode?.toInt() in 200..299)
            }.resume()
        }
}
