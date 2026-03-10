package de.egril.defender.analytics

import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSProcessInfo
import platform.Foundation.dataUsingEncoding
import platform.Foundation.create

private const val PLATFORM = "IOS"

private val backendUrl: String
    get() = NSProcessInfo.processInfo.environment["ANALYTICS_BACKEND_URL"] as? String
        ?: "http://localhost:8080"

/**
 * Fire-and-forget HTTP POST via NSURLSession.
 * Errors are silently swallowed so analytics never disrupts gameplay.
 */
actual fun reportEvent(eventType: String, levelName: String?) {
    val json = buildEventJson(eventType, levelName, PLATFORM)
    val url = NSURL.URLWithString("$backendUrl/api/events") ?: return
    val request = NSMutableURLRequest.requestWithURL(url)
    request.HTTPMethod = "POST"
    request.setValue("application/json", forHTTPHeaderField = "Content-Type")
    request.HTTPBody = NSString.create(string = json).dataUsingEncoding(NSUTF8StringEncoding)
    NSURLSession.sharedSession.dataTaskWithRequest(request).resume()
}
