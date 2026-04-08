package de.egril.defender.save

/**
 * Platform-agnostic interface for exporting and importing save files
 */
interface FileExportImport {
    /**
     * Export a single save file to user-selected location
     * @param filename Name of the file to export
     * @param content Content of the file
     * @return true if export was successful
     */
    suspend fun exportFile(filename: String, content: String): Boolean
    
    /**
     * Export multiple save files as a ZIP archive
     * @param zipFilename Name of the ZIP file
     * @param files Map of filename to content
     * @return true if export was successful
     */
    suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean
    
    /**
     * Import save files from user-selected location
     * Can be individual JSON files or ZIP archives
     * @return List of ImportedFile objects, or null if user cancelled
     */
    suspend fun importFiles(): List<ImportedFile>?
}

/**
 * Represents an imported file
 */
data class ImportedFile(
    val filename: String,
    val content: String
)

/**
 * Get platform-specific FileExportImport implementation
 */
expect fun getFileExportImport(): FileExportImport
