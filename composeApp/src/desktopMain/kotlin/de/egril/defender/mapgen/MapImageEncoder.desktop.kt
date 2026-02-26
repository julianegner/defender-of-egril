package de.egril.defender.mapgen

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual object MapImageEncoder {
    actual fun encodeToPng(pixels: IntArray, width: Int, height: Int): ByteArray? {
        return try {
            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            image.setRGB(0, 0, width, height, pixels, 0, width)
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "png", baos)
            baos.toByteArray()
        } catch (e: Exception) {
            println("MapImageEncoder: Failed to encode PNG: ${e.message}")
            null
        }
    }
}
