package de.egril.defender.mapgen

import de.egril.defender.png.OptimalPngEncoder
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
    fun `encoder optimizes opaque images and uses RGBA for alpha images`() {
        val width = 64
        val height = 64
        // All pixels fully opaque with some color variation
        val opaquePixels = IntArray(width * height) { i ->
            val r = (i * 3) % 256
            val g = (i * 7) % 256
            val b = (i * 13) % 256
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        val opaqueBytes = OptimalPngEncoder.encode(opaquePixels, width, height)

        // Same pixels but with varying alpha — forces RGBA truecolor
        val alphaPixels = IntArray(width * height) { i ->
            val alpha = 128 + (i % 128)
            val r = (i * 3) % 256
            val g = (i * 7) % 256
            val b = (i * 13) % 256
            (alpha shl 24) or (r shl 16) or (g shl 8) or b
        }
        val alphaBytes = OptimalPngEncoder.encode(alphaPixels, width, height)

        assertNotNull(opaqueBytes)
        assertNotNull(alphaBytes)

        // Opaque image should use palette (3) or RGB (2) — not RGBA (6)
        val opaqueColorType = opaqueBytes[25].toInt()
        assertTrue(
            opaqueColorType == 2 || opaqueColorType == 3,
            "Opaque image should use color type 2 (RGB) or 3 (indexed), got $opaqueColorType"
        )

        // Alpha image should use RGBA truecolor (color type 6)
        assertEquals(6.toByte(), alphaBytes[25], "Alpha image should use color type 6 (RGBA)")

        // Both should be valid, decodable PNGs
        val decodedOpaque = ImageIO.read(ByteArrayInputStream(opaqueBytes))
        val decodedAlpha = ImageIO.read(ByteArrayInputStream(alphaBytes))
        assertNotNull(decodedOpaque)
        assertNotNull(decodedAlpha)
        assertEquals(width, decodedOpaque.width)
        assertEquals(width, decodedAlpha.width)
    }

    @Test
    fun `encoder uses palette mode for images with few colors`() {
        // Create a 100x100 image with only 10 distinct colors — palette mode is optimal
        val width = 100
        val height = 100
        val colors = IntArray(10) { i ->
            (0xFF shl 24) or ((i * 25) shl 16) or ((255 - i * 25) shl 8) or (i * 10)
        }
        val pixels = IntArray(width * height) { i -> colors[i % 10] }

        val pngBytes = OptimalPngEncoder.encode(pixels, width, height)
        assertNotNull(pngBytes)

        // Verify it's a valid PNG that can be decoded
        val decoded = ImageIO.read(ByteArrayInputStream(pngBytes))
        assertNotNull(decoded)
        assertEquals(width, decoded.width)
        assertEquals(height, decoded.height)

        // Should be very small — palette mode is ideal for few-color images
        assertTrue(
            pngBytes.size < 5000,
            "PNG (${pngBytes.size} bytes) should be well under 5KB for a 10-color 100x100 image"
        )
    }

    @Test
    fun `encoder handles images with many colors efficiently`() {
        // Create a 200x200 image with thousands of unique colors (gradients + noise)
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

        val pngBytes = OptimalPngEncoder.encode(pixels, width, height)
        assertNotNull(pngBytes)

        // Verify it's a valid PNG that can be decoded
        val decoded = ImageIO.read(ByteArrayInputStream(pngBytes))
        assertNotNull(decoded)
        assertEquals(width, decoded.width)
        assertEquals(height, decoded.height)

        // Encoder picks whichever mode (palette or RGB) produces smaller output
        val colorType = pngBytes[25].toInt()
        assertTrue(
            colorType == 2 || colorType == 3,
            "Opaque image should use color type 2 (RGB) or 3 (indexed), got $colorType"
        )

        // Should be significantly smaller than default ImageIO
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, width, height, pixels, 0, width)
        val baos = java.io.ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        val imageioSize = baos.size()

        println("Optimal PNG (color type $colorType): ${pngBytes.size} bytes vs ImageIO: $imageioSize bytes")
        assertTrue(
            pngBytes.size < imageioSize,
            "Optimal PNG (${pngBytes.size} bytes) should be smaller than ImageIO ($imageioSize bytes)"
        )
    }
}
