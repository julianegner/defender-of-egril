package de.egril.defender.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual fun decodeMapImageBitmap(bytes: ByteArray): ImageBitmap? {
    return try {
        val image = ImageIO.read(ByteArrayInputStream(bytes))
        image?.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
