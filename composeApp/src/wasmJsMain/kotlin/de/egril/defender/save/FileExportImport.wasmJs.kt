@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.egril.defender.save

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.url.URL
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
            // For web, we use JSZip library via external declaration
            // Since we don't have JSZip, we'll create a simple implementation
            // that saves files individually or implement a minimal ZIP creator
            
            // For now, create a simple text file with all saves concatenated
            // This is a fallback - ideally we'd use JSZip library
            val zipContent = buildString {
                files.forEach { (filename, content) ->
                    appendLine("=== FILE: $filename ===")
                    appendLine(content)
                    appendLine()
                }
            }
            
            val blob = createBlob(zipContent, "application/zip")
            val url = URL.createObjectURL(blob)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = url
            link.download = zipFilename
            link.style.display = "none"
            
            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)
            
            URL.revokeObjectURL(url)
            
            true
        } catch (e: Exception) {
            println("Error exporting ZIP: ${e.message}")
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
                            val reader = FileReader()
                            reader.onload = {
                                val content = reader.result as String
                                val filename = file.name
                                
                                if (filename.endsWith(".json", ignoreCase = true)) {
                                    importedFiles.add(ImportedFile(filename, content))
                                }
                                // For ZIP files, we'd need to parse them
                                // For now, we skip ZIP handling in browser
                                
                                processed++
                                if (processed == total) {
                                    document.body?.removeChild(input)
                                    continuation.resume(importedFiles)
                                }
                            }
                            reader.onerror = {
                                processed++
                                if (processed == total) {
                                    document.body?.removeChild(input)
                                    continuation.resume(importedFiles)
                                }
                            }
                            reader.readAsText(file)
                        } else {
                            processed++
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
