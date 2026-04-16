package de.egril.defender.mapgen

import de.egril.defender.png.OptimalPngEncoder

actual object MapImageEncoder {
    actual fun encodeToPng(pixels: IntArray, width: Int, height: Int): ByteArray? {
        return try {
            OptimalPngEncoder.encode(pixels, width, height)
        } catch (e: Exception) {
            println("MapImageEncoder: Failed to encode PNG: ${e.message}")
            null
        }
    }
}
