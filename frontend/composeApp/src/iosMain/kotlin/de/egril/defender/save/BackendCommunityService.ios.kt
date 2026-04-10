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

private val backendUrl: String
    get() = NSProcessInfo.processInfo.environment["ANALYTICS_BACKEND_URL"] as? String
        ?: "http://localhost:8080"

actual object BackendCommunityService {

    actual suspend fun uploadCommunityFile(
        fileType: String,
        fileId: String,
        jsonData: String,
        token: String,
        description: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val url = NSURL.URLWithString("$backendUrl/api/community/files")
        if (url == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        val request = NSMutableURLRequest.requestWithURL(url)
        request.HTTPMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField = "Content-Type")
        request.setValue("Bearer $token", forHTTPHeaderField = "Authorization")
        val body = buildCommunityUploadJson(fileType, fileId, jsonData, description)
        request.HTTPBody = NSString.create(string = body).dataUsingEncoding(NSUTF8StringEncoding)

        NSURLSession.sharedSession.dataTaskWithRequest(request) { _, response, _ ->
            val httpResponse = response as? NSHTTPURLResponse
            continuation.resume(httpResponse?.statusCode?.toInt() in 200..299)
        }.resume()
    }

    actual suspend fun fetchCommunityFileList(fileType: String?): List<CommunityFileInfo>? =
        suspendCancellableCoroutine { continuation ->
            val path = if (fileType != null) {
                "/api/community/files?fileType=$fileType"
            } else {
                "/api/community/files"
            }
            val url = NSURL.URLWithString("$backendUrl$path")
            if (url == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }
            val request = NSMutableURLRequest.requestWithURL(url)
            request.HTTPMethod = "GET"

            NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, _ ->
                val httpResponse = response as? NSHTTPURLResponse
                if (httpResponse?.statusCode?.toInt() !in 200..299 || data == null) {
                    continuation.resume(null)
                    return@dataTaskWithRequest
                }
                @Suppress("CAST_NEVER_SUCCEEDS")
                val json = NSString.create(data as NSData, NSUTF8StringEncoding)?.toString()
                continuation.resume(if (json != null) parseCommunityFileListJson(json) else null)
            }.resume()
        }

    actual suspend fun fetchCommunityFile(
        fileType: String,
        fileId: String
    ): CommunityFileData? = suspendCancellableCoroutine { continuation ->
        val url = NSURL.URLWithString("$backendUrl/api/community/files/$fileType/$fileId")
        if (url == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val request = NSMutableURLRequest.requestWithURL(url)
        request.HTTPMethod = "GET"

        NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, _ ->
            val httpResponse = response as? NSHTTPURLResponse
            if (httpResponse?.statusCode?.toInt() !in 200..299 || data == null) {
                continuation.resume(null)
                return@dataTaskWithRequest
            }
            @Suppress("CAST_NEVER_SUCCEEDS")
            val json = NSString.create(data as NSData, NSUTF8StringEncoding)?.toString()
            continuation.resume(if (json != null) parseCommunityFileDataJson(json) else null)
        }.resume()
    }

    actual suspend fun fetchCommunityMapImage(mapId: String): ByteArray? {
        // Image generation falls back to local generation on iOS
        return null
    }
}
