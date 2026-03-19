package de.egril.defender.save

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
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

private val settingsBackendUrl: String
    get() = NSProcessInfo.processInfo.environment["ANALYTICS_BACKEND_URL"] as? String
        ?: "http://localhost:8080"

actual object BackendSettingsService {

    actual suspend fun uploadSettings(settingsJson: String, token: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            val url = NSURL.URLWithString("$settingsBackendUrl/api/settings")
            if (url == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            val request = NSMutableURLRequest.requestWithURL(url)
            request.HTTPMethod = "POST"
            request.setValue("application/json", forHTTPHeaderField = "Content-Type")
            request.setValue("Bearer $token", forHTTPHeaderField = "Authorization")
            val body = buildSettingsUploadJson(settingsJson)
            request.HTTPBody = NSString.create(string = body).dataUsingEncoding(NSUTF8StringEncoding)

            NSURLSession.sharedSession.dataTaskWithRequest(request) { _, response, _ ->
                val httpResponse = response as? NSHTTPURLResponse
                continuation.resume(httpResponse?.statusCode?.toInt() in 200..299)
            }.resume()
        }

    actual suspend fun fetchSettings(token: String): Map<String, String>? =
        suspendCancellableCoroutine { continuation ->
            val url = NSURL.URLWithString("$settingsBackendUrl/api/settings")
            if (url == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val request = NSMutableURLRequest.requestWithURL(url)
            request.HTTPMethod = "GET"
            request.setValue("Bearer $token", forHTTPHeaderField = "Authorization")

            NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, _ ->
                val httpResponse = response as? NSHTTPURLResponse
                if (httpResponse?.statusCode?.toInt() !in 200..299 || data == null) {
                    continuation.resume(null)
                    return@dataTaskWithRequest
                }
                @Suppress("CAST_NEVER_SUCCEEDS")
                val json = NSString.create(data as NSData, NSUTF8StringEncoding)?.toString()
                if (json == null) {
                    continuation.resume(null)
                } else {
                    continuation.resume(parseSettingsResponseJson(json))
                }
            }.resume()
        }
}
