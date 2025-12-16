package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of FileExportImport
 * 
 * Note: This implementation requires UIDocumentPickerViewController integration.
 * For now, it provides stub implementations that return false.
 */
class IosFileExportImport : FileExportImport {
    
    override suspend fun exportFile(filename: String, content: String): Boolean = withContext(Dispatchers.Default) {
        // TODO: Implement using UIDocumentPickerViewController
        println("iOS exportFile not yet implemented - use desktop for now")
        false
    }
    
    override suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean = withContext(Dispatchers.Default) {
        // TODO: Implement using UIDocumentPickerViewController
        println("iOS exportZip not yet implemented - use desktop for now")
        false
    }
    
    override suspend fun importFiles(): List<ImportedFile>? = withContext(Dispatchers.Default) {
        // TODO: Implement using UIDocumentPickerViewController
        println("iOS importFiles not yet implemented - use desktop for now")
        null
    }
}

actual fun getFileExportImport(): FileExportImport = IosFileExportImport()
