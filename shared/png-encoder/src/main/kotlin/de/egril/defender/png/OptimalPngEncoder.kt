package de.egril.defender.png

import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater
import kotlin.math.abs

/**
 * Optimal PNG encoder using adaptive row filtering and maximum DEFLATE compression.
 *
 * Encoding strategy (in order of preference):
 * 1. **Palette mode** (color type 3) — if the image has ≤ 256 unique RGB colors,
 *    encodes as indexed PNG with 1 byte/pixel. This is ~3× smaller than RGB.
 * 2. **Quantized palette mode** — if the image has > 256 unique colors but is fully
 *    opaque, applies median-cut color quantization to reduce to 256 colors and
 *    encodes as indexed PNG. Visual quality loss is imperceptible for map images.
 * 3. **RGB mode** (color type 2) — fallback for opaque images that can't be palettized.
 * 4. **RGBA mode** (color type 6) — for images with transparency.
 *
 * All modes use per-row adaptive filter selection and `Deflater.BEST_COMPRESSION`.
 */
object OptimalPngEncoder {

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    /** Maximum palette size for indexed PNG. */
    private const val MAX_PALETTE_SIZE = 256

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

        // For opaque images, try both palette and RGB, pick the smallest
        if (!hasAlpha) {
            val paletteResult = buildPalette(pixels)
            if (paletteResult != null) {
                val palettePng = encodePaletted(paletteResult.palette, paletteResult.indices, width, height)
                val rgbPng = encodeTruecolor(pixels, width, height, hasAlpha = false)
                return if (palettePng.size <= rgbPng.size) palettePng else rgbPng
            }
        }

        // Fallback to RGB/RGBA truecolor encoding
        return encodeTruecolor(pixels, width, height, hasAlpha)
    }

    // =========================================================================
    // Palette building & color quantization
    // =========================================================================

    private class PaletteResult(
        /** Palette colors as packed 0x00RRGGBB ints. */
        val palette: IntArray,
        /** Per-pixel palette index (row-major, same length as input pixels). */
        val indices: ByteArray
    )

    /** A color with its pixel frequency, used during median-cut quantization. */
    private class ColorEntry(val rgb: Int, val count: Int)

    /**
     * Build a palette for the given opaque pixels.
     * - If unique colors ≤ 256: exact palette (no quality loss).
     * - If > 256: median-cut quantization to 256 colors.
     *
     * The palette is always sorted by luminance so that smooth gradients in the
     * image map to slowly-changing palette indices, maximizing PNG filter and
     * DEFLATE compression effectiveness.
     */
    private fun buildPalette(pixels: IntArray): PaletteResult? {
        // Strip alpha — pixels are known to be opaque
        val rgbPixels = IntArray(pixels.size) { pixels[it] and 0x00FFFFFF }

        // Count unique colors
        val colorSet = HashSet<Int>(minOf(pixels.size, 1024))
        for (c in rgbPixels) colorSet.add(c)
        val uniqueCount = colorSet.size

        val rawResult = if (uniqueCount <= MAX_PALETTE_SIZE) {
            // Exact palette — no quantization needed
            val palette = colorSet.toIntArray()
            val colorToIndex = HashMap<Int, Int>(palette.size * 2)
            for (i in palette.indices) colorToIndex[palette[i]] = i
            val indices = ByteArray(rgbPixels.size) { colorToIndex[rgbPixels[it]]!!.toByte() }
            PaletteResult(palette, indices)
        } else {
            // Median-cut quantization to MAX_PALETTE_SIZE colors
            medianCutQuantize(rgbPixels)
        }

        // Sort palette by luminance for better spatial coherence in indices
        return sortPaletteByLuminance(rawResult)
    }

    /**
     * Sort palette entries by perceived luminance and re-map indices.
     * This ensures that smooth color gradients in the image produce slowly
     * changing index values, which compress dramatically better with PNG
     * row filtering + DEFLATE.
     */
    private fun sortPaletteByLuminance(result: PaletteResult): PaletteResult {
        val oldPalette = result.palette
        // Sort order: by luminance (0.299R + 0.587G + 0.114B)
        val sortedIndices = oldPalette.indices.sortedBy { i ->
            val r = (oldPalette[i] ushr 16) and 0xFF
            val g = (oldPalette[i] ushr 8) and 0xFF
            val b = oldPalette[i] and 0xFF
            r * 299 + g * 587 + b * 114
        }
        val newPalette = IntArray(oldPalette.size) { oldPalette[sortedIndices[it]] }
        // Build old→new index mapping
        val oldToNew = IntArray(oldPalette.size)
        for (newIdx in sortedIndices.indices) {
            oldToNew[sortedIndices[newIdx]] = newIdx
        }
        val newIndices = ByteArray(result.indices.size) {
            oldToNew[result.indices[it].toInt() and 0xFF].toByte()
        }
        return PaletteResult(newPalette, newIndices)
    }

    /**
     * Median-cut color quantization: partitions the RGB color space into
     * [MAX_PALETTE_SIZE] buckets, picks the average color of each bucket
     * as the representative palette entry, and maps every pixel to the
     * nearest palette entry.
     */
    private fun medianCutQuantize(rgbPixels: IntArray): PaletteResult {
        // Build histogram: unique color → count
        val histogram = HashMap<Int, Int>(4096)
        for (c in rgbPixels) histogram[c] = (histogram[c] ?: 0) + 1

        // Collect unique colors into a list for partitioning
        val entries = histogram.map { (rgb, count) -> ColorEntry(rgb, count) }

        // Partition into buckets using median cut.
        // Track splittable bucket count to avoid O(n²) any{} scan each iteration.
        val buckets = mutableListOf(entries.toMutableList())
        var splittableCount = if (entries.size > 1) 1 else 0
        while (buckets.size < MAX_PALETTE_SIZE && splittableCount > 0) {
            // Find the bucket with the widest color range to split
            var bestIdx = -1
            var bestRange = -1
            for (i in buckets.indices) {
                if (buckets[i].size <= 1) continue
                val range = bucketColorRange(buckets[i])
                if (range > bestRange) {
                    bestRange = range
                    bestIdx = i
                }
            }
            if (bestIdx < 0) break

            val bucket = buckets.removeAt(bestIdx)
            splittableCount-- // removed one splittable bucket
            val (lo, hi) = splitBucket(bucket)
            buckets.add(lo)
            buckets.add(hi)
            if (lo.size > 1) splittableCount++
            if (hi.size > 1) splittableCount++
        }

        // Compute palette: average color of each bucket, weighted by frequency
        val palette = IntArray(buckets.size)
        for (i in buckets.indices) {
            var rSum = 0L; var gSum = 0L; var bSum = 0L; var wSum = 0L
            for (entry in buckets[i]) {
                rSum += ((entry.rgb ushr 16) and 0xFF).toLong() * entry.count
                gSum += ((entry.rgb ushr 8) and 0xFF).toLong() * entry.count
                bSum += (entry.rgb and 0xFF).toLong() * entry.count
                wSum += entry.count
            }
            if (wSum == 0L) continue
            val r = (rSum / wSum).toInt().coerceIn(0, 255)
            val g = (gSum / wSum).toInt().coerceIn(0, 255)
            val b = (bSum / wSum).toInt().coerceIn(0, 255)
            palette[i] = (r shl 16) or (g shl 8) or b
        }

        // Map each pixel to the nearest palette entry using a cache
        val nearestCache = HashMap<Int, Byte>(histogram.size * 2)
        val indices = ByteArray(rgbPixels.size)
        for (i in rgbPixels.indices) {
            val rgb = rgbPixels[i]
            val cached = nearestCache[rgb]
            if (cached != null) {
                indices[i] = cached
            } else {
                val idx = nearestPaletteIndex(rgb, palette)
                nearestCache[rgb] = idx.toByte()
                indices[i] = idx.toByte()
            }
        }

        return PaletteResult(palette, indices)
    }

    /** Compute the range of the widest channel in the bucket. */
    private fun bucketColorRange(bucket: List<ColorEntry>): Int {
        var rMin = 255; var rMax = 0
        var gMin = 255; var gMax = 0
        var bMin = 255; var bMax = 0
        for (entry in bucket) {
            val r = (entry.rgb ushr 16) and 0xFF
            val g = (entry.rgb ushr 8) and 0xFF
            val b = entry.rgb and 0xFF
            if (r < rMin) rMin = r; if (r > rMax) rMax = r
            if (g < gMin) gMin = g; if (g > gMax) gMax = g
            if (b < bMin) bMin = b; if (b > bMax) bMax = b
        }
        return maxOf(rMax - rMin, gMax - gMin, bMax - bMin)
    }

    /** Split a bucket along the channel with the widest range using median. */
    private fun splitBucket(
        bucket: MutableList<ColorEntry>
    ): Pair<MutableList<ColorEntry>, MutableList<ColorEntry>> {
        // Find the channel with the widest range
        var rMin = 255; var rMax = 0
        var gMin = 255; var gMax = 0
        var bMin = 255; var bMax = 0
        for (entry in bucket) {
            val r = (entry.rgb ushr 16) and 0xFF
            val g = (entry.rgb ushr 8) and 0xFF
            val b = entry.rgb and 0xFF
            if (r < rMin) rMin = r; if (r > rMax) rMax = r
            if (g < gMin) gMin = g; if (g > gMax) gMax = g
            if (b < bMin) bMin = b; if (b > bMax) bMax = b
        }

        val rRange = rMax - rMin
        val gRange = gMax - gMin
        val bRange = bMax - bMin

        // Sort by the widest channel
        val sorted = when {
            rRange >= gRange && rRange >= bRange ->
                bucket.sortedBy { (it.rgb ushr 16) and 0xFF }
            gRange >= bRange ->
                bucket.sortedBy { (it.rgb ushr 8) and 0xFF }
            else ->
                bucket.sortedBy { it.rgb and 0xFF }
        }

        val mid = sorted.size / 2
        return Pair(
            sorted.subList(0, mid).toMutableList(),
            sorted.subList(mid, sorted.size).toMutableList()
        )
    }

    /** Find the index of the nearest palette color by Euclidean distance. */
    private fun nearestPaletteIndex(rgb: Int, palette: IntArray): Int {
        val r = (rgb ushr 16) and 0xFF
        val g = (rgb ushr 8) and 0xFF
        val b = rgb and 0xFF
        var bestIdx = 0
        var bestDist = Int.MAX_VALUE
        for (i in palette.indices) {
            val pr = (palette[i] ushr 16) and 0xFF
            val pg = (palette[i] ushr 8) and 0xFF
            val pb = palette[i] and 0xFF
            val dr = r - pr; val dg = g - pg; val db = b - pb
            val dist = dr * dr + dg * dg + db * db
            if (dist < bestDist) {
                bestDist = dist
                bestIdx = i
            }
            if (dist == 0) break
        }
        return bestIdx
    }

    // =========================================================================
    // Palette-based PNG encoding (color type 3)
    // =========================================================================

    /**
     * Encode pixels as an indexed PNG (color type 3) with the given palette.
     * Each pixel is represented by 1 byte (palette index) instead of 3 (RGB).
     */
    private fun encodePaletted(
        palette: IntArray,
        indices: ByteArray,
        width: Int,
        height: Int
    ): ByteArray {
        val bytesPerPixel = 1
        val bytesPerRow = width

        // Build filtered scanline data with adaptive filter selection
        val filteredData = ByteArrayOutputStream(height * (1 + bytesPerRow))
        val prevRow = ByteArray(bytesPerRow)

        for (y in 0 until height) {
            val currRow = ByteArray(bytesPerRow)
            System.arraycopy(indices, y * width, currRow, 0, width)

            val bestFilter = selectBestFilter(currRow, prevRow, bytesPerPixel)
            val filteredRow = applyFilter(bestFilter, currRow, prevRow, bytesPerPixel)

            filteredData.write(bestFilter)
            filteredData.write(filteredRow)

            System.arraycopy(currRow, 0, prevRow, 0, bytesPerRow)
        }

        val compressedData = deflateData(filteredData.toByteArray())

        return buildPalettedPngFile(width, height, palette, compressedData)
    }

    /** Build a complete indexed PNG file with PLTE chunk. */
    private fun buildPalettedPngFile(
        width: Int,
        height: Int,
        palette: IntArray,
        compressedData: ByteArray
    ): ByteArray {
        val output = ByteArrayOutputStream()

        // PNG signature
        output.write(PNG_SIGNATURE)

        // IHDR chunk — color type 3 (indexed)
        val ihdr = ByteArray(13)
        writeInt(ihdr, 0, width)
        writeInt(ihdr, 4, height)
        ihdr[8] = 8 // bit depth (8 bits per palette index)
        ihdr[9] = 3 // color type: 3 = indexed
        ihdr[10] = 0 // compression method (deflate)
        ihdr[11] = 0 // filter method (adaptive)
        ihdr[12] = 0 // interlace method (none)
        writeChunk(output, "IHDR", ihdr)

        // PLTE chunk — palette entries as R,G,B triplets
        val plteData = ByteArray(palette.size * 3)
        for (i in palette.indices) {
            plteData[i * 3] = ((palette[i] ushr 16) and 0xFF).toByte()     // R
            plteData[i * 3 + 1] = ((palette[i] ushr 8) and 0xFF).toByte()  // G
            plteData[i * 3 + 2] = (palette[i] and 0xFF).toByte()           // B
        }
        writeChunk(output, "PLTE", plteData)

        // IDAT chunk(s)
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

    // =========================================================================
    // Truecolor PNG encoding (color type 2 or 6) — fallback
    // =========================================================================

    /** Encode pixels as RGB (color type 2) or RGBA (color type 6) PNG. */
    private fun encodeTruecolor(
        pixels: IntArray,
        width: Int,
        height: Int,
        hasAlpha: Boolean
    ): ByteArray {
        val bytesPerPixel = if (hasAlpha) 4 else 3
        val bytesPerRow = width * bytesPerPixel

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

            val bestFilter = selectBestFilter(currRow, prevRow, bytesPerPixel)
            val filteredRow = applyFilter(bestFilter, currRow, prevRow, bytesPerPixel)

            filteredData.write(bestFilter)
            filteredData.write(filteredRow)

            System.arraycopy(currRow, 0, prevRow, 0, bytesPerRow)
        }

        val compressedData = deflateData(filteredData.toByteArray())
        return buildTruecolorPngFile(width, height, hasAlpha, compressedData)
    }

    /** Build a complete truecolor PNG file. */
    private fun buildTruecolorPngFile(
        width: Int,
        height: Int,
        hasAlpha: Boolean,
        compressedData: ByteArray
    ): ByteArray {
        val output = ByteArrayOutputStream()
        output.write(PNG_SIGNATURE)

        val ihdr = ByteArray(13)
        writeInt(ihdr, 0, width)
        writeInt(ihdr, 4, height)
        ihdr[8] = 8 // bit depth
        ihdr[9] = if (hasAlpha) 6 else 2 // color type: 6=RGBA, 2=RGB
        ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0
        writeChunk(output, "IHDR", ihdr)

        var offset = 0
        while (offset < compressedData.size) {
            val chunkSize = minOf(65536, compressedData.size - offset)
            writeChunk(output, "IDAT", compressedData, offset, chunkSize)
            offset += chunkSize
        }

        writeChunk(output, "IEND", ByteArray(0))
        return output.toByteArray()
    }

    // =========================================================================
    // Shared helpers: filtering, compression, chunk writing
    // =========================================================================

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
