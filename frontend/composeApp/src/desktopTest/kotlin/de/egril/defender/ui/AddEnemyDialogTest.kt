package de.egril.defender.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.egril.defender.ui.editor.level.AddEnemyDialog
import org.junit.Rule
import org.junit.Test

class AddEnemyDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun specialTabShowsSpecialEnemies() {
        composeTestRule.setContent {
            AddEnemyDialog(
                turn = 1,
                map = null,
                onDismiss = {},
                onAdd = { _, _, _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("Special", substring = true, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText("Blue Demon", substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Red Demon", substring = true, ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Snotling", substring = true, ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun villainTabShowsVillains() {
        composeTestRule.setContent {
            AddEnemyDialog(
                turn = 1,
                map = null,
                onDismiss = {},
                onAdd = { _, _, _, _ -> },
            )
        }

        composeTestRule.onNodeWithText("Villains", substring = true, ignoreCase = true).performClick()
        composeTestRule.onNodeWithText("Gribnak", substring = true, ignoreCase = true).assertIsDisplayed()
    }
}
