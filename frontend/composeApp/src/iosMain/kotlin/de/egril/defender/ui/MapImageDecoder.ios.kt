package de.egril.defender.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skia.Bitmap as SkiaBitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image

actual fun decodeMapImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val skiaImage = Image.makeFromEncoded(bytes)
        val bitmap = SkiaBitmap()
        bitmap.allocN32Pixels(skiaImage.width, skiaImage.height)
        val canvas = Canvas(bitmap)
        canvas.drawImage(skiaImage, 0f, 0f)
        bitmap.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
