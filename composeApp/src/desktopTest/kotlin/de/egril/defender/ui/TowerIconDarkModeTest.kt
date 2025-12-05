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
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * Test to verify tower base visibility in dark mode
 */
class TowerIconDarkModeTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @After
    fun cleanup() {
        // Reset dark mode after test
        AppSettings.saveDarkMode(false)
    }
    
    @Test
    fun testTowerIconsInLightMode() {
        // Set light mode
        AppSettings.saveDarkMode(false)
        
        composeTestRule.setContent {
            MaterialTheme {
                TowerIconGrid(isDarkMode = false)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "tower_icons_light_mode",
            1200,
            800
        )
    }
    
    @Test
    fun testTowerIconsInDarkMode() {
        // Set dark mode
        AppSettings.saveDarkMode(true)
        
        composeTestRule.setContent {
            MaterialTheme {
                TowerIconGrid(isDarkMode = true)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "tower_icons_dark_mode",
            1200,
            800
        )
    }
}

@Composable
private fun TowerIconGrid(isDarkMode: Boolean) {
    val backgroundColor = if (isDarkMode) Color.DarkGray else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (isDarkMode) "Tower Icons - Dark Mode" else "Tower Icons - Light Mode",
            color = textColor,
            style = MaterialTheme.typography.headlineMedium
        )
        
        // Display all tower types in a grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DefenderType.entries.take(4).forEach { type ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(if (isDarkMode) Color.Black else Color.LightGray)
                    ) {
                        TowerTypeIcon(
                            defenderType = type,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = type.name.replace("_", " "),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DefenderType.entries.drop(4).forEach { type ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(if (isDarkMode) Color.Black else Color.LightGray)
                    ) {
                        TowerTypeIcon(
                            defenderType = type,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = type.name.replace("_", " "),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
