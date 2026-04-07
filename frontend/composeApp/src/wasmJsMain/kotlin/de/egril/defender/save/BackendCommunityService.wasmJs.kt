@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume

actual object BackendCommunityService {

    actual suspend fun uploadCommunityFile(
        fileType: String,
        fileId: String,
        jsonData: String,
        token: String
    ): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            val xhr = XMLHttpRequest()
            xhr.open("POST", "/api/community/files", async = true)
            xhr.setRequestHeader("Content-Type", "application/json")
            xhr.setRequestHeader("Authorization", "Bearer $token")
            xhr.onload = {
                continuation.resume(xhr.status.toInt() in 200..299)
            }
            xhr.onerror = {
                continuation.resume(false)
            }
            xhr.send(buildCommunityUploadJson(fileType, fileId, jsonData))
        } catch (_: Exception) {
            continuation.resume(false)
        }
    }

    actual suspend fun fetchCommunityFileList(fileType: String?): List<CommunityFileInfo>? =
        suspendCancellableCoroutine { continuation ->
            try {
                val path = if (fileType != null) {
                    "/api/community/files?fileType=$fileType"
                } else {
                    "/api/community/files"
                }
                val xhr = XMLHttpRequest()
                xhr.open("GET", path, async = true)
                xhr.onload = {
                    if (xhr.status.toInt() in 200..299) {
                        continuation.resume(parseCommunityFileListJson(xhr.responseText))
                    } else {
                        continuation.resume(null)
                    }
                }
                xhr.onerror = {
                    continuation.resume(null)
                }
                xhr.send()
            } catch (_: Exception) {
                continuation.resume(null)
            }
        }

    actual suspend fun fetchCommunityFile(
        fileType: String,
        fileId: String
    ): CommunityFileData? = suspendCancellableCoroutine { continuation ->
        try {
            val xhr = XMLHttpRequest()
            xhr.open("GET", "/api/community/files/$fileType/$fileId", async = true)
            xhr.onload = {
                if (xhr.status.toInt() in 200..299) {
                    continuation.resume(parseCommunityFileDataJson(xhr.responseText))
                } else {
                    continuation.resume(null)
                }
            }
            xhr.onerror = {
                continuation.resume(null)
            }
            xhr.send()
        } catch (_: Exception) {
            continuation.resume(null)
        }
    }
}
