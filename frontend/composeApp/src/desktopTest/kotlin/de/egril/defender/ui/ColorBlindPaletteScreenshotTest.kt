package de.egril.defender.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.ui.a11y.ColorBlindPalette
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.app_name
import org.junit.Rule
import org.junit.Test

class ColorBlindPaletteScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureProtanopiaPaletteSample() {
        val scheme = AppTheme.applyColorBlindPalette(AppTheme.lightColorScheme, ColorBlindPalette.PROTANOPIA)

        composeTestRule.setContent {
            MaterialTheme(colorScheme = scheme) {
                Surface(tonalElevation = 2.dp) {
                    Text(stringResource(Res.string.app_name))
                }
            }
        }

        composeTestRule.waitForIdle()
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule = composeTestRule,
            filename = "color-blind-palette-protanopia",
        )
    }
}
