@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.editor

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun pickBackgroundImageBytes(): ByteArray? =
    suspendCancellableCoroutine { continuation ->
        try {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.multiple = false
            input.accept = "image/png,image/jpeg"
            input.style.display = "none"

            input.onchange = {
                val files = input.files
                val file = if (files != null && files.length > 0) files.item(0) else null
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = {
                        try {
                            val dataUrl = reader.result?.toString() ?: ""
                            val base64 =
                                if (dataUrl.contains("base64,")) {
                                    dataUrl.substringAfter("base64,")
                                } else {
                                    ""
                                }
                            document.body?.removeChild(input)
                            if (base64.isNotEmpty()) {
                                continuation.resume(Base64.decode(base64))
                            } else {
                                continuation.resume(null)
                            }
                        } catch (e: Exception) {
                            println("Error reading background image: ${e.message}")
                            continuation.resume(null)
                        }
                    }
                    reader.onerror = {
                        document.body?.removeChild(input)
                        continuation.resume(null)
                    }
                    reader.readAsDataURL(file)
                } else {
                    document.body?.removeChild(input)
                    continuation.resume(null)
                }
            }

            document.body?.appendChild(input)
            input.click()
        } catch (e: Exception) {
            println("Background image picker failed: ${e.message}")
            continuation.resume(null)
        }
    }
