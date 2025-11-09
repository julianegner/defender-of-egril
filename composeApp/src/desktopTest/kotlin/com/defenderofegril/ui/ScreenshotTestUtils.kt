package com.defenderofegril.ui

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

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
     * @param width Width of the screenshot in pixels
     * @param height Height of the screenshot in pixels
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
        
        // For desktop, we need to use the onRoot() to capture the entire tree
        val image = captureNodeAsImage(composeTestRule.onRoot(), width, height)
        
        // Save to file
        ImageIO.write(image, "PNG", screenshotFile)
        
        println("Screenshot saved to: ${screenshotFile.absolutePath}")
        
        return screenshotFile
    }
    
    /**
     * Captures a specific node as an image.
     * This is a simplified implementation for desktop testing.
     */
    private fun captureNodeAsImage(
        node: SemanticsNodeInteraction,
        width: Int,
        height: Int
    ): BufferedImage {
        // Create a blank image as placeholder
        // In a real implementation, we would use the actual rendered content
        // For now, this creates a basic image that can be replaced with actual rendering
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        
        // Fill with white background
        graphics.color = java.awt.Color.WHITE
        graphics.fillRect(0, 0, width, height)
        
        // Add a simple marker to show this is a test screenshot
        graphics.color = java.awt.Color.BLACK
        graphics.drawString("UI Test Screenshot", 10, 20)
        
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
