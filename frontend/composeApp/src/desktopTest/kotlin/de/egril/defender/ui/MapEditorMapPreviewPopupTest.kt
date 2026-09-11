package de.egril.defender.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import de.egril.defender.editor.EditorMap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.map.MapEditorView
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO

class MapEditorMapPreviewPopupTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun popupDisplaysAlreadyExistingMapImage() {
        val originalUserHome = System.getProperty("user.home")
        val tempHome = Files.createTempDirectory("map_preview_popup_test").toFile()

        try {
            System.setProperty("user.home", tempHome.absolutePath)

            val testMap =
                EditorMap(
                    id = "test_map_preview_popup",
                    name = "Preview Test Map",
                    width = 5,
                    height = 5,
                    tiles =
                        (0 until 5).flatMap { y ->
                            (0 until 5).map { x -> "$x,$y" to TileType.BUILD_AREA }
                        }.toMap(),
                )

            val mapImageFile = File(tempHome, ".defender-of-egril/gamedata/user/maps/${testMap.id}.png")
            mapImageFile.parentFile.mkdirs()
            mapImageFile.writeBytes(createTestPngBytes())

            composeTestRule.setContent {
                MapEditorView(
                    map = testMap,
                    onSave = { _, _, _ -> },
                    onCancel = {},
                )
            }

            composeTestRule.onNodeWithContentDescription("Map Preview", substring = true, ignoreCase = true).performClick()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onAllNodesWithText("Map image preview", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithText("Map image preview", substring = true, ignoreCase = true).assertExists()
        } finally {
            System.setProperty("user.home", originalUserHome)
            tempHome.deleteRecursively()
        }
    }

    private fun createTestPngBytes(): ByteArray {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0xFFFF0000.toInt())
        image.setRGB(1, 0, 0xFF00FF00.toInt())
        image.setRGB(0, 1, 0xFF0000FF.toInt())
        image.setRGB(1, 1, 0xFFFFFFFF.toInt())

        return ByteArrayOutputStream().use { outputStream ->
            ImageIO.write(image, "png", outputStream)
            outputStream.toByteArray()
        }
    }
}
