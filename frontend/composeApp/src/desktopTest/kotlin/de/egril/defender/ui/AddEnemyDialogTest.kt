package de.egril.defender.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.AttackerType
import de.egril.defender.model.Position
import de.egril.defender.model.SpawnPointType
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
        composeTestRule.onNodeWithText("Blue Demon", substring = true, ignoreCase = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Goblin Runner", substring = true, ignoreCase = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Zombie", substring = true, ignoreCase = true).performScrollTo().assertIsDisplayed()
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

    @Test
    fun singleWaterSpawnDisablesGoblinAndDefaultsToCompatibleEnemy() {
        val water = Position(0, 0)
        val map =
            EditorMap(
                id = "water_spawn_dialog",
                width = 2,
                height = 2,
                tiles = mapOf("0,0" to TileType.SPAWN_POINT),
                spawnPointInfoMap = mapOf("0,0" to SpawnPointType.WATER),
            )
        var added: Pair<AttackerType, Position?>? = null
        composeTestRule.setContent {
            AddEnemyDialog(
                turn = 1,
                map = map,
                initialType = AttackerType.GOBLIN,
                onDismiss = {},
                onAdd = { type, _, _, point -> added = type to point },
            )
        }

        composeTestRule.onNodeWithText("Add").performClick()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(AttackerType.PIRATE to water, added)
        }
    }
}
