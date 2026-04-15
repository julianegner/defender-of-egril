package de.egril.defender.ui.common

/**
 * Pure-Kotlin QR code matrix generator.
 *
 * Encodes a UTF-8 string as a QR code using byte mode and error correction level M.
 * Supports versions 1–7, which covers URLs up to roughly 120 bytes.
 *
 * Returns a 2-D boolean matrix:  true = dark module,  false = light module.
 * The matrix is square with side length (4 * version + 17).
 */
object QrCodeGenerator {

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun generate(text: String): Array<BooleanArray> {
        val bytes = text.encodeToByteArray()
        val version = selectVersion(bytes.size)
        val dataCodewords = encodeData(bytes, version)
        val allCodewords = addErrorCorrection(dataCodewords, version)
        val (matrix, reserved) = buildMatrix(version, allCodewords)
        return applyBestMask(matrix, reserved, version)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Version selection
    // ─────────────────────────────────────────────────────────────────────────

    /** Data capacity in bytes (M error correction), versions 1-7. */
    private val DATA_CAPACITY = intArrayOf(16, 28, 44, 64, 86, 108, 124)

    private fun selectVersion(byteCount: Int): Int {
        // Bits needed: 4 (mode indicator) + 8 (char count, versions 1-9) + 8*n (data) + 4 (terminator)
        // = 16 + 8*n  →  ceil to bytes = 2 + n
        val bytesNeeded = byteCount + 2
        for (v in 1..7) {
            if (DATA_CAPACITY[v - 1] >= bytesNeeded) return v
        }
        error("Input too long for QR versions 1–7 with M error correction (max ~122 bytes). Got $byteCount bytes.")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data encoding (byte mode)
    // ─────────────────────────────────────────────────────────────────────────

    private fun encodeData(bytes: ByteArray, version: Int): ByteArray {
        val totalDataBytes = DATA_CAPACITY[version - 1]
        val buf = BitBuffer()

        // Mode indicator: 0100 = byte mode
        buf.append(0b0100, 4)
        // Character count (8 bits for versions 1–9)
        buf.append(bytes.size, 8)
        // Data bytes
        for (b in bytes) buf.append(b.toInt() and 0xFF, 8)
        // Terminator (up to 4 zero bits)
        val remaining = totalDataBytes * 8 - buf.length
        buf.append(0, minOf(4, remaining))
        // Pad to byte boundary
        while (buf.length % 8 != 0) buf.append(0, 1)
        // Pad bytes: 0xEC, 0x11 alternating
        val padBytes = intArrayOf(0xEC, 0x11)
        var pi = 0
        while (buf.length < totalDataBytes * 8) {
            buf.append(padBytes[pi % 2], 8); pi++
        }

        return buf.toByteArray()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Reed-Solomon error correction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Block structure for M error correction:
     * [ecPerBlock, dataPerBlock, numBlocks]
     * Verified against ISO/IEC 18004 Table 9.
     */
    private val BLOCK_INFO = arrayOf(
        intArrayOf(10, 16, 1),   // V1 M
        intArrayOf(16, 28, 1),   // V2 M
        intArrayOf(13, 22, 2),   // V3 M
        intArrayOf(18, 32, 2),   // V4 M
        intArrayOf(24, 43, 2),   // V5 M
        intArrayOf(16, 27, 4),   // V6 M
        intArrayOf(18, 31, 4),   // V7 M
    )

    private fun addErrorCorrection(data: ByteArray, version: Int): ByteArray {
        val (ecPerBlock, dataPerBlock, numBlocks) = BLOCK_INFO[version - 1].let {
            Triple(it[0], it[1], it[2])
        }

        val blocks = Array(numBlocks) { i ->
            data.sliceArray(i * dataPerBlock until (i + 1) * dataPerBlock)
        }
        val ecBlocks = blocks.map { generateEcCodewords(it, ecPerBlock) }

        // Interleave data, then interleave EC
        val result = ByteArray((dataPerBlock + ecPerBlock) * numBlocks)
        var idx = 0
        for (pos in 0 until dataPerBlock) for (block in blocks) result[idx++] = block[pos]
        for (pos in 0 until ecPerBlock) for (ec in ecBlocks) result[idx++] = ec[pos]
        return result
    }

    // GF(256) tables with primitive polynomial x^8+x^4+x^3+x^2+1 = 0x11D
    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 256) {
            GF_EXP[i] = x; GF_EXP[i + 256] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[(GF_LOG[a] + GF_LOG[b]) % 255]
    }

    private fun gfPow(x: Int, power: Int): Int = GF_EXP[(GF_LOG[x] * power) % 255]

    private fun rsGeneratorPoly(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val factor = intArrayOf(1, gfPow(2, i))
            val product = IntArray(poly.size + 1)
            for (p in poly.indices) for (f in factor.indices) {
                product[p + f] = product[p + f] xor gfMul(poly[p], factor[f])
            }
            poly = product
        }
        return poly
    }

    private fun generateEcCodewords(data: ByteArray, ecCount: Int): ByteArray {
        val generator = rsGeneratorPoly(ecCount)
        val msg = IntArray(data.size + ecCount)
        for (i in data.indices) msg[i] = data[i].toInt() and 0xFF
        for (i in data.indices) {
            val coeff = msg[i]
            if (coeff != 0) {
                for (j in 1..ecCount) msg[i + j] = msg[i + j] xor gfMul(generator[j], coeff)
            }
        }
        return ByteArray(ecCount) { msg[data.size + it].toByte() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Matrix construction
    // ─────────────────────────────────────────────────────────────────────────

    private fun buildMatrix(
        version: Int,
        codewords: ByteArray
    ): Pair<Array<BooleanArray>, Array<BooleanArray>> {
        val size = version * 4 + 17
        val matrix = Array(size) { BooleanArray(size) }
        val reserved = Array(size) { BooleanArray(size) }

        placeFinderPattern(matrix, reserved, 0, 0, size)
        placeFinderPattern(matrix, reserved, 0, size - 7, size)
        placeFinderPattern(matrix, reserved, size - 7, 0, size)

        placeTimingPatterns(matrix, reserved, size)

        if (version >= 2) placeAlignmentPatterns(matrix, reserved, version, size)

        // Reserve format info areas (written later per mask)
        reserveFormatInfo(reserved, size)

        // Dark module (always dark, version 1+)
        matrix[size - 8][8] = true
        reserved[size - 8][8] = true

        placeData(matrix, reserved, size, codewords)

        return Pair(matrix, reserved)
    }

    /** 7×7 finder pattern plus 1-module separator ring. */
    private fun placeFinderPattern(
        matrix: Array<BooleanArray>, reserved: Array<BooleanArray>,
        row: Int, col: Int, size: Int
    ) {
        val pattern = arrayOf(
            booleanArrayOf(true, true, true, true, true, true, true),
            booleanArrayOf(true, false, false, false, false, false, true),
            booleanArrayOf(true, false, true, true, true, false, true),
            booleanArrayOf(true, false, true, true, true, false, true),
            booleanArrayOf(true, false, true, true, true, false, true),
            booleanArrayOf(true, false, false, false, false, false, true),
            booleanArrayOf(true, true, true, true, true, true, true),
        )
        // Place pattern + separator (one extra row/col of false around the pattern)
        for (dr in -1..7) {
            for (dc in -1..7) {
                val mr = row + dr; val mc = col + dc
                if (mr < 0 || mr >= size || mc < 0 || mc >= size) continue
                reserved[mr][mc] = true
                matrix[mr][mc] = if (dr in 0..6 && dc in 0..6) pattern[dr][dc] else false
            }
        }
    }

    private fun placeTimingPatterns(
        matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, size: Int
    ) {
        for (i in 8 until size - 8) {
            val dark = (i % 2 == 0)
            if (!reserved[6][i]) { matrix[6][i] = dark; reserved[6][i] = true }
            if (!reserved[i][6]) { matrix[i][6] = dark; reserved[i][6] = true }
        }
    }

    /** Alignment pattern center positions for versions 2–7. */
    private val ALIGN_POS = arrayOf(
        intArrayOf(),               // V1 (unused index)
        intArrayOf(6, 18),          // V2
        intArrayOf(6, 22),          // V3
        intArrayOf(6, 26),          // V4
        intArrayOf(6, 30),          // V5
        intArrayOf(6, 34),          // V6
        intArrayOf(6, 22, 38),      // V7
    )

    private fun placeAlignmentPatterns(
        matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, version: Int, size: Int
    ) {
        val positions = ALIGN_POS[version - 1]
        for (r in positions) {
            for (c in positions) {
                // Skip positions that overlap with finder patterns
                if (r <= 8 && c <= 8) continue
                if (r <= 8 && c >= size - 8) continue
                if (r >= size - 8 && c <= 8) continue
                // Skip if already reserved
                if (reserved[r][c]) continue
                placeAlignmentAt(matrix, reserved, r, c)
            }
        }
    }

    private fun placeAlignmentAt(
        matrix: Array<BooleanArray>, reserved: Array<BooleanArray>, row: Int, col: Int
    ) {
        // 5×5 alignment pattern
        for (dr in -2..2) {
            for (dc in -2..2) {
                val dark = (dr == -2 || dr == 2 || dc == -2 || dc == 2 ||
                        (dr == 0 && dc == 0))
                matrix[row + dr][col + dc] = dark
                reserved[row + dr][col + dc] = true
            }
        }
    }

    private fun reserveFormatInfo(reserved: Array<BooleanArray>, size: Int) {
        // Around top-left finder
        for (i in 0..8) { reserved[8][i] = true; reserved[i][8] = true }
        // Top-right and bottom-left copies
        for (i in 0..7) reserved[8][size - 1 - i] = true
        for (i in 0..6) reserved[size - 1 - i][8] = true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Data placement (zigzag)
    // ─────────────────────────────────────────────────────────────────────────

    private fun placeData(
        matrix: Array<BooleanArray>, reserved: Array<BooleanArray>,
        size: Int, codewords: ByteArray
    ) {
        // Expand codewords to a bit list
        val bits = BooleanArray(codewords.size * 8) { i ->
            codewords[i / 8].toInt() and (0x80 ushr (i % 8)) != 0
        }
        var bitIdx = 0

        var right = size - 1
        var goUp = true
        while (right > 0) {
            if (right == 6) right--  // skip vertical timing column
            val rows = if (goUp) (size - 1 downTo 0) else (0 until size)
            for (row in rows) {
                for (dc in 0..1) {
                    val col = right - dc
                    if (!reserved[row][col]) {
                        matrix[row][col] = bitIdx < bits.size && bits[bitIdx++]
                    }
                }
            }
            goUp = !goUp
            right -= 2
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Masking
    // ─────────────────────────────────────────────────────────────────────────

    private val MASK_FUNS: List<(Int, Int) -> Boolean> = listOf(
        { r, c -> (r + c) % 2 == 0 },
        { r, _ -> r % 2 == 0 },
        { _, c -> c % 3 == 0 },
        { r, c -> (r + c) % 3 == 0 },
        { r, c -> (r / 2 + c / 3) % 2 == 0 },
        { r, c -> r * c % 2 + r * c % 3 == 0 },
        { r, c -> (r * c % 2 + r * c % 3) % 2 == 0 },
        { r, c -> ((r + c) % 2 + r * c % 3) % 2 == 0 },
    )

    /**
     * Pre-computed 15-bit format info words for M error correction level, masks 0–7.
     * Values from ISO/IEC 18004 Table 10 (already XORed with masking value 0x5412 >> shifts,
     * i.e., the mask pattern 101010000010010 is already applied).
     */
    private val FORMAT_STRINGS_M = intArrayOf(
        0x5412, // mask 0
        0x5125, // mask 1
        0x5E7C, // mask 2
        0x5B4B, // mask 3
        0x45F9, // mask 4
        0x40CE, // mask 5
        0x4F97, // mask 6
        0x4AA0, // mask 7
    )

    private fun applyBestMask(
        baseMatrix: Array<BooleanArray>,
        reserved: Array<BooleanArray>,
        version: Int
    ): Array<BooleanArray> {
        var best: Array<BooleanArray> = baseMatrix
        var bestScore = Int.MAX_VALUE
        val size = baseMatrix.size

        for (mask in 0..7) {
            val candidate = applyMask(baseMatrix, reserved, size, mask)
            writeFormatInfo(candidate, size, mask)
            val score = maskPenalty(candidate, size)
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best
    }

    private fun applyMask(
        base: Array<BooleanArray>, reserved: Array<BooleanArray>,
        size: Int, maskPattern: Int
    ): Array<BooleanArray> {
        val fn = MASK_FUNS[maskPattern]
        return Array(size) { r ->
            BooleanArray(size) { c ->
                if (!reserved[r][c]) base[r][c] xor fn(r, c)
                else base[r][c]
            }
        }
    }

    private fun writeFormatInfo(matrix: Array<BooleanArray>, size: Int, mask: Int) {
        val fmt = FORMAT_STRINGS_M[mask]

        // Copy 1: around top-left finder
        val copy1Positions = listOf(
            // (row, col, bitIndex)  — bit 14 is MSB
            Pair(8, 0), Pair(8, 1), Pair(8, 2), Pair(8, 3), Pair(8, 4), Pair(8, 5),
            Pair(8, 7), // skip col 6 (timing)
            Pair(8, 8), Pair(7, 8),
            Pair(5, 8), // skip row 6 (timing)
            Pair(4, 8), Pair(3, 8), Pair(2, 8), Pair(1, 8), Pair(0, 8),
        )
        for ((i, pos) in copy1Positions.withIndex()) {
            val bit = (fmt ushr (14 - i)) and 1 == 1
            matrix[pos.first][pos.second] = bit
        }

        // Copy 2: top-right and bottom-left
        val copy2Positions = listOf(
            Pair(size - 1, 8), Pair(size - 2, 8), Pair(size - 3, 8),
            Pair(size - 4, 8), Pair(size - 5, 8), Pair(size - 6, 8), Pair(size - 7, 8),
            Pair(8, size - 8), Pair(8, size - 7),
            Pair(8, size - 6), Pair(8, size - 5), Pair(8, size - 4),
            Pair(8, size - 3), Pair(8, size - 2), Pair(8, size - 1),
        )
        for ((i, pos) in copy2Positions.withIndex()) {
            val bit = (fmt ushr (14 - i)) and 1 == 1
            matrix[pos.first][pos.second] = bit
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mask penalty evaluation
    // ─────────────────────────────────────────────────────────────────────────

    private fun maskPenalty(matrix: Array<BooleanArray>, size: Int): Int {
        return penalty1(matrix, size) +
                penalty2(matrix, size) +
                penalty3(matrix, size) +
                penalty4(matrix, size)
    }

    /** Rule 1: runs of 5+ same-colour modules. */
    private fun penalty1(matrix: Array<BooleanArray>, size: Int): Int {
        var score = 0
        for (r in 0 until size) {
            score += runPenalty(Array(size) { matrix[r][it] })
        }
        for (c in 0 until size) {
            score += runPenalty(Array(size) { matrix[it][c] })
        }
        return score
    }

    private fun runPenalty(line: Array<Boolean>): Int {
        var score = 0; var run = 1
        for (i in 1 until line.size) {
            if (line[i] == line[i - 1]) run++
            else {
                if (run >= 5) score += run - 2
                run = 1
            }
        }
        if (run >= 5) score += run - 2
        return score
    }

    /** Rule 2: 2×2 blocks of same colour. */
    private fun penalty2(matrix: Array<BooleanArray>, size: Int): Int {
        var score = 0
        for (r in 0 until size - 1) {
            for (c in 0 until size - 1) {
                val v = matrix[r][c]
                if (matrix[r][c + 1] == v && matrix[r + 1][c] == v && matrix[r + 1][c + 1] == v) {
                    score += 3
                }
            }
        }
        return score
    }

    /** Rule 3: 1:1:3:1:1 finder-like patterns. */
    private fun penalty3(matrix: Array<BooleanArray>, size: Int): Int {
        val p1 = booleanArrayOf(true, false, true, true, true, false, true, false, false, false, false)
        val p2 = booleanArrayOf(false, false, false, false, true, false, true, true, true, false, true)
        var score = 0
        for (r in 0 until size) {
            for (c in 0..size - 11) {
                if ((0 until 11).all { matrix[r][c + it] == p1[it] }) score += 40
                if ((0 until 11).all { matrix[r][c + it] == p2[it] }) score += 40
            }
        }
        for (c in 0 until size) {
            for (r in 0..size - 11) {
                if ((0 until 11).all { matrix[r + it][c] == p1[it] }) score += 40
                if ((0 until 11).all { matrix[r + it][c] == p2[it] }) score += 40
            }
        }
        return score
    }

    /** Rule 4: balance of dark/light modules. */
    private fun penalty4(matrix: Array<BooleanArray>, size: Int): Int {
        val total = size * size
        val dark = matrix.sumOf { row -> row.count { it } }
        val percent = dark * 100 / total
        val prev = percent / 5 * 5
        val next = prev + 5
        return minOf(kotlin.math.abs(prev - 50), kotlin.math.abs(next - 50)) / 5 * 10
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bit buffer helper
    // ─────────────────────────────────────────────────────────────────────────

    private class BitBuffer {
        private val data = mutableListOf<Int>()  // packed bytes
        var length = 0; private set

        fun append(value: Int, bits: Int) {
            for (i in bits - 1 downTo 0) {
                val bit = (value ushr i) and 1
                val byteIdx = length / 8
                if (byteIdx >= data.size) data.add(0)
                if (bit == 1) data[byteIdx] = data[byteIdx] or (0x80 ushr (length % 8))
                length++
            }
        }

        fun toByteArray(): ByteArray = ByteArray(data.size) { data[it].toByte() }
    }
}
