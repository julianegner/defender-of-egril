package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.map.MapEditorView
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        composeTestRule.onNodeWithText("Start zone drawing").assertExists()
        composeTestRule.onNodeWithText("With an uploaded map background", substring = true).assertExists()
        composeTestRule.onNodeWithText("gamedata/user/maps/zone_toggle_map.png", substring = true).assertExists()
        composeTestRule.onNodeWithText("gamedata/user/maps/zone_toggle_map.json", substring = true).assertExists()
        composeTestRule.onNodeWithText("Start zone drawing").performClick()
        composeTestRule.onNodeWithText("Finish zone drawing").assertExists()
        toggle.performClick()
        toggle.performClick()
        composeTestRule.onNodeWithText("Start zone drawing").assertExists()
        toggle.performClick()
        composeTestRule.onNodeWithText("Add zone").assertDoesNotExist()
    }

    @Test
    fun zoneDrawingUsesTheMainTileSelectorWithoutChangingBaseMap() {
        var savedMap: EditorMap? = null
        val map =
            EditorMap(
                id = "zone_drawing_map",
                name = "Zone Drawing",
                width = 3,
                height = 3,
                tiles = mapOf("0,0" to TileType.PATH, "0,2" to TileType.PATH),
            )
        composeTestRule.setContent {
            MapEditorView(map = map, onSave = { updatedMap, _, _ -> savedMap = updatedMap }, onCancel = {})
        }

        composeTestRule.onNode(hasText("Tile zones") and hasClickAction()).performClick()
        composeTestRule.onNodeWithText("Add zone").performClick()
        composeTestRule.onNodeWithText("Start zone drawing").performClick()

        composeTestRule.onNode(hasText("PATH") and hasClickAction()).performClick()
        TileType.entries.forEach { tileType ->
            assertTrue(composeTestRule.onAllNodesWithText(tileType.name).fetchSemanticsNodes().isNotEmpty())
        }
        composeTestRule.onNodeWithText(TileType.BUILD_AREA.name).performClick()
        composeTestRule.onNodeWithText("Finish zone drawing").assertExists()
        composeTestRule.onNodeWithText("0,2").performTouchInput {
            down(center)
            moveBy(Offset(30f, 0f), delayMillis = 200)
            up()
        }
        composeTestRule.onNodeWithText("Save Map").performClick()

        composeTestRule.runOnIdle {
            val saved = requireNotNull(savedMap)
            assertEquals(map.tiles, saved.tiles)
            assertTrue(saved.tileZones.single().tiles.values.contains(TileType.BUILD_AREA))
        }
    }
}
