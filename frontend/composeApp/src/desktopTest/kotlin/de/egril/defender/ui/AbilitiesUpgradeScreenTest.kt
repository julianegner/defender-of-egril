package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.model.PlayerAbilities
import de.egril.defender.save.PlayerProfile
import de.egril.defender.ui.ScreenshotTestUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Abilities Upgrade screen.
 */
class AbilitiesUpgradeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    private fun createTestProfile(availableAbilityPoints: Int = 3) = PlayerProfile(
        id = "test_player",
        name = "Test Player",
        createdAt = 0L,
        lastPlayedAt = 0L,
        abilities = PlayerAbilities(
            totalXP = 500,
            level = 3,
            availableAbilityPoints = availableAbilityPoints
        )
    )

    @Test
    fun testAbilitiesScreenWithContinueButton() {
        var backClicked = false
        var continueClicked = false

        composeTestRule.setContent {
            AbilitiesUpgradeScreen(
                playerProfile = createTestProfile(),
                onUpgradeAbility = {},
                onUnlockSpell = {},
                onBack = { backClicked = true },
                onContinueToNextLevel = { continueClicked = true },
                nextLevelName = "The Ork Invasion"
            )
        }

        composeTestRule.waitForIdle()

        // "Back to World Map" button should appear (instead of plain "Back")
        composeTestRule.onNodeWithText("Back to World Map", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()

        // "Continue with Level" button should appear
        composeTestRule.onNodeWithText("Continue with Level", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()

        // Plain "Back" button should NOT appear (only "Back to World Map" is shown)
        // We check by using exact match (not substring) to distinguish from "Back to World Map"
        composeTestRule.onAllNodesWithText("Back", ignoreCase = true)
            .filter(hasText("Back", substring = false))
            .assertCountEquals(0)

        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "abilities-screen-with-continue-button",
            width = 1200,
            height = 800
        )
    }

    @Test
    fun testAbilitiesScreenWithoutContinueButton() {
        var backClicked = false

        composeTestRule.setContent {
            AbilitiesUpgradeScreen(
                playerProfile = createTestProfile(),
                onUpgradeAbility = {},
                onUnlockSpell = {},
                onBack = { backClicked = true }
            )
        }

        composeTestRule.waitForIdle()

        // Plain "Back" button should appear
        composeTestRule.onNodeWithText("Back", substring = true, ignoreCase = true)
            .assertExists()
            .assertHasClickAction()

        // "Back to World Map" and "Continue with Level" should NOT appear
        composeTestRule.onNodeWithText("Back to World Map", substring = true, ignoreCase = true)
            .assertDoesNotExist()

        composeTestRule.onNodeWithText("Continue with Level", substring = true, ignoreCase = true)
            .assertDoesNotExist()

        // Capture screenshot
        ScreenshotTestUtils.captureScreenshot(
            composeTestRule,
            "abilities-screen-normal",
            width = 1200,
            height = 800
        )
    }
}
