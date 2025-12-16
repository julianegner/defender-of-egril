package de.egril.defender.save

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop/JVM implementation of FileExportImport using Swing file chooser
 */
class DesktopFileExportImport : FileExportImport {
    
    private fun getDownloadsDirectory(): File {
        // Get user's Downloads directory
        val userHome = System.getProperty("user.home")
        val downloadsDir = File(userHome, "Downloads")
        
        // Fall back to user home if Downloads doesn't exist
        return if (downloadsDir.exists() && downloadsDir.isDirectory) {
            downloadsDir
        } else {
            File(userHome)
        }
    }
    
    override suspend fun exportFile(filename: String, content: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = getDownloadsDirectory()
            val fileChooser = JFileChooser(downloadsDir).apply {
                dialogTitle = "Save File"
                selectedFile = File(downloadsDir, filename)
                fileFilter = FileNameExtensionFilter("JSON Files (*.json)", "json")
            }
            
            val result = fileChooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                // Ensure .json extension
                val finalFile = if (!file.name.endsWith(".json")) {
                    File(file.parentFile, file.name + ".json")
                } else {
                    file
                }
                finalFile.writeText(content)
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
    
    override suspend fun exportZip(zipFilename: String, files: Map<String, String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = getDownloadsDirectory()
            val fileChooser = JFileChooser(downloadsDir).apply {
                dialogTitle = "Save ZIP Archive"
                selectedFile = File(downloadsDir, zipFilename)
                fileFilter = FileNameExtensionFilter("ZIP Files (*.zip)", "zip")
            }
            
            val result = fileChooser.showSaveDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                // Ensure .zip extension
                val finalFile = if (!file.name.endsWith(".zip")) {
                    File(file.parentFile, file.name + ".zip")
                } else {
                    file
                }
                
                // Create ZIP file
                ZipOutputStream(FileOutputStream(finalFile)).use { zos ->
                    files.forEach { (filename, content) ->
                        val entry = ZipEntry(filename)
                        zos.putNextEntry(entry)
                        zos.write(content.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
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
    
    override suspend fun importFiles(): List<ImportedFile>? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = getDownloadsDirectory()
            val fileChooser = JFileChooser(downloadsDir).apply {
                dialogTitle = "Select Save Files"
                isMultiSelectionEnabled = true
                fileFilter = FileNameExtensionFilter("Save Files (*.json, *.zip)", "json", "zip")
            }
            
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                val files = fileChooser.selectedFiles
                val importedFiles = mutableListOf<ImportedFile>()
                
                files.forEach { file ->
                    when {
                        file.name.endsWith(".json", ignoreCase = true) -> {
                            // Import JSON file directly
                            val content = file.readText()
                            importedFiles.add(ImportedFile(file.name, content))
                        }
                        file.name.endsWith(".zip", ignoreCase = true) -> {
                            // Extract JSON files from ZIP
                            ZipFile(file).use { zipFile ->
                                zipFile.entries().asSequence()
                                    .filter { !it.isDirectory && it.name.endsWith(".json", ignoreCase = true) }
                                    .forEach { entry ->
                                        val content = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                                        // Use only the filename without path
                                        val filename = entry.name.substringAfterLast('/')
                                        importedFiles.add(ImportedFile(filename, content))
                                    }
                            }
                        }
                    }
                }
                
                importedFiles
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error importing files: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}

actual fun getFileExportImport(): FileExportImport = DesktopFileExportImport()
