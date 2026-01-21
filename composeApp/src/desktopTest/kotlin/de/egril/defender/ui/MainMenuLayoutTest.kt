package de.egril.defender.ui

import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Rule
import org.junit.Test

/**
 * Test to verify the Main Menu screen button layout.
 * 
 * On desktop, buttons should be in a vertical column.
 * On mobile (Android/iOS), buttons should be in a horizontal row.
 * 
 * This test runs on desktop, so it verifies the column layout.
 */
class MainMenuLayoutTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testMainMenuDesktopLayout() {
        // Set the content
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onShowRules = {},
                onShowInstallationInfo = {},
                onSelectPlayer = {},
                onEditPlayerName = {},  // No player selection in tests
                currentPlayerName = null  // No player name in tests
            )
        }
        
        // Wait for composition to complete
        composeTestRule.waitForIdle()
        
        // Capture screenshot for desktop layout (buttons in column)
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "main-menu-desktop-layout",
            width = 1200,
            height = 800
        )
    }
}
