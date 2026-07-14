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
import de.egril.defender.ui.icon.WoodIcon
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that a barricade serving as a tower base is rendered with the wooden
 * tower-base platform, so it is visually distinguishable from a plain barricade (issue #627).
 */
class TowerBasePlatformIconTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun towerBasePlatformIconComposesWithoutError() {
        composeTestRule.setContent {
            MaterialTheme {
                TowerBasePlatformIcon(size = 48.dp)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun barricadeVersusTowerBaseBarricade() {
        composeTestRule.setContent {
            MaterialTheme {
                BarricadeComparisonGrid()
            }
        }

        composeTestRule.waitForIdle()

        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "barricade_vs_tower_base_barricade",
            600,
            400,
        )
    }
}

@Composable
private fun BarricadeComparisonGrid() {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                WoodIcon(size = 48.dp)
            }
            Text(text = "Barricade", color = Color.Black)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                WoodIcon(size = 48.dp)
                TowerBasePlatformIcon(size = 48.dp)
            }
            Text(text = "Tower base barricade", color = Color.Black)
        }
    }
}
