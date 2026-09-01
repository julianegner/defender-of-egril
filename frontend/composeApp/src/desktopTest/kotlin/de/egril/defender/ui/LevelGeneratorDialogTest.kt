package de.egril.defender.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.hyperether.resources.AppLocale
import com.hyperether.resources.currentLanguage
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.level.generator.LevelGeneratorDialog
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the Level Generator dialog reachable from the Level Editor.
 */
class LevelGeneratorDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setDefaultLanguage() {
        currentLanguage.value = AppLocale.DEFAULT
    }

    private val testMap =
        EditorMap(
            id = "generator_test_map",
            name = "Generator Test Map",
            width = 4,
            height = 3,
            tiles =
                mapOf(
                    "0,1" to TileType.SPAWN_POINT,
                    "1,1" to TileType.PATH,
                    "2,1" to TileType.PATH,
                    "3,1" to TileType.TARGET,
                    "1,0" to TileType.BUILD_AREA,
                ),
            readyToUse = true,
        )

    @Test
    fun dialogShowsAllGeneratorOptions() {
        composeTestRule.setContent {
            LevelGeneratorDialog(
                availableMaps = listOf(testMap),
                onDismiss = {},
                onGenerate = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Level Generator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Difficulty").assertIsDisplayed()
        composeTestRule.onNodeWithText("Villains").assertIsDisplayed()
        composeTestRule.onNodeWithText("Generate a new map").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Use an existing map").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Map size").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun villainDescriptionsAreShownInSelectionList() {
        composeTestRule.setContent {
            LevelGeneratorDialog(
                availableMaps = listOf(testMap),
                onDismiss = {},
                onGenerate = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Garokk the Skullsplitter").performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "His War Cry drives nearby Horde units into a frenzy, letting them advance faster. Cut him down before his roar overwhelms your defenses!",
            ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun rosterSelectionIsShownWhenNoVillainIsSelected() {
        composeTestRule.setContent {
            LevelGeneratorDialog(
                availableMaps = listOf(testMap),
                onDismiss = {},
                onGenerate = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Primary roster").assertIsDisplayed()
        composeTestRule.onNodeWithText("Secondary roster (optional)").assertIsDisplayed()
    }

    @Test
    fun selectingExistingMapShowsMapSelection() {
        composeTestRule.setContent {
            LevelGeneratorDialog(
                availableMaps = listOf(testMap),
                onDismiss = {},
                onGenerate = {},
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Use an existing map").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Select map").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Generator Test Map").performScrollTo().assertIsDisplayed()
    }
}
