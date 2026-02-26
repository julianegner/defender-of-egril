@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package de.egril.defender.mapgen

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import org.khronos.webgl.Uint8ClampedArray

@JsFun("(arr, idx, value) => { arr[idx] = value; }")
private external fun setByte(arr: Uint8ClampedArray, idx: Int, value: Int)

actual object MapImageEncoder {
    actual fun encodeToPng(pixels: IntArray, width: Int, height: Int): ByteArray? {
        return try {
            val canvas = (document.createElement("canvas") as HTMLCanvasElement).apply {
                this.width = width
                this.height = height
            }
            val ctx = canvas.getContext("2d") as? CanvasRenderingContext2D ?: return null

        val data = Uint8ClampedArray(width * height * 4)
        var p = 0
        for (c in pixels) {
            // Kotlin Int pixel is ARGB; canvas ImageData expects RGBA.
            setByte(data, p++, (c ushr 16) and 0xFF) // R
            setByte(data, p++, (c ushr 8) and 0xFF)  // G
            setByte(data, p++, c and 0xFF)           // B
            setByte(data, p++, (c ushr 24) and 0xFF) // A
        }

            ctx.putImageData(ImageData(data, width), 0.0, 0.0)
            val dataUrl = canvas.toDataURL("image/png") ?: return null
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex < 0) return null
            val base64 = dataUrl.substring(commaIndex + 1)
            val binary = window.atob(base64)
            ByteArray(binary.length) { idx -> binary[idx].code.toByte() }
        } catch (_: Throwable) {
            null
        }
    }
}
