package de.egril.defender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.gameplay.defenderButtons.CompactDefenderButton
import de.egril.defender.ui.gameplay.defenderButtons.DefenderButton
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * Test to verify disabled defender button appearance in light and dark modes.
 * Verifies that disabled buttons are not transparent and have appropriate colors.
 */
class DisabledDefenderButtonTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @After
    fun cleanup() {
        // Reset dark mode after test
        AppSettings.saveDarkMode(false)
    }
    
    @Test
    fun testDefenderButtonsInLightMode() {
        // Set light mode
        AppSettings.saveDarkMode(false)
        
        composeTestRule.setContent {
            MaterialTheme {
                DefenderButtonGrid(isDarkMode = false)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "defender_buttons_light_mode",
            1000,
            800
        )
    }
    
    @Test
    fun testDefenderButtonsInDarkMode() {
        // Set dark mode
        AppSettings.saveDarkMode(true)
        
        composeTestRule.setContent {
            MaterialTheme {
                DefenderButtonGrid(isDarkMode = true)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "defender_buttons_dark_mode",
            1000,
            800
        )
    }
    
    @Test
    fun testCompactDefenderButtonsInLightMode() {
        // Set light mode
        AppSettings.saveDarkMode(false)
        
        composeTestRule.setContent {
            MaterialTheme {
                CompactDefenderButtonGrid(isDarkMode = false)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "compact_defender_buttons_light_mode",
            800,
            600
        )
    }
    
    @Test
    fun testCompactDefenderButtonsInDarkMode() {
        // Set dark mode
        AppSettings.saveDarkMode(true)
        
        composeTestRule.setContent {
            MaterialTheme {
                CompactDefenderButtonGrid(isDarkMode = true)
            }
        }
        
        composeTestRule.waitForIdle()
        
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "compact_defender_buttons_dark_mode",
            800,
            600
        )
    }
}

@Composable
private fun DefenderButtonGrid(isDarkMode: Boolean) {
    val backgroundColor = if (isDarkMode) Color.DarkGray else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isDarkMode) "Defender Buttons - Dark Mode" else "Defender Buttons - Light Mode",
            color = textColor,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Enabled (100 coins available)",
            color = textColor,
            style = MaterialTheme.typography.titleMedium
        )
        
        // Enabled buttons (player has 100 coins)
        val enabledCoins = remember { mutableStateOf(100) }
        DefenderButton(
            type = DefenderType.SPIKE_TOWER,
            isSelected = false,
            canAfford = true,
            coinsState = enabledCoins,
            onClick = {}
        )
        
        DefenderButton(
            type = DefenderType.WIZARD_TOWER,
            isSelected = false,
            canAfford = true,
            coinsState = enabledCoins,
            onClick = {}
        )
        
        Text(
            text = "Disabled (5 coins available - not enough)",
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // Disabled buttons (player has only 5 coins)
        val disabledCoins = remember { mutableStateOf(5) }
        DefenderButton(
            type = DefenderType.SPIKE_TOWER,
            isSelected = false,
            canAfford = false,
            coinsState = disabledCoins,
            onClick = {}
        )
        
        DefenderButton(
            type = DefenderType.WIZARD_TOWER,
            isSelected = false,
            canAfford = false,
            coinsState = disabledCoins,
            onClick = {}
        )
    }
}

@Composable
private fun CompactDefenderButtonGrid(isDarkMode: Boolean) {
    val backgroundColor = if (isDarkMode) Color.DarkGray else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isDarkMode) "Compact Buttons - Dark Mode" else "Compact Buttons - Light Mode",
            color = textColor,
            style = MaterialTheme.typography.headlineMedium
        )
        
        Text(
            text = "Enabled (enough coins)",
            color = textColor,
            style = MaterialTheme.typography.titleMedium
        )
        
        // Enabled compact buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDefenderButton(
                type = DefenderType.SPIKE_TOWER,
                isSelected = false,
                canAfford = true,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
            
            CompactDefenderButton(
                type = DefenderType.BOW_TOWER,
                isSelected = false,
                canAfford = true,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
        }
        
        Text(
            text = "Disabled (not enough coins)",
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // Disabled compact buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDefenderButton(
                type = DefenderType.SPIKE_TOWER,
                isSelected = false,
                canAfford = false,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
            
            CompactDefenderButton(
                type = DefenderType.BOW_TOWER,
                isSelected = false,
                canAfford = false,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
        }
        
        Text(
            text = "Mixed (some affordable, some not)",
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // Mixed state - player has 15 coins (can afford spike and spear but not bow)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactDefenderButton(
                type = DefenderType.SPIKE_TOWER,  // 10 coins - affordable
                isSelected = false,
                canAfford = true,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
            
            CompactDefenderButton(
                type = DefenderType.SPEAR_TOWER,  // 15 coins - affordable
                isSelected = false,
                canAfford = true,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
            
            CompactDefenderButton(
                type = DefenderType.BOW_TOWER,  // 20 coins - not affordable
                isSelected = false,
                canAfford = false,
                modifier = Modifier.weight(1f).height(50.dp),
                onClick = {}
            )
        }
    }
}
