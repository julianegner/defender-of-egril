package de.egril.defender.editor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual suspend fun pickBackgroundImageBytes(): ByteArray? =
    withContext(Dispatchers.IO) {
        try {
            val fileChooser =
                JFileChooser().apply {
                    dialogTitle = "Select Background Image"
                    isMultiSelectionEnabled = false
                    fileFilter =
                        FileNameExtensionFilter(
                            "Image files (*.png, *.jpg, *.jpeg)",
                            "png",
                            "jpg",
                            "jpeg",
                        )
                }
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFile.readBytes()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Background image pick failed: ${e.message}")
            null
        }
    }
