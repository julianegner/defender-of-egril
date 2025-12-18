@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.files.Blob
import kotlin.js.JsExport

/**
 * Simple ZIP file writer for Web/WASM platform
 * Creates uncompressed ZIP files (STORE method)
 * 
 * ZIP file format specification:
 * - Local file header + file data for each file
 * - Central directory entries for all files
 * - End of central directory record
 */
class ZipWriter {
    private val files = mutableListOf<ZipEntry>()
    
    data class ZipEntry(
        val filename: String,
        val content: ByteArray,
        val crc32: Long,
        val localHeaderOffset: Int
    )
    
    /**
     * Add a file to the ZIP archive
     */
    fun addFile(filename: String, content: String) {
        val bytes = content.encodeToByteArray()
        val crc = calculateCRC32(bytes)
        files.add(ZipEntry(filename, bytes, crc, 0))
    }
    
    /**
     * Build the complete ZIP file as a byte array
     */
    fun build(): ByteArray {
        val output = mutableListOf<Byte>()
        val updatedFiles = mutableListOf<ZipEntry>()
        
        // Write local file headers and file data
        for (entry in files) {
            val offset = output.size
            writeLocalFileHeader(output, entry)
            output.addAll(entry.content.toList())
            updatedFiles.add(entry.copy(localHeaderOffset = offset))
        }
        
        // Remember where central directory starts
        val centralDirOffset = output.size
        
        // Write central directory entries
        for (entry in updatedFiles) {
            writeCentralDirectoryEntry(output, entry)
        }
        
        val centralDirSize = output.size - centralDirOffset
        
        // Write end of central directory record
        writeEndOfCentralDirectory(output, updatedFiles.size, centralDirSize, centralDirOffset)
        
        return output.toByteArray()
    }
    
    private fun writeLocalFileHeader(output: MutableList<Byte>, entry: ZipEntry) {
        // Local file header signature (0x04034b50)
        writeInt(output, 0x04034b50)
        
        // Version needed to extract (2.0)
        writeShort(output, 20)
        
        // General purpose bit flag (bit 11 = UTF-8 filenames)
        writeShort(output, 0x0800)
        
        // Compression method (0 = STORE, no compression)
        writeShort(output, 0)
        
        // Last mod file time and date (use dummy values)
        writeShort(output, 0x0021) // Time: 00:00:02
        writeShort(output, 0x0021) // Date: 1980-01-01
        
        // CRC-32
        writeInt(output, entry.crc32.toInt())
        
        // Compressed size
        writeInt(output, entry.content.size)
        
        // Uncompressed size
        writeInt(output, entry.content.size)
        
        // File name length
        val filenameBytes = entry.filename.encodeToByteArray()
        writeShort(output, filenameBytes.size)
        
        // Extra field length
        writeShort(output, 0)
        
        // File name
        output.addAll(filenameBytes.toList())
    }
    
    private fun writeCentralDirectoryEntry(output: MutableList<Byte>, entry: ZipEntry) {
        // Central directory file header signature (0x02014b50)
        writeInt(output, 0x02014b50)
        
        // Version made by (UNIX, version 2.0)
        writeShort(output, 0x0314)
        
        // Version needed to extract (2.0)
        writeShort(output, 20)
        
        // General purpose bit flag (bit 11 = UTF-8 filenames)
        writeShort(output, 0x0800)
        
        // Compression method (0 = STORE)
        writeShort(output, 0)
        
        // Last mod file time and date
        writeShort(output, 0x0021)
        writeShort(output, 0x0021)
        
        // CRC-32
        writeInt(output, entry.crc32.toInt())
        
        // Compressed size
        writeInt(output, entry.content.size)
        
        // Uncompressed size
        writeInt(output, entry.content.size)
        
        // File name length
        val filenameBytes = entry.filename.encodeToByteArray()
        writeShort(output, filenameBytes.size)
        
        // Extra field length
        writeShort(output, 0)
        
        // File comment length
        writeShort(output, 0)
        
        // Disk number start
        writeShort(output, 0)
        
        // Internal file attributes
        writeShort(output, 0)
        
        // External file attributes
        writeInt(output, 0)
        
        // Relative offset of local header
        writeInt(output, entry.localHeaderOffset)
        
        // File name
        output.addAll(filenameBytes.toList())
    }
    
    private fun writeEndOfCentralDirectory(
        output: MutableList<Byte>,
        fileCount: Int,
        centralDirSize: Int,
        centralDirOffset: Int
    ) {
        // End of central directory signature (0x06054b50)
        writeInt(output, 0x06054b50)
        
        // Number of this disk
        writeShort(output, 0)
        
        // Disk where central directory starts
        writeShort(output, 0)
        
        // Number of central directory records on this disk
        writeShort(output, fileCount)
        
        // Total number of central directory records
        writeShort(output, fileCount)
        
        // Size of central directory
        writeInt(output, centralDirSize)
        
        // Offset of start of central directory
        writeInt(output, centralDirOffset)
        
        // ZIP file comment length
        writeShort(output, 0)
    }
    
    private fun writeShort(output: MutableList<Byte>, value: Int) {
        output.add((value and 0xFF).toByte())
        output.add(((value shr 8) and 0xFF).toByte())
    }
    
    private fun writeInt(output: MutableList<Byte>, value: Int) {
        output.add((value and 0xFF).toByte())
        output.add(((value shr 8) and 0xFF).toByte())
        output.add(((value shr 16) and 0xFF).toByte())
        output.add(((value shr 24) and 0xFF).toByte())
    }
    
    /**
     * Calculate CRC-32 checksum for a byte array
     * Uses the standard CRC-32 algorithm (IEEE 802.3 polynomial)
     */
    private fun calculateCRC32(data: ByteArray): Long {
        var crc = 0xFFFFFFFF.toInt()
        
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0 until 8) {
                crc = if ((crc and 1) != 0) {
                    (crc ushr 1) xor 0xEDB88320.toInt()
                } else {
                    crc ushr 1
                }
            }
        }
        
        return (crc xor 0xFFFFFFFF.toInt()).toLong() and 0xFFFFFFFFL
    }
}

/**
 * External JS function to create a Blob from a byte array
 */
@JsFun("(bytes) => new Blob([bytes], {type: 'application/zip'})")
external fun createBlobFromBytes(bytes: Uint8Array): Blob

/**
 * Helper function to convert ByteArray to Uint8Array for JavaScript interop
 */
fun ByteArray.toUint8Array(): Uint8Array {
    val array = Uint8Array(this.size)
    for (i in this.indices) {
        array[i] = this[i]
    }
    return array
}
