package de.egril.defender.mapgen

/**
 * Platform-independent PNG encoding.
 * Encodes pixel data to PNG bytes without external dependencies.
 */
expect object MapImageEncoder {
    /**
     * Encode pixel array to PNG bytes.
     * @param pixels ARGB int array (row-major)
     * @param width Image width
     * @param height Image height
     * @return PNG-encoded bytes, or null if encoding fails
     */
    fun encodeToPng(pixels: IntArray, width: Int, height: Int): ByteArray?
}
