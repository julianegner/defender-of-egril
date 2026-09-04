package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.map.MapEditorView
import org.junit.Rule
import org.junit.Test

/**
 * UI test for the crosshair toggle in the map editor.
 *
 * Verifies that a "Crosshair" toggle button is available in the map editor's expanded
 * header controls, and that it can be toggled on/off without crashing the screen.
 */
class MapEditorCrosshairToggleTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testMap =
        EditorMap(
            id = "test_crosshair_map",
            name = "Test Map",
            width = 5,
            height = 5,
            tiles =
                (0 until 5).flatMap { y ->
                    (0 until 5).map { x -> "$x,$y" to TileType.BUILD_AREA }
                }.toMap(),
        )

    @Test
    fun testCrosshairToggleTogglesWithoutCrashing() {
        composeTestRule.setContent {
            MapEditorView(
                map = testMap,
                onSave = { _, _, _ -> },
                onCancel = {},
            )
        }

        composeTestRule.waitForIdle()

        // Expand the header to reveal the overlay toggle buttons.
        composeTestRule
            .onNodeWithContentDescription("Expand", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        // The crosshair toggle button should be present and clickable.
        composeTestRule
            .onNodeWithText("Crosshair", substring = true, ignoreCase = true)
            .assertExists()
            .performClick()
        composeTestRule.waitForIdle()

        // Toggle it off again to verify it doesn't leave the screen in a broken state.
        composeTestRule
            .onNodeWithText("Crosshair", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().assertExists()
    }
}

