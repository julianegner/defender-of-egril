package de.egril.defender.mapgen

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.abs

/**
 * Optimal PNG encoder using adaptive row filtering and maximum DEFLATE compression.
 *
 * Produces significantly smaller PNG files than the default `ImageIO.write("png")`
 * by using per-row adaptive filter selection (tries all 5 PNG filter types per row
 * and picks the one that minimizes the sum of absolute values of the filtered bytes).
 *
 * Additionally, map images are fully opaque (alpha = 0xFF), so this encoder strips
 * the alpha channel and writes RGB (3 bytes/pixel) instead of RGBA (4 bytes/pixel),
 * saving ~25% of raw data before compression.
 *
 * NOTE: This file is intentionally duplicated in the backend server module
 * (servers/backend/src/main/kotlin/de/egril/defender/OptimalPngEncoder.kt)
 * because frontend and backend are separate Gradle modules with no shared JVM library.
 * Keep both copies in sync when making changes.
 */
object OptimalPngEncoder {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /**
     * Encode an ARGB pixel array to optimally compressed PNG bytes.
     *
     * @param pixels ARGB int array (row-major), where each int is 0xAARRGGBB
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return PNG-encoded bytes
     */
    fun encode(pixels: IntArray, width: Int, height: Int): ByteArray {
        val hasAlpha = pixelsHaveAlpha(pixels)
        val bytesPerPixel = if (hasAlpha) 4 else 3
        val bytesPerRow = width * bytesPerPixel

        // Build filtered scanline data with adaptive filter selection
        val filteredData = ByteArrayOutputStream(height * (1 + bytesPerRow))
        val prevRow = ByteArray(bytesPerRow)

        for (y in 0 until height) {
            val currRow = ByteArray(bytesPerRow)
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                val offset = x * bytesPerPixel
                currRow[offset] = ((pixel ushr 16) and 0xFF).toByte()     // R
                currRow[offset + 1] = ((pixel ushr 8) and 0xFF).toByte()  // G
                currRow[offset + 2] = (pixel and 0xFF).toByte()           // B
                if (hasAlpha) {
                    currRow[offset + 3] = ((pixel ushr 24) and 0xFF).toByte() // A
                }
            }

            // Adaptive filter: try all 5 filters and pick the best
            val bestFilter = selectBestFilter(currRow, prevRow, bytesPerPixel)
            val filteredRow = applyFilter(bestFilter, currRow, prevRow, bytesPerPixel)

            filteredData.write(bestFilter)
            filteredData.write(filteredRow)

            System.arraycopy(currRow, 0, prevRow, 0, bytesPerRow)
        }

        // Compress with maximum DEFLATE level
        val compressedData = deflateData(filteredData.toByteArray())

        // Build the complete PNG file
        return buildPngFile(width, height, hasAlpha, compressedData)
    }

    /** Check if any pixel has a non-fully-opaque alpha value. */
    private fun pixelsHaveAlpha(pixels: IntArray): Boolean {
        for (pixel in pixels) {
            if ((pixel ushr 24) and 0xFF != 0xFF) return true
        }
        return false
    }

    /**
     * Try all 5 PNG filter types on the given row and return the filter type (0-4)
     * that produces the smallest sum of absolute values of filtered bytes.
     */
    private fun selectBestFilter(
        currRow: ByteArray,
        prevRow: ByteArray,
        bytesPerPixel: Int
    ): Int {
        var bestFilter = 0
        var bestSum = Long.MAX_VALUE

        for (filterType in 0..4) {
            val sum = filterAbsoluteSum(filterType, currRow, prevRow, bytesPerPixel)
            if (sum < bestSum) {
                bestSum = sum
                bestFilter = filterType
            }
        }
        return bestFilter
    }

    /** Compute the sum of absolute values for a given filter type without allocating a full array. */
    private fun filterAbsoluteSum(
        filterType: Int,
        currRow: ByteArray,
        prevRow: ByteArray,
        bytesPerPixel: Int
    ): Long {
        var sum = 0L
        for (i in currRow.indices) {
            val raw = currRow[i].toInt() and 0xFF
            val left = if (i >= bytesPerPixel) currRow[i - bytesPerPixel].toInt() and 0xFF else 0
            val up = prevRow[i].toInt() and 0xFF
            val upLeft = if (i >= bytesPerPixel) prevRow[i - bytesPerPixel].toInt() and 0xFF else 0

            val filtered = when (filterType) {
                0 -> raw                                          // None
                1 -> (raw - left) and 0xFF                        // Sub
                2 -> (raw - up) and 0xFF                          // Up
                3 -> (raw - ((left + up) / 2)) and 0xFF           // Average
                4 -> (raw - paethPredictor(left, up, upLeft)) and 0xFF // Paeth
                else -> raw
            }
            sum += abs(filtered - if (filtered > 127) 256 else 0)
        }
        return sum
    }

    /** Apply the selected filter to the row. */
    private fun applyFilter(
        filterType: Int,
        currRow: ByteArray,
        prevRow: ByteArray,
        bytesPerPixel: Int
    ): ByteArray {
        val result = ByteArray(currRow.size)
        for (i in currRow.indices) {
            val raw = currRow[i].toInt() and 0xFF
            val left = if (i >= bytesPerPixel) currRow[i - bytesPerPixel].toInt() and 0xFF else 0
            val up = prevRow[i].toInt() and 0xFF
            val upLeft = if (i >= bytesPerPixel) prevRow[i - bytesPerPixel].toInt() and 0xFF else 0

            result[i] = when (filterType) {
                0 -> raw
                1 -> (raw - left) and 0xFF
                2 -> (raw - up) and 0xFF
                3 -> (raw - ((left + up) / 2)) and 0xFF
                4 -> (raw - paethPredictor(left, up, upLeft)) and 0xFF
                else -> raw
            }.toByte()
        }
        return result
    }

    /** PNG Paeth predictor function. */
    private fun paethPredictor(a: Int, b: Int, c: Int): Int {
        val p = a + b - c
        val pa = abs(p - a)
        val pb = abs(p - b)
        val pc = abs(p - c)
        return when {
            pa <= pb && pa <= pc -> a
            pb <= pc -> b
            else -> c
        }
    }

    /** Compress data using DEFLATE with maximum compression level. */
    private fun deflateData(data: ByteArray): ByteArray {
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        try {
            deflater.setInput(data)
            deflater.finish()
            val output = ByteArrayOutputStream(data.size / 2)
            val buffer = ByteArray(8192)
            while (!deflater.finished()) {
                val len = deflater.deflate(buffer)
                output.write(buffer, 0, len)
            }
            return output.toByteArray()
        } finally {
            deflater.end()
        }
    }

    /** Build the complete PNG file from compressed IDAT data. */
    private fun buildPngFile(
        width: Int,
        height: Int,
        hasAlpha: Boolean,
        compressedData: ByteArray
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // PNG signature
        output.write(PNG_SIGNATURE)

        // IHDR chunk
        val ihdr = ByteArray(13)
        writeInt(ihdr, 0, width)
        writeInt(ihdr, 4, height)
        ihdr[8] = 8 // bit depth
        ihdr[9] = if (hasAlpha) 6 else 2 // color type: 6=RGBA, 2=RGB
        ihdr[10] = 0 // compression method (deflate)
        ihdr[11] = 0 // filter method (adaptive)
        ihdr[12] = 0 // interlace method (none)
        writeChunk(output, "IHDR", ihdr)

        // IDAT chunk(s) - write compressed data in chunks of 64KB
        var offset = 0
        while (offset < compressedData.size) {
            val chunkSize = minOf(65536, compressedData.size - offset)
            writeChunk(output, "IDAT", compressedData, offset, chunkSize)
            offset += chunkSize
        }

        // IEND chunk
        writeChunk(output, "IEND", ByteArray(0))

        return output.toByteArray()
    }

    /** Write a 4-byte big-endian integer to a byte array at the given offset. */
    private fun writeInt(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value ushr 24).toByte()
        array[offset + 1] = (value ushr 16).toByte()
        array[offset + 2] = (value ushr 8).toByte()
        array[offset + 3] = value.toByte()
    }

    /** Write a PNG chunk to the output stream. */
    private fun writeChunk(
        output: ByteArrayOutputStream,
        type: String,
        data: ByteArray,
        dataOffset: Int = 0,
        dataLength: Int = data.size
    ) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)

        // Length (4 bytes, big-endian)
        val lengthBytes = ByteArray(4)
        writeInt(lengthBytes, 0, dataLength)
        output.write(lengthBytes)

        // Type (4 bytes)
        output.write(typeBytes)

        // Data
        output.write(data, dataOffset, dataLength)

        // CRC-32 over type + data
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data, dataOffset, dataLength)
        val crcBytes = ByteArray(4)
        writeInt(crcBytes, 0, crc.value.toInt())
        output.write(crcBytes)
    }
}
