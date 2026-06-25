package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.game.LevelData
import de.egril.defender.iam.IamState
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.ui.worldmap.WorldMapScreen
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Daily Hint Banner feature on the World Map screen.
 *
 * Verifies that the banner appears once per calendar day, can be dismissed,
 * and does not reappear after being shown today.
 */
class DailyHintBannerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSampleWorldLevels(): List<WorldLevel> =
        LevelData.createLevels().take(3).map { level ->
            WorldLevel(level = level, status = LevelStatus.UNLOCKED)
        }

    @Before
    fun setup() {
        // Reset daily hint state so the banner appears on every test run
        AppSettings.dailyHintLastShownDate.value = ""
        AppSettings.dailyHintLastIndex.value = -1
    }

    @Test
    fun testDailyHintBannerAppearsOnFirstEntry() {
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = createSampleWorldLevels(),
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,
                iamState = IamState(),
            )
        }

        composeTestRule.waitForIdle()

        // The "Daily Hint" title must be visible
        composeTestRule
            .onNodeWithText("Daily Hint", substring = true, ignoreCase = true)
            .assertExists()

        // Close button must be present
        composeTestRule
            .onNodeWithContentDescription("Close", substring = true, ignoreCase = true)
            .assertHasClickAction()
    }

    @Test
    fun testDailyHintBannerDoesNotAppearIfAlreadyShownToday() {
        // Simulate that the hint was already shown today
        val today =
            de.egril.defender.ui.worldmap
                .todayLocalDateString()
        AppSettings.markDailyHintShown(today, 0)

        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = createSampleWorldLevels(),
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,
                iamState = IamState(),
            )
        }

        composeTestRule.waitForIdle()

        // Banner must NOT appear
        composeTestRule
            .onNodeWithText("Daily Hint", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun testDailyHintBannerDismissedByCloseButton() {
        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = createSampleWorldLevels(),
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,
                iamState = IamState(),
            )
        }

        composeTestRule.waitForIdle()

        // Banner is visible
        composeTestRule
            .onNodeWithText("Daily Hint", substring = true, ignoreCase = true)
            .assertExists()

        // Click the close button
        composeTestRule
            .onNodeWithContentDescription("Close", substring = true, ignoreCase = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Banner is gone
        composeTestRule
            .onNodeWithText("Daily Hint", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun testDailyHintBannerHiddenForAuthenticatedAccountHint() {
        // Force index so the next hint is the "account" hint (index 0), which is
        // hidden for authenticated users.  With lastIndex = -1 the next candidate
        // is index 0 (account hint), which shouldShow returns false for authenticated.
        // Depending on catalog order the next eligible hint will be shown instead.
        AppSettings.dailyHintLastShownDate.value = ""
        AppSettings.dailyHintLastIndex.value = -1

        composeTestRule.setContent {
            WorldMapScreen(
                worldLevels = createSampleWorldLevels(),
                onLevelSelected = {},
                onBackToMenu = {},
                onShowRules = {},
                onOpenEditor = {},
                onLoadGame = {},
                checkForNewRepositoryData = false,
                iamState = IamState(isAuthenticated = true, username = "testuser"),
            )
        }

        composeTestRule.waitForIdle()

        // A hint should still appear (the account hint is skipped, but another is shown)
        composeTestRule
            .onNodeWithText("Daily Hint", substring = true, ignoreCase = true)
            .assertExists()

        // The account-specific message must NOT appear for authenticated users
        composeTestRule
            .onNodeWithText("Create a free Egril account", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }
}
