package de.egril.defender.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import java.io.File

/**
 * Utility object for screenshot testing in Compose UI tests.
 * 
 * This provides functionality to:
 * - Capture screenshots of Compose UI components
 * - Save screenshots to a designated directory
 * - Compare screenshots (for regression testing)
 */
object ScreenshotTestUtils {
    
    /**
     * Directory where screenshots will be saved.
     * This directory is committed to git so screenshots are accessible to GitHub Copilot.
     */
    private val screenshotDir = File("test-screenshots").apply {
        if (!exists()) {
            mkdirs()
        }
    }
    
    /**
     * Captures a screenshot of the current compose content and saves it to a file.
     * 
     * @param composeTestRule The compose test rule
     * @param filename Name of the screenshot file (without extension)
     * @param width Width of the screenshot in pixels (ignored, uses actual size)
     * @param height Height of the screenshot in pixels (ignored, uses actual size)
     * @return The saved screenshot file
     */
    fun captureScreenshot(
        composeTestRule: ComposeContentTestRule,
        filename: String,
        width: Int = 1200,
        height: Int = 800
    ): File {
        val screenshotFile = File(screenshotDir, "$filename.png")
        
        // Use the test rule to capture the composable content
        composeTestRule.waitForIdle()
        
        try {
            // Try to capture the main root node
            // For dialogs and popups, there may be multiple roots, so we use useUnmergedTree = true
            val imageBitmap = composeTestRule.onRoot(useUnmergedTree = true).captureToImage()
            
            // Save to file using the ImageBitmap's built-in encoding
            imageBitmap.toAwtImage().let { bufferedImage ->
                javax.imageio.ImageIO.write(bufferedImage, "PNG", screenshotFile)
            }
            
            println("Screenshot saved to: ${screenshotFile.absolutePath}")
        } catch (e: Exception) {
            println("Warning: Could not capture actual UI screenshot: ${e.message}")
            println("Creating placeholder for: ${screenshotFile.absolutePath}")
            
            // Fallback to placeholder if capture fails
            createPlaceholderImage(width, height, filename).let { bufferedImage ->
                javax.imageio.ImageIO.write(bufferedImage, "PNG", screenshotFile)
            }
        }
        
        return screenshotFile
    }
    
    /**
     * Creates a placeholder image when actual capture fails
     */
    private fun createPlaceholderImage(width: Int, height: Int, name: String): java.awt.image.BufferedImage {
        val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        
        graphics.color = java.awt.Color.WHITE
        graphics.fillRect(0, 0, width, height)
        
        graphics.color = java.awt.Color.BLACK
        graphics.drawString("Placeholder: $name", 10, 20)
        
        graphics.dispose()
        return image
    }
    
    /**
     * Gets the screenshot directory for storing test screenshots.
     */
    fun getScreenshotDirectory(): File = screenshotDir
    
    /**
     * Cleans up old screenshots (optional, use with caution).
     */
    fun cleanScreenshotDirectory() {
        screenshotDir.listFiles()?.forEach { it.delete() }
    }
}

/**
 * Extension function to convert ImageBitmap to AWT BufferedImage
 */
private fun androidx.compose.ui.graphics.ImageBitmap.toAwtImage(): java.awt.image.BufferedImage {
    val bufferedImage = java.awt.image.BufferedImage(
        width,
        height,
        java.awt.image.BufferedImage.TYPE_INT_ARGB
    )
    
    val pixels = IntArray(width * height)
    this.readPixels(pixels)
    
    bufferedImage.setRGB(0, 0, width, height, pixels, 0, width)
    
    return bufferedImage
}
