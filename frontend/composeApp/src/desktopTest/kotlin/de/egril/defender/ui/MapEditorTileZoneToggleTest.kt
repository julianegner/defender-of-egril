package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.geometry.Offset
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.model.Position
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.RiverTile
import de.egril.defender.model.TileZone
import de.egril.defender.ui.editor.map.MapEditorView
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapEditorTileZoneToggleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sidePanelSectionOpensAndClosesTileZonePanel() {
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

        expandSidePanel()
        composeTestRule.onNodeWithText("Add zone").assertExists()
        composeTestRule.onNodeWithText("With an uploaded map background", substring = true).assertDoesNotExist()
        composeTestRule.onNodeWithText("Add zone").performClick()
        composeTestRule.onNodeWithText("Start zone drawing").assertExists()
        composeTestRule.onNodeWithText("With an uploaded map background", substring = true).assertExists()
        composeTestRule.onNodeWithText("gamedata/user/maps/zone_toggle_map.png", substring = true).assertExists()
        composeTestRule.onNodeWithText("gamedata/user/maps/zone_toggle_map.json", substring = true).assertExists()
        composeTestRule.onNodeWithText("Start zone drawing").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Finish zone drawing").assertExists()
        composeTestRule.onNodeWithText("Finish zone drawing").performScrollTo().performClick()
        composeTestRule.onNode(hasText("Tile zones") and hasClickAction()).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodes(hasText("Add zone")).assertCountEquals(0)
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

        expandSidePanel()
        composeTestRule.onNodeWithText("Add zone").performClick()
        composeTestRule.onNodeWithText("Start zone drawing").performScrollTo().performClick()

        composeTestRule.onNode(hasText("PATH") and hasClickAction()).performScrollTo().performClick()
        composeTestRule.waitForIdle()
        TileType.entries.forEach { tileType ->
            assertTrue(composeTestRule.onAllNodesWithText(tileType.name).fetchSemanticsNodes().isNotEmpty())
        }
        composeTestRule.onNodeWithText(TileType.BUILD_AREA.name).performScrollTo().performClick()
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

    @Test
    fun zonesCanBeCopiedAndOtherZoneOverlaysCanBeToggled() {
        var savedMap: EditorMap? = null
        val position = Position(1, 1)
        val riverTile = RiverTile(position, RiverFlow.WEST, flowSpeed = 2)
        val sourceZone =
            TileZone(
                id = "high_tide",
                name = "High tide",
                tiles = mapOf(position to TileType.RIVER),
                riverTiles = mapOf(position to riverTile),
            )
        val map =
            EditorMap(
                id = "zone_copy_map",
                name = "Zone Copy",
                width = 3,
                height = 3,
                tiles = mapOf("0,0" to TileType.PATH),
                tileZones = listOf(sourceZone),
            )
        composeTestRule.setContent {
            MapEditorView(map = map, onSave = { updatedMap, _, _ -> savedMap = updatedMap }, onCancel = {})
        }

        expandSidePanel()
        composeTestRule.onNodeWithText("Copy").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Show other zones").performScrollTo().assertExists().performClick()
        composeTestRule.onNodeWithText("Save Map").performClick()

        composeTestRule.runOnIdle {
            val zones = requireNotNull(savedMap).tileZones
            assertEquals(2, zones.size)
            assertEquals("zone_2", zones[1].id)
            assertEquals(sourceZone.tiles, zones[1].tiles)
            assertEquals(sourceZone.riverTiles, zones[1].riverTiles)
        }
    }

    /** Opens the tile zones section of the side panel if it is currently collapsed. */
    private fun expandSidePanel() {
        val sectionOpen =
            composeTestRule
                .onAllNodes(hasText("Add zone"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        if (!sectionOpen) {
            composeTestRule.onNode(hasText("Tile zones") and hasClickAction()).performClick()
            composeTestRule.waitForIdle()
        }
    }
}

