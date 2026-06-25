package de.egril.defender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.model.Achievement
import de.egril.defender.model.AchievementId
import de.egril.defender.model.TutorialStep
import de.egril.defender.ui.gameplay.TutorialOverlay
import de.egril.defender.ui.gameplay.calculateTutorialOverlayLayout
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MobileWebOverlayScreenshotTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    @Test
    fun captureMobileWebTutorialAndAchievementLayouts() {
        composeTestRule.setContent {
            MaterialTheme {
                val tutorialLayout =
                    calculateTutorialOverlayLayout(
                        availableWidth = 900.dp,
                        availableHeight = 480.dp,
                        isMobileWeb = true,
                        isSideOverlayVisible = false,
                    )
                val achievementLayout =
                    calculateAchievementNotificationLayout(
                        availableWidth = 900.dp,
                        availableHeight = 480.dp,
                        isPlatformMobileDevice = false,
                        isMobileWeb = true,
                    )

                Row(
                    modifier =
                        Modifier
                            .width(900.dp)
                            .height(480.dp)
                            .background(Color(0xFF111111))
                            .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TutorialOverlay(
                        currentStep = TutorialStep.BUILD_TOWER,
                        isNextEnabled = true,
                        onNext = {},
                        onSkip = {},
                        layout = tutorialLayout,
                    )

                    AchievementNotificationCard(
                        achievement = Achievement(AchievementId.BUILD_TOWER, 0L),
                        layout = achievementLayout,
                        titleStyle = MaterialTheme.typography.titleLarge,
                        nameStyle = MaterialTheme.typography.titleMedium,
                        descriptionStyle = MaterialTheme.typography.bodySmall,
                        onDismiss = {},
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "mobile-web-overlays-responsive",
            width = 900,
            height = 480,
        )
    }
}
