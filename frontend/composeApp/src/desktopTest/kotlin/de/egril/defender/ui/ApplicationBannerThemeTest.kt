package de.egril.defender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * Test to verify ApplicationBanner appearance in both dark and light modes
 */
class ApplicationBannerThemeTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @After
    fun cleanup() {
        // Reset dark mode after test
        AppSettings.saveDarkMode(false)
    }
    
    @Test
    fun testApplicationBannerInLightMode() {
        // Set light mode
        AppSettings.saveDarkMode(false)
        
        composeTestRule.setContent {
            MaterialTheme(colorScheme = AppTheme.lightColorScheme) {
                BannerTestLayout(isDarkMode = false)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "application_banner_light_mode",
            1200,
            600
        )
    }
    
    @Test
    fun testApplicationBannerInDarkMode() {
        // Set dark mode
        AppSettings.saveDarkMode(true)
        
        composeTestRule.setContent {
            MaterialTheme(colorScheme = AppTheme.darkColorScheme) {
                BannerTestLayout(isDarkMode = true)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "application_banner_dark_mode",
            1200,
            600
        )
    }
    
    @Test
    fun testStickerScreenInLightMode() {
        // Set light mode
        AppSettings.saveDarkMode(false)
        
        composeTestRule.setContent {
            MaterialTheme(colorScheme = AppTheme.lightColorScheme) {
                StickerScreen(onBack = {})
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "sticker_screen_light_mode",
            1400,
            1000
        )
    }
    
    @Test
    fun testStickerScreenInDarkMode() {
        // Set dark mode
        AppSettings.saveDarkMode(true)
        
        composeTestRule.setContent {
            MaterialTheme(colorScheme = AppTheme.darkColorScheme) {
                StickerScreen(onBack = {})
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "sticker_screen_dark_mode",
            1400,
            1000
        )
    }
}

@Composable
private fun BannerTestLayout(isDarkMode: Boolean) {
    val backgroundColor = if (isDarkMode) Color(0xFF1C1B1F) else Color.White
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isDarkMode) "Dark Mode" else "Light Mode",
                color = if (isDarkMode) Color.White else Color.Black,
                style = MaterialTheme.typography.headlineSmall
            )
            
            ApplicationBanner()
        }
    }
}
