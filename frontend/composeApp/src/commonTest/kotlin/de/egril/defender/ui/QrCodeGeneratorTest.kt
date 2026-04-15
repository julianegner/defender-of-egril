package de.egril.defender.ui

import de.egril.defender.ui.common.QrCodeGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QrCodeGeneratorTest {

    @Test
    fun generatesCorrectMatrixSizeForVersion1() {
        // 3 bytes → version 1 M (capacity 16 bytes)
        val matrix = QrCodeGenerator.generate("hi!")
        assertEquals(21, matrix.size)
        assertTrue(matrix.all { it.size == 21 })
    }

    @Test
    fun generatesCorrectMatrixSizeForVersion4() {
        // ~50-char URL → version 4 M (capacity 64 bytes)
        val url = "https://auth.egril.de/realms/egril/device?user_code=ABCD"
        val matrix = QrCodeGenerator.generate(url)
        val version = (matrix.size - 17) / 4
        assertTrue(version >= 3, "Expected version >= 3 for 50-char URL, got $version")
        assertEquals(version * 4 + 17, matrix.size)
        assertTrue(matrix.all { it.size == matrix.size })
    }

    @Test
    fun generatesCorrectMatrixSizeForTypicalKeycloakUrl() {
        // Typical verification_uri_complete from Keycloak
        val url = "https://auth.egril.de/realms/egril/device?user_code=WXYZ-1234"
        val matrix = QrCodeGenerator.generate(url)
        val size = matrix.size
        assertTrue(size == 21 || size == 25 || size == 29 || size == 33 || size == 37,
            "Unexpected matrix size $size")
    }

    @Test
    fun finderPatternTopLeftIsCorrect() {
        val matrix = QrCodeGenerator.generate("test")
        // Top-left finder: 7×7 dark border, inner 5×5 with light inner ring and dark center
        // Row 0, cols 0-6 must be all dark
        assertTrue((0..6).all { matrix[0][it] }, "Top row of finder should be dark")
        // Row 0 and row 6 fully dark
        assertTrue((0..6).all { matrix[6][it] }, "Bottom row of finder should be dark")
        // Col 0 and col 6 fully dark
        assertTrue((0..6).all { matrix[it][0] }, "Left col of finder should be dark")
        assertTrue((0..6).all { matrix[it][6] }, "Right col of finder should be dark")
        // Inner ring (row 1-5, col 1-5) should have light corners
        assertTrue(!matrix[1][1], "Inner ring top-left should be light")
        assertTrue(!matrix[1][5], "Inner ring top-right should be light")
        assertTrue(!matrix[5][1], "Inner ring bottom-left should be light")
        assertTrue(!matrix[5][5], "Inner ring bottom-right should be light")
        // Center 3×3 dark block
        assertTrue(matrix[2][2], "3×3 center top-left should be dark")
        assertTrue(matrix[4][4], "3×3 center bottom-right should be dark")
        assertTrue(matrix[3][3], "Centre module should be dark")
    }

    @Test
    fun failsGracefullyForTooLongInput() {
        val tooLong = "a".repeat(130)
        assertFailsWith<IllegalStateException> {
            QrCodeGenerator.generate(tooLong)
        }
    }
}
