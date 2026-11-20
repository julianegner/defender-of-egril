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
                (0 until 5)
                    .flatMap { y ->
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

        // The side panel starts open; the overlay toggles are reached via the header.
        // Expand the header to reveal the overlay toggle buttons.
        composeTestRule
            .onNodeWithContentDescription("Expand", substring = true, ignoreCase = true)
            .performClick()
        composeTestRule.waitForIdle()

        // The icon's content description sits on the inner Canvas; the click action is on the parent Button.
        // Clicking the Canvas still reaches the Button. The hover tooltip has no content description, so it is not matched.
        val crosshairIcon = hasContentDescription("Crosshair", substring = true, ignoreCase = true)
        composeTestRule.onNode(crosshairIcon).assertExists().performClick()
        composeTestRule.waitForIdle()

        // Toggle it off again to verify it doesn't leave the screen in a broken state.
        composeTestRule.onNode(crosshairIcon).performClick()
        composeTestRule.waitForIdle()

        // Check the toggle itself: onRoot() is ambiguous while the hover tooltip popup is open.
        composeTestRule
            .onNode(crosshairIcon)
            .assertExists()
    }
}
