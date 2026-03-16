@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.xhr.XMLHttpRequest
import kotlin.coroutines.resume

actual object BackendUserDataService {

    actual suspend fun uploadUserData(jsonData: String, token: String): Boolean =
        suspendCancellableCoroutine { continuation ->
            try {
                val xhr = XMLHttpRequest()
                xhr.open("POST", "/api/userdata", async = true)
                xhr.setRequestHeader("Content-Type", "application/json")
                xhr.setRequestHeader("Authorization", "Bearer $token")
                xhr.onload = {
                    continuation.resume(xhr.status.toInt() in 200..299)
                }
                xhr.onerror = {
                    continuation.resume(false)
                }
                xhr.send(buildUserDataUploadJson(jsonData))
            } catch (_: Exception) {
                continuation.resume(false)
            }
        }

    actual suspend fun fetchUserData(token: String): RemoteUserData? =
        suspendCancellableCoroutine { continuation ->
            try {
                val xhr = XMLHttpRequest()
                xhr.open("GET", "/api/userdata", async = true)
                xhr.setRequestHeader("Authorization", "Bearer $token")
                xhr.onload = {
                    if (xhr.status.toInt() in 200..299) {
                        continuation.resume(parseRemoteUserDataJson(xhr.responseText))
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
