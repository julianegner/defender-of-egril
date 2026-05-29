package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.ui.settings.AppSettings
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * UI tests for the Accessibility Banner feature.
 *
 * Verifies that the banner is shown on first run, can be dismissed, and links to
 * the accessibility settings.
 */
class AccessibilityBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        // Show the accessibility banner (first run state)
        AppSettings.accessibilityBannerShown.value = false
        // Suppress the settings hint box so it does not interfere
        AppSettings.settingsHintShown.value = true
    }

    @Test
    fun testAccessibilityBannerShowsOnFirstRun() {
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onContinueGame = {},
                hasAutosave = false,
                onShowRules = {},
                onShowInstallationInfo = {},
                onEditPlayerName = {},
                currentPlayerName = null
            )
        }

        composeTestRule.waitForIdle()

        // Title must be present
        composeTestRule.onNodeWithText("Accessibility Settings Available", substring = true, ignoreCase = true)
            .assertExists()

        // Both action buttons must exist
        composeTestRule.onAllNodesWithText("Got it!", substring = true, ignoreCase = true)
            .onFirst()
            .assertHasClickAction()

        composeTestRule.onNodeWithText("Open Accessibility Settings", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()
    }

    @Test
    fun testAccessibilityBannerDoesNotShowOnSubsequentRuns() {
        AppSettings.accessibilityBannerShown.value = true

        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onContinueGame = {},
                hasAutosave = false,
                onShowRules = {},
                onShowInstallationInfo = {},
                onEditPlayerName = {},
                currentPlayerName = null
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Accessibility Settings Available", substring = true, ignoreCase = true)
            .assertDoesNotExist()
    }

    @Test
    fun testAccessibilityBannerCanBeDismissed() {
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onContinueGame = {},
                hasAutosave = false,
                onShowRules = {},
                onShowInstallationInfo = {},
                onEditPlayerName = {},
                currentPlayerName = null
            )
        }

        composeTestRule.waitForIdle()

        // Banner is initially visible
        composeTestRule.onNodeWithText("Accessibility Settings Available", substring = true, ignoreCase = true)
            .assertExists()

        // Click the dismiss button (first "Got it!" button on screen)
        composeTestRule.onAllNodesWithText("Got it!", substring = true, ignoreCase = true)
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()

        // Banner is gone after dismissal
        composeTestRule.onNodeWithText("Accessibility Settings Available", substring = true, ignoreCase = true)
            .assertDoesNotExist()

        // State is persisted
        assertTrue(AppSettings.accessibilityBannerShown.value, "Accessibility banner should be marked as shown after dismissal")
    }

    @Test
    fun testAccessibilityBannerOpenSettingsButtonDismissesBanner() {
        composeTestRule.setContent {
            MainMenuScreen(
                onStartGame = {},
                onContinueGame = {},
                hasAutosave = false,
                onShowRules = {},
                onShowInstallationInfo = {},
                onEditPlayerName = {},
                currentPlayerName = null
            )
        }

        composeTestRule.waitForIdle()

        // Click "Open Accessibility Settings" button
        composeTestRule.onNodeWithText("Open Accessibility Settings", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()

        composeTestRule.waitForIdle()

        // Banner should be dismissed after clicking the link
        composeTestRule.onNodeWithText("Accessibility Settings Available", substring = true, ignoreCase = true)
            .assertDoesNotExist()

        // State is persisted
        assertTrue(AppSettings.accessibilityBannerShown.value, "Accessibility banner should be marked as shown after opening settings")
    }
}
