package de.egril.defender.save

import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * iOS implementation of FileExportImport using UIDocumentPickerViewController
 * 
 * Note: This uses UIActivityViewController for sharing/exporting files and 
 * UIDocumentPickerViewController for importing files. This approach works well
 * with iOS's native file sharing mechanisms.
 */
class IosFileExportImport : FileExportImport {
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun exportFile(filename: String, content: String): Boolean = withContext(Dispatchers.Main) {
        try {
            // Write content to temporary file
            val tempDir = NSTemporaryDirectory()
            val tempFilePath = "$tempDir$filename"
            val tempFileURL = NSURL.fileURLWithPath(tempFilePath)
            
            // Write content to temporary file
            (content as NSString).writeToFile(
                tempFilePath,
                atomically = true,
                encoding = NSUTF8StringEncoding,
                error = null
            )
            
            // Use UIActivityViewController for sharing the file
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val activityViewController = UIActivityViewController(
                    activityItems = listOf(tempFileURL),
                    applicationActivities = null
                )
                
                // For iPad, need to set popover presentation controller
                activityViewController.popoverPresentationController?.sourceView = rootViewController.view
                
                rootViewController.presentViewController(activityViewController, animated = true, completion = null)
                
                // Clean up will happen when activity controller is dismissed
                // For simplicity, we'll just return true
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error exporting file: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean = withContext(Dispatchers.Main) {
        try {
            // Create ZIP file in temporary directory
            val tempDir = NSTemporaryDirectory()
            val tempZipPath = "$tempDir$zipFilename"
            val tempZipURL = NSURL.fileURLWithPath(tempZipPath)
            
            // Create ZIP-like archive
            withContext(Dispatchers.Default) {
                createZipFile(tempZipPath, files)
            }
            
            // Use UIActivityViewController for sharing the ZIP file
            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            if (rootViewController != null) {
                val activityViewController = UIActivityViewController(
                    activityItems = listOf(tempZipURL),
                    applicationActivities = null
                )
                
                // For iPad, need to set popover presentation controller
                activityViewController.popoverPresentationController?.sourceView = rootViewController.view
                
                rootViewController.presentViewController(activityViewController, animated = true, completion = null)
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            println("Error exporting ZIP: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun importFiles(): List<ImportedFile>? = withContext(Dispatchers.Main) {
        try {
            // Present document picker for import
            suspendCancellableCoroutine<List<NSURL>?> { continuation ->
                val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
                if (rootViewController != null) {
                    val documentPicker = UIDocumentPickerViewController(
                        forOpeningContentTypes = listOf(UTTypeJSON, UTTypeZIP),
                        asCopy = true
                    )
                    documentPicker.allowsMultipleSelection = true
                    
                    val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
                        override fun documentPicker(
                            controller: UIDocumentPickerViewController,
                            didPickDocumentsAtURLs: List<*>
                        ) {
                            @Suppress("UNCHECKED_CAST")
                            val urls = didPickDocumentsAtURLs as List<NSURL>
                            continuation.resume(urls)
                        }
                        
                        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                            continuation.resume(null)
                        }
                    }
                    
                    documentPicker.setDelegate(delegate)
                    rootViewController.presentViewController(documentPicker, animated = true, completion = null)
                } else {
                    continuation.resume(null)
                }
            }?.let { urls ->
                // Process selected files
                withContext(Dispatchers.Default) {
                    processImportedFiles(urls)
                }
            }
        } catch (e: Exception) {
            println("Error importing files: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun createZipFile(zipPath: String, files: Map<String, String>) {
        // Create a simple concatenated file with markers
        // Note: For proper ZIP support, would need to integrate with
        // Apple's Compression framework or a third-party library
        
        val zipContent = buildString {
            files.forEach { (filename, content) ->
                appendLine("=== FILE: $filename ===")
                appendLine(content)
                appendLine()
            }
        }
        
        (zipContent as NSString).writeToFile(
            zipPath,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
    }
    
    @OptIn(ExperimentalForeignApi::class)
    private fun processImportedFiles(urls: List<NSURL>): List<ImportedFile> {
        val results = mutableListOf<ImportedFile>()
        
        urls.forEach { url ->
            try {
                val filename = url.lastPathComponent ?: "unknown"
                val path = url.path ?: return@forEach
                
                when {
                    filename.endsWith(".json", ignoreCase = true) -> {
                        // Read JSON file
                        val content = NSString.stringWithContentsOfFile(
                            path,
                            encoding = NSUTF8StringEncoding,
                            error = null
                        ) as? String
                        
                        if (content != null) {
                            results.add(ImportedFile(filename, content))
                        }
                    }
                    filename.endsWith(".zip", ignoreCase = true) -> {
                        // Try to read as our simple concatenated format
                        val content = NSString.stringWithContentsOfFile(
                            path,
                            encoding = NSUTF8StringEncoding,
                            error = null
                        ) as? String
                        
                        if (content != null) {
                            // Parse the simple format
                            val lines = content.lines()
                            var currentFilename: String? = null
                            val currentContent = StringBuilder()
                            
                            lines.forEach { line ->
                                when {
                                    line.startsWith("=== FILE: ") -> {
                                        // Save previous file if exists
                                        if (currentFilename != null && currentContent.isNotEmpty()) {
                                            results.add(ImportedFile(currentFilename!!, currentContent.toString().trim()))
                                            currentContent.clear()
                                        }
                                        // Start new file
                                        currentFilename = line.substringAfter("=== FILE: ").substringBefore(" ===")
                                    }
                                    line.isBlank() && currentFilename != null -> {
                                        // Skip blank lines between files
                                    }
                                    currentFilename != null -> {
                                        if (currentContent.isNotEmpty()) {
                                            currentContent.appendLine()
                                        }
                                        currentContent.append(line)
                                    }
                                }
                            }
                            
                            // Save last file
                            if (currentFilename != null && currentContent.isNotEmpty()) {
                                results.add(ImportedFile(currentFilename!!, currentContent.toString().trim()))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Error processing file $url: ${e.message}")
                e.printStackTrace()
            }
        }
        
        return results
    }
}

actual fun getFileExportImport(): FileExportImport = IosFileExportImport()
