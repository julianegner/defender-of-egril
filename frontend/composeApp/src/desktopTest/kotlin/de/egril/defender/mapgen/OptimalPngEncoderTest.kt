package de.egril.defender.mapgen

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OptimalPngEncoderTest {

    @Test
    fun `encode produces valid PNG that can be decoded`() {
        val width = 10
        val height = 10
        val pixels = IntArray(width * height) { i ->
            val r = (i * 7) % 256
            val g = (i * 13) % 256
            val b = (i * 23) % 256
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val pngBytes = OptimalPngEncoder.encode(pixels, width, height)

        assertNotNull(pngBytes)
        assertTrue(pngBytes.size > 8, "PNG should be larger than just the signature")

        // Verify PNG signature
        assertEquals(0x89.toByte(), pngBytes[0])
        assertEquals(0x50.toByte(), pngBytes[1]) // 'P'
        assertEquals(0x4E.toByte(), pngBytes[2]) // 'N'
        assertEquals(0x47.toByte(), pngBytes[3]) // 'G'

        // Verify it can be decoded by ImageIO
        val decoded = ImageIO.read(ByteArrayInputStream(pngBytes))
        assertNotNull(decoded, "ImageIO should be able to decode the PNG")
        assertEquals(width, decoded.width)
        assertEquals(height, decoded.height)
    }

    @Test
    fun `encoded PNG preserves pixel colors`() {
        val width = 4
        val height = 4
        val pixels = IntArray(width * height) { i ->
            val r = i * 16
            val g = 255 - i * 16
            val b = (i * 37) % 256
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        val pngBytes = OptimalPngEncoder.encode(pixels, width, height)
        assertNotNull(pngBytes)

        val decoded = ImageIO.read(ByteArrayInputStream(pngBytes))
        assertNotNull(decoded)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val original = pixels[y * width + x]
                val decodedPixel = decoded.getRGB(x, y)
                val origR = (original ushr 16) and 0xFF
                val origG = (original ushr 8) and 0xFF
                val origB = original and 0xFF
                val decR = (decodedPixel ushr 16) and 0xFF
                val decG = (decodedPixel ushr 8) and 0xFF
                val decB = decodedPixel and 0xFF
                assertEquals(origR, decR, "Red mismatch at ($x,$y)")
                assertEquals(origG, decG, "Green mismatch at ($x,$y)")
                assertEquals(origB, decB, "Blue mismatch at ($x,$y)")
            }
        }
    }

    @Test
    fun `optimal encoder produces smaller output than ImageIO`() {
        // Create a larger image with gradients (similar to map images)
        val width = 200
        val height = 200
        val pixels = IntArray(width * height) { i ->
            val x = i % width
            val y = i / width
            val r = (x * 255) / width
            val g = (y * 255) / height
            val b = ((x + y) * 127) / (width + height)
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }

        // Encode with OptimalPngEncoder
        val optimalBytes = OptimalPngEncoder.encode(pixels, width, height)
        assertNotNull(optimalBytes)

        // Encode with default ImageIO
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, width, height, pixels, 0, width)
        val baos = java.io.ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val defaultBytes = baos.toByteArray()

        println("OptimalPngEncoder: ${optimalBytes.size} bytes")
        println("Default ImageIO: ${defaultBytes.size} bytes")
        println("Savings: ${100 - (optimalBytes.size * 100 / defaultBytes.size)}%")

        assertTrue(
            optimalBytes.size < defaultBytes.size,
            "Optimal encoder (${optimalBytes.size} bytes) should produce smaller files than ImageIO (${defaultBytes.size} bytes)"
        )
    }

    @Test
    fun `encoder handles single pixel image`() {
        val pixels = intArrayOf((0xFF shl 24) or (128 shl 16) or (64 shl 8) or 32)
        val pngBytes = OptimalPngEncoder.encode(pixels, 1, 1)
        assertNotNull(pngBytes)

        val decoded = ImageIO.read(ByteArrayInputStream(pngBytes))
        assertNotNull(decoded)
        assertEquals(1, decoded.width)
        assertEquals(1, decoded.height)
    }

    @Test
    fun `encoder strips alpha channel for opaque images`() {
        val width = 4
        val height = 4
        // All pixels fully opaque (alpha = 0xFF)
        val opaquePixels = IntArray(width * height) { (0xFF shl 24) or (100 shl 16) or (150 shl 8) or 200 }
        val opaqueBytes = OptimalPngEncoder.encode(opaquePixels, width, height)

        // Same pixels but with varying alpha
        val alphaPixels = IntArray(width * height) { i ->
            val alpha = 128 + (i * 8)
            (alpha shl 24) or (100 shl 16) or (150 shl 8) or 200
        }
        val alphaBytes = OptimalPngEncoder.encode(alphaPixels, width, height)

        assertNotNull(opaqueBytes)
        assertNotNull(alphaBytes)
        // Opaque image should be smaller (RGB vs RGBA)
        assertTrue(
            opaqueBytes.size < alphaBytes.size,
            "Opaque image (${opaqueBytes.size} bytes) should be smaller than alpha image (${alphaBytes.size} bytes)"
        )
    }
}
