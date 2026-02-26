package de.egril.defender.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun decodeMapImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val skiaImage = Image.makeFromEncoded(bytes)
        skiaImage.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
