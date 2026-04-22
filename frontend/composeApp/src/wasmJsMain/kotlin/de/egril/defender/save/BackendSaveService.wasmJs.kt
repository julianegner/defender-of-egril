@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import de.egril.defender.analytics.backendUrl
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume

actual object BackendSaveService {

    actual suspend fun uploadSavefile(saveId: String, jsonData: String, token: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            try {
                val xhr = XMLHttpRequest()
                xhr.open("POST", "$backendUrl/api/savefiles", async = true)
                xhr.setRequestHeader("Content-Type", "application/json")
                xhr.setRequestHeader("Authorization", "Bearer $token")
                xhr.onload = {
                    continuation.resume(xhr.status.toInt() in 200..299)
                }
                xhr.onerror = {
                    continuation.resume(false)
                }
                xhr.send(buildUploadJson(saveId, jsonData))
            } catch (_: Exception) {
                continuation.resume(false)
            }
        }

    actual suspend fun fetchSavefiles(token: String): List<RemoteSavefileInfo>? =
        suspendCancellableCoroutine { continuation ->
            try {
                val xhr = XMLHttpRequest()
                xhr.open("GET", "/api/savefiles", async = true)
                xhr.setRequestHeader("Authorization", "Bearer $token")
                xhr.onload = {
                    if (xhr.status.toInt() in 200..299) {
                        val text = xhr.responseText
                        continuation.resume(parseRemoteSavefilesJson(text))
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
