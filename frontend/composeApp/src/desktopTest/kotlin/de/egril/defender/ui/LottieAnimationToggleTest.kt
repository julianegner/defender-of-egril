package de.egril.defender.ui

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import de.egril.defender.ui.animations.AnimationType
import de.egril.defender.ui.animations.LottieAnimation
import de.egril.defender.ui.settings.AppSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LottieAnimationToggleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var originalAnimationsEnabled: Boolean = true

    @Before
    fun storeOriginalSetting() {
        originalAnimationsEnabled = AppSettings.enableAnimations.value
    }

    @After
    fun restoreAnimationsSetting() {
        AppSettings.saveEnableAnimations(originalAnimationsEnabled)
    }

    @Test
    fun lottieAnimationDoesNotRenderWhenAnimationsDisabled() {
        AppSettings.saveEnableAnimations(false)

        composeTestRule.setContent {
            LottieAnimation(
                animationType = AnimationType.ENEMY_SPAWN,
                modifier = Modifier.size(80.dp)
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Enemy spawn portal animation", useUnmergedTree = true)
            .assertDoesNotExist()
    }
}
