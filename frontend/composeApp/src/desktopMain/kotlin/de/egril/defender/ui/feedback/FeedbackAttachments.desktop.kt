package de.egril.defender.ui.feedback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun captureScreenshot(): FeedbackAttachment? =
    withContext(Dispatchers.IO) {
        try {
            val robot = Robot()
            val screenRect = Rectangle(Toolkit.getDefaultToolkit().screenSize)
            val screenshot = robot.createScreenCapture(screenRect)
            val baos = ByteArrayOutputStream()
            ImageIO.write(screenshot, "png", baos)
            val base64 = Base64.encode(baos.toByteArray())
            FeedbackAttachment(
                filename = "screenshot.png",
                mimeType = "image/png",
                base64Content = base64,
            )
        } catch (e: Exception) {
            println("Screenshot capture failed: ${e.message}")
            null
        }
    }

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun pickFeedbackFiles(): List<FeedbackAttachment> =
    withContext(Dispatchers.IO) {
        try {
            val fileChooser =
                JFileChooser().apply {
                    dialogTitle = "Attach files"
                    isMultiSelectionEnabled = true
                    fileFilter =
                        FileNameExtensionFilter(
                            "Images, Text, JSON (*.png, *.jpg, *.jpeg, *.gif, *.txt, *.json)",
                            "png",
                            "jpg",
                            "jpeg",
                            "gif",
                            "txt",
                            "json",
                        )
                }
            val result = fileChooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                fileChooser.selectedFiles.mapNotNull { file -> readFileAsAttachment(file) }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("File pick failed: ${e.message}")
            emptyList()
        }
    }

@OptIn(ExperimentalEncodingApi::class)
private fun readFileAsAttachment(file: File): FeedbackAttachment? =
    try {
        val bytes = file.readBytes()
        val mimeType =
            when (file.extension.lowercase()) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "gif" -> "image/gif"
                "json" -> "application/json"
                "txt" -> "text/plain"
                else -> "application/octet-stream"
            }
        FeedbackAttachment(
            filename = file.name,
            mimeType = mimeType,
            base64Content = Base64.encode(bytes),
        )
    } catch (e: Exception) {
        println("Error reading file ${file.name}: ${e.message}")
        null
    }
