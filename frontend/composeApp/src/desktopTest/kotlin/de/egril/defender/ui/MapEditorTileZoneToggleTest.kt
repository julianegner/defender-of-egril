package de.egril.defender.ui

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.map.MapEditorView
import org.junit.Rule
import org.junit.Test

class MapEditorTileZoneToggleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun collapsedHeaderOpensAndClosesTileZonePanel() {
        val map =
            EditorMap(
                id = "zone_toggle_map",
                name = "Zone Toggle",
                width = 3,
                height = 3,
                tiles = mapOf("0,0" to TileType.PATH),
            )
        composeTestRule.setContent {
            MapEditorView(map = map, onSave = { _, _, _ -> }, onCancel = {})
        }

        val toggle = composeTestRule.onNode(hasText("Tile zones") and hasClickAction())
        toggle.assertExists().performClick()
        composeTestRule.onNodeWithText("Add zone").assertExists()
        composeTestRule.onNodeWithText("With an uploaded map background", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Add zone").performClick()
        composeTestRule.onNodeWithText("With an uploaded map background", substring = true).assertExists()
        composeTestRule.onNodeWithText("gamedata/user/maps/zone_toggle_map.png", substring = true).assertExists()
        composeTestRule.onNodeWithText("gamedata/user/maps/zone_toggle_map.json", substring = true).assertExists()
        toggle.performClick()
        composeTestRule.onNodeWithText("Add zone").assertDoesNotExist()
    }
}
