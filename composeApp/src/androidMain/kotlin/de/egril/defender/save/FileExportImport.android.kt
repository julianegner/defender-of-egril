package de.egril.defender.save

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import de.egril.defender.AndroidContextProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

/**
 * Android implementation of FileExportImport using Storage Access Framework
 */
class AndroidFileExportImport : FileExportImport {
    
    companion object {
        private var instance: AndroidFileExportImport? = null
        
        /**
         * Initialize the file export/import with the main activity
         * Must be called from MainActivity.onCreate()
         */
        @Synchronized
        fun initialize(activity: ComponentActivity) {
            if (instance == null) {
                instance = AndroidFileExportImport()
            }
            instance?.initializeLaunchers(activity)
        }
        
        fun getInstance(): AndroidFileExportImport {
            return instance ?: AndroidFileExportImport().also { instance = it }
        }
    }
    
    private var createDocumentLauncher: ActivityResultLauncher<String>? = null
    private var openDocumentLauncher: ActivityResultLauncher<Array<String>>? = null
    private var createDocumentContinuation: Continuation<Uri?>? = null
    private var openDocumentContinuation: Continuation<List<Uri>>? = null
    
    private fun initializeLaunchers(activity: ComponentActivity) {
        // Launcher for creating a new file (export)
        createDocumentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream")
        ) { uri ->
            createDocumentContinuation?.resume(uri)
            createDocumentContinuation = null
        }
        
        // Launcher for opening existing files (import)
        openDocumentLauncher = activity.registerForActivityResult(
            ActivityResultContracts.OpenMultipleDocuments()
        ) { uris ->
            openDocumentContinuation?.resume(uris)
            openDocumentContinuation = null
        }
    }
    
    override suspend fun exportFile(filename: String, content: String): Boolean = withContext(Dispatchers.Main) {
        try {
            if (createDocumentLauncher == null) {
                println("Error: Cannot export file - Activity launchers not initialized")
                return@withContext false
            }
            
            val context = AndroidContextProvider.getContext()
            
            // Launch the create document intent
            val uri = suspendCancellableCoroutine { continuation ->
                createDocumentContinuation = continuation
                createDocumentLauncher?.launch(filename)
            }
            
            if (uri != null) {
                // Write content to the selected file
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
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
    
    override suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean = withContext(Dispatchers.Main) {
        try {
            if (createDocumentLauncher == null) {
                println("Error: Cannot export ZIP - Activity launchers not initialized")
                return@withContext false
            }
            
            val context = AndroidContextProvider.getContext()
            
            // Launch the create document intent
            val uri = suspendCancellableCoroutine { continuation ->
                createDocumentContinuation = continuation
                createDocumentLauncher?.launch(zipFilename)
            }
            
            if (uri != null) {
                // Create ZIP file with all saves
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        ZipOutputStream(outputStream).use { zos ->
                            files.forEach { (filename, content) ->
                                val entry = ZipEntry(filename)
                                zos.putNextEntry(entry)
                                zos.write(content.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                            }
                        }
                    }
                }
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
    
    override suspend fun importFiles(): List<ImportedFile>? = withContext(Dispatchers.Main) {
        try {
            if (openDocumentLauncher == null) {
                println("Error: Cannot import files - Activity launchers not initialized")
                return@withContext null
            }
            
            val context = AndroidContextProvider.getContext()
            
            // Launch the open document intent with multiple selection
            val uris = suspendCancellableCoroutine { continuation ->
                openDocumentContinuation = continuation
                // Accept JSON and ZIP files specifically, plus generic binary for compatibility
                openDocumentLauncher?.launch(arrayOf("application/json", "application/zip", "*/*"))
            }
            
            if (uris.isEmpty()) {
                return@withContext null
            }
            
            // Process all selected files
            val importedFiles = withContext(Dispatchers.IO) {
                val results = mutableListOf<ImportedFile>()
                
                uris.forEach { uri ->
                    try {
                        val filename = getFileName(context, uri) ?: "unknown"
                        val mimeType = context.contentResolver.getType(uri)
                        
                        // Check both filename extension and MIME type for robust file type detection
                        val isJsonFile = filename.endsWith(".json", ignoreCase = true) || 
                                        mimeType == "application/json" || 
                                        mimeType == "text/json"
                        val isZipFile = filename.endsWith(".zip", ignoreCase = true) || 
                                       mimeType == "application/zip" || 
                                       mimeType == "application/x-zip-compressed"
                        
                        when {
                            isJsonFile -> {
                                // Read JSON file directly
                                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    inputStream.bufferedReader().use { it.readText() }
                                }
                                if (content != null) {
                                    results.add(ImportedFile(filename, content))
                                }
                            }
                            isZipFile -> {
                                // Extract JSON files from ZIP
                                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                    ZipInputStream(inputStream).use { zis ->
                                        var entry = zis.nextEntry
                                        while (entry != null) {
                                            if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                                                val content = zis.bufferedReader().use { it.readText() }
                                                // Use only the filename without path (handle both / and \ separators)
                                                // with fallback for empty names
                                                val entryFilename = entry.name
                                                    .replace('\\', '/')
                                                    .substringAfterLast('/')
                                                    .takeIf { it.isNotEmpty() } ?: "unknown.json"
                                                results.add(ImportedFile(entryFilename, content))
                                            }
                                            entry = zis.nextEntry
                                        }
                                    }
                                }
                            }
                            else -> {
                                println("Skipping file $filename with unsupported type: $mimeType")
                            }
                        }
                    } catch (e: Exception) {
                        println("Error processing file $uri: ${e.message}")
                        e.printStackTrace()
                    }
                }
                
                results
            }
            
            importedFiles
        } catch (e: Exception) {
            println("Error importing files: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    private fun getFileName(context: android.content.Context, uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

actual fun getFileExportImport(): FileExportImport = AndroidFileExportImport.getInstance()
