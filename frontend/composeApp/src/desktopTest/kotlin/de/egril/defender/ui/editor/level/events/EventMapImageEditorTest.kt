package de.egril.defender.ui.editor.level.events

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import de.egril.defender.editor.RepositoryImageCatalog
import de.egril.defender.model.EventAction
import de.egril.defender.model.EventActionType
import de.egril.defender.model.EventMapImage
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class EventMapImageEditorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context =
        EventEditorContext(
            minePositions = emptySet(),
            tileZones = emptyList(),
            loopEvents = emptyList(),
            imageFiles = listOf("first.png", "second.png"),
            imageIds = listOf("first", "second"),
        )

    @Test
    fun showActionEditsImageFileAndDecimalGeometry() {
        var action by mutableStateOf(
            EventAction(
                EventActionType.SHOW_MAP_IMAGE,
                mapImage = EventMapImage("first", "first.png", width = 2f, height = 3f),
            ),
        )
        composeTestRule.setContent {
            MaterialTheme { EventMapImageEditor(action, { action = it }, context) }
        }
        composeTestRule.onNodeWithText("first.png").performClick()
        composeTestRule.onNodeWithText("second.png").performClick()
        composeTestRule.onNodeWithText("2.0").performTextReplacement("4,5")
        composeTestRule.onNodeWithText("3.0").performTextReplacement("1.25")
        composeTestRule.runOnIdle {
            assertEquals("second.png", action.mapImage?.fileName)
            assertEquals(4.5f, action.mapImage?.width)
            assertEquals(1.25f, action.mapImage?.height)
        }
    }

    @Test
    fun hideActionSelectsAnImageId() {
        var action by mutableStateOf(EventAction(EventActionType.HIDE_MAP_IMAGE, imageId = "first"))
        composeTestRule.setContent {
            MaterialTheme { EventMapImageEditor(action, { action = it }, context) }
        }
        composeTestRule.onNodeWithText("first").performClick()
        composeTestRule.onNodeWithText("second").performClick()
        composeTestRule.runOnIdle { assertEquals("second", action.imageId) }
        composeTestRule.onNodeWithText("second").assertExists()
    }

    @Test
    fun bundledImageCatalogContainsEveryRepositoryImage() {
        val current = File(System.getProperty("user.dir"))
        val frontend = if (current.name == "composeApp") current.parentFile else current
        val directory = File(frontend, "composeApp/src/commonMain/composeResources/files/repository/levels")
        val files =
            requireNotNull(directory.listFiles())
                .filter { it.isFile && EventMapImage.isValidFileName(it.name) }
                .map { it.name }
                .sorted()
        assertEquals(files, RepositoryImageCatalog.fileNames)
    }
}
