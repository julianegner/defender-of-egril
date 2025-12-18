@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

/**
 * Simple ZIP file reader for Web/WASM platform
 * Reads uncompressed ZIP files (STORE method)
 * 
 * ZIP file format specification:
 * - Local file header + file data for each file
 * - Central directory entries for all files
 * - End of central directory record
 */
class ZipReader(private val zipData: ByteArray) {
    
    /**
     * Represents a file entry in the ZIP archive
     */
    data class ZipEntry(
        val filename: String,
        val content: ByteArray
    )
    
    /**
     * Extract all files from the ZIP archive
     * @return List of ZipEntry objects, or empty list if parsing fails
     */
    fun extractAll(): List<ZipEntry> {
        return try {
            val entries = mutableListOf<ZipEntry>()
            
            // Find the end of central directory record
            val eocdrOffset = findEndOfCentralDirectory() ?: return emptyList()
            
            // Read end of central directory
            val centralDirOffset = readInt(eocdrOffset + 16)
            val numEntries = readShort(eocdrOffset + 10)
            
            // Read central directory entries to get local header offsets
            var cdOffset = centralDirOffset
            for (i in 0 until numEntries) {
                val signature = readInt(cdOffset)
                if (signature != 0x02014b50) {
                    println("Invalid central directory signature: 0x${signature.toString(16)}")
                    break
                }
                
                // Read metadata
                val filenameLength = readShort(cdOffset + 28)
                val extraFieldLength = readShort(cdOffset + 30)
                val commentLength = readShort(cdOffset + 32)
                val localHeaderOffset = readInt(cdOffset + 42)
                
                // Read filename
                val filename = readString(cdOffset + 46, filenameLength)
                
                // Extract file content from local header
                val content = extractFileFromLocalHeader(localHeaderOffset)
                if (content != null) {
                    entries.add(ZipEntry(filename, content))
                }
                
                // Move to next central directory entry
                cdOffset += 46 + filenameLength + extraFieldLength + commentLength
            }
            
            entries
        } catch (e: Exception) {
            println("Error extracting ZIP: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Find the end of central directory record by scanning from the end of the file
     */
    private fun findEndOfCentralDirectory(): Int? {
        // EOCD signature: 0x06054b50
        val signature = byteArrayOf(0x50, 0x4b, 0x05, 0x06)
        
        // Scan from end of file (EOCD is typically at the end)
        // Maximum comment length is 65535, so we only need to check last 65KB
        val searchStart = maxOf(0, zipData.size - 65536)
        
        for (i in zipData.size - 22 downTo searchStart) {
            if (i + 3 < zipData.size &&
                zipData[i] == signature[0] &&
                zipData[i + 1] == signature[1] &&
                zipData[i + 2] == signature[2] &&
                zipData[i + 3] == signature[3]) {
                return i
            }
        }
        
        return null
    }
    
    /**
     * Extract file content from local file header
     */
    private fun extractFileFromLocalHeader(offset: Int): ByteArray? {
        try {
            val signature = readInt(offset)
            if (signature != 0x04034b50) {
                println("Invalid local file header signature at offset $offset: 0x${signature.toString(16)}")
                return null
            }
            
            // Read compression method
            val compressionMethod = readShort(offset + 8)
            if (compressionMethod != 0) {
                println("Compressed files not supported (compression method: $compressionMethod)")
                return null
            }
            
            // Read sizes
            val compressedSize = readInt(offset + 18)
            val filenameLength = readShort(offset + 26)
            val extraFieldLength = readShort(offset + 28)
            
            // Calculate data offset
            val dataOffset = offset + 30 + filenameLength + extraFieldLength
            
            // Extract file data
            if (dataOffset + compressedSize > zipData.size) {
                println("File data extends beyond ZIP file bounds")
                return null
            }
            
            return zipData.copyOfRange(dataOffset, dataOffset + compressedSize)
        } catch (e: Exception) {
            println("Error extracting file from local header: ${e.message}")
            return null
        }
    }
    
    /**
     * Read a 16-bit little-endian integer
     */
    private fun readShort(offset: Int): Int {
        if (offset + 1 >= zipData.size) return 0
        return (zipData[offset].toInt() and 0xFF) or
               ((zipData[offset + 1].toInt() and 0xFF) shl 8)
    }
    
    /**
     * Read a 32-bit little-endian integer
     */
    private fun readInt(offset: Int): Int {
        if (offset + 3 >= zipData.size) return 0
        return (zipData[offset].toInt() and 0xFF) or
               ((zipData[offset + 1].toInt() and 0xFF) shl 8) or
               ((zipData[offset + 2].toInt() and 0xFF) shl 16) or
               ((zipData[offset + 3].toInt() and 0xFF) shl 24)
    }
    
    /**
     * Read a UTF-8 string
     */
    private fun readString(offset: Int, length: Int): String {
        if (offset + length > zipData.size) return ""
        return zipData.copyOfRange(offset, offset + length).decodeToString()
    }
}

/**
 * Helper function to convert Uint8Array to ByteArray for ZIP parsing
 */
fun Uint8Array.toByteArray(): ByteArray {
    val array = ByteArray(this.length)
    for (i in 0 until this.length) {
        array[i] = this[i]
    }
    return array
}
