package de.egril.defender.mapgen

import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.ImageData
import org.khronos.webgl.Uint8ClampedArray

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
            for (i in pixels.indices) {
                val c = pixels[i]
                data[p++] = ((c shr 16) and 0xFF).toByte()
                data[p++] = ((c shr 8) and 0xFF).toByte()
                data[p++] = (c and 0xFF).toByte()
                data[p++] = ((c ushr 24) and 0xFF).toByte()
            }

            ctx.putImageData(ImageData(data, width), 0.0, 0.0)
            val dataUrl = canvas.toDataURL("image/png") ?: return null
            val commaIndex = dataUrl.indexOf(',')
            if (commaIndex < 0) return null
            val base64 = dataUrl.substring(commaIndex + 1)
            val binary = window.atob(base64)
            val bytes = ByteArray(binary.length) { idx -> binary[idx].code.toByte() }
            bytes
        } catch (_: Throwable) {
            null
        }
    }
}
