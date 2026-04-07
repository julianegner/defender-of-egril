@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FileReader
import kotlin.coroutines.resume

// External JS functions for Blob creation
@JsFun("(content, type) => new Blob([content], {type: type})")
external fun createBlob(content: String, type: String): Blob

/**
 * Web/WASM implementation of FileExportImport using browser File API
 */
class WasmJsFileExportImport : FileExportImport {
    
    override suspend fun exportFile(filename: String, content: String): Boolean {
        return try {
            // Create a blob from the content
            val blob = createBlob(content, "application/json")
            
            // Create a download link
            val url = URL.createObjectURL(blob)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = url
            link.download = filename
            link.style.display = "none"
            
            // Trigger download
            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)
            
            // Clean up
            URL.revokeObjectURL(url)
            
            true
        } catch (e: Exception) {
            println("Error exporting file: ${e.message}")
            false
        }
    }
    
    override suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean {
        return try {
            // Create a ZIP file using our custom ZipWriter
            val zipWriter = ZipWriter()
            
            // Add all files to the ZIP
            files.forEach { (filename, content) ->
                zipWriter.addFile(filename, content)
            }
            
            // Build the ZIP file as a byte array
            val zipBytes = zipWriter.build()
            
            // Convert to Uint8Array for JavaScript interop
            val uint8Array = zipBytes.toUint8Array()
            
            // Create a blob from the byte array
            val blob = createBlobFromBytes(uint8Array)
            
            // Create a download link
            val url = URL.createObjectURL(blob)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = url
            link.download = zipFilename
            link.style.display = "none"
            
            // Trigger download
            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)
            
            // Clean up
            URL.revokeObjectURL(url)
            
            true
        } catch (e: Exception) {
            println("Error exporting ZIP: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    override suspend fun importFiles(): List<ImportedFile>? = suspendCancellableCoroutine { continuation ->
        try {
            // Create file input element
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.multiple = true
            input.accept = ".json,.zip"
            input.style.display = "none"
            
            input.onchange = { event ->
                val files = input.files
                if (files != null && files.length > 0) {
                    val importedFiles = mutableListOf<ImportedFile>()
                    var processed = 0
                    val total = files.length
                    
                    for (i in 0 until total) {
                        val file = files.item(i)
                        if (file != null) {
                            val filename = file.name
                            
                            when {
                                filename.endsWith(".json", ignoreCase = true) -> {
                                    // Read JSON file as text
                                    val reader = FileReader()
                                    reader.onload = {
                                        try {
                                            // FileReader.result is Any? in Kotlin/Wasm, use toString()
                                            val content = reader.result?.toString() ?: ""
                                            if (content.isNotEmpty()) {
                                                importedFiles.add(ImportedFile(filename, content))
                                            } else {
                                                println("Empty content for JSON file $filename")
                                            }
                                        } catch (e: Exception) {
                                            println("Error reading JSON file $filename: ${e.message}")
                                            e.printStackTrace()
                                        }
                                        processed++
                                        if (processed == total) {
                                            document.body?.removeChild(input)
                                            continuation.resume(importedFiles)
                                        }
                                    }
                                    reader.onerror = {
                                        println("Error reading file $filename")
                                        processed++
                                        if (processed == total) {
                                            document.body?.removeChild(input)
                                            continuation.resume(importedFiles)
                                        }
                                    }
                                    reader.readAsText(file)
                                }
                                filename.endsWith(".zip", ignoreCase = true) -> {
                                    // Read ZIP file as binary
                                    val reader = FileReader()
                                    reader.onload = {
                                        try {
                                            // FileReader.result is Any? in Kotlin/Wasm, need to cast carefully
                                            val result = reader.result
                                            if (result != null) {
                                                @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
                                                val arrayBuffer = result as ArrayBuffer
                                                val uint8Array = Uint8Array(arrayBuffer)
                                                val byteArray = uint8Array.toByteArray()
                                                
                                                // Parse ZIP file
                                                val zipReader = ZipReader(byteArray)
                                                val entries = zipReader.extractAll()
                                                
                                                // Add all JSON files from the ZIP
                                                entries.forEach { entry ->
                                                    if (entry.filename.endsWith(".json", ignoreCase = true)) {
                                                        val content = entry.content.decodeToString()
                                                        // Use only the filename without path
                                                        val entryFilename = entry.filename
                                                            .replace('\\', '/')
                                                            .substringAfterLast('/')
                                                            .takeIf { it.isNotEmpty() } ?: "unknown.json"
                                                        importedFiles.add(ImportedFile(entryFilename, content))
                                                    }
                                                }
                                            } else {
                                                println("No result from FileReader for ZIP file $filename")
                                            }
                                        } catch (e: Exception) {
                                            println("Error parsing ZIP file $filename: ${e.message}")
                                            e.printStackTrace()
                                        }
                                        processed++
                                        if (processed == total) {
                                            document.body?.removeChild(input)
                                            continuation.resume(importedFiles)
                                        }
                                    }
                                    reader.onerror = {
                                        println("Error reading ZIP file $filename")
                                        processed++
                                        if (processed == total) {
                                            document.body?.removeChild(input)
                                            continuation.resume(importedFiles)
                                        }
                                    }
                                    reader.readAsArrayBuffer(file)
                                }
                                else -> {
                                    // Unsupported file type
                                    println("Skipping unsupported file: $filename")
                                    processed++
                                    if (processed == total) {
                                        document.body?.removeChild(input)
                                        continuation.resume(importedFiles)
                                    }
                                }
                            }
                        } else {
                            processed++
                            if (processed == total) {
                                document.body?.removeChild(input)
                                continuation.resume(importedFiles)
                            }
                        }
                    }
                } else {
                    document.body?.removeChild(input)
                    continuation.resume(null)
                }
            }
            
            input.oncancel = {
                document.body?.removeChild(input)
                continuation.resume(null)
            }
            
            document.body?.appendChild(input)
            input.click()
            
        } catch (e: Exception) {
            println("Error importing files: ${e.message}")
            continuation.resume(null)
        }
    }
}

actual fun getFileExportImport(): FileExportImport = WasmJsFileExportImport()
