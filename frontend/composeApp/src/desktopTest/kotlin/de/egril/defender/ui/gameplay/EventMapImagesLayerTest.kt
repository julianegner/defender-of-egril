package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.getFileStorage
import de.egril.defender.model.DefenderType
import de.egril.defender.model.EventMapImage
import de.egril.defender.model.GameState
import de.egril.defender.model.Level
import de.egril.defender.model.Position
import de.egril.defender.ui.MapImageProvider
import de.egril.defender.ui.settings.AppSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventMapImagesLayerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val fileName = "event-image-test-${UUID.randomUUID()}.png"
    private val path = "gamedata/user/levels/$fileName"
    private val oldTileImages = AppSettings.useTileImages.value
    private val oldMapImages = AppSettings.useLevelMapImage.value

    @Before
    fun createImage() {
        val image = BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, 0xFFFF0000.toInt())
        val bytes =
            ByteArrayOutputStream().use {
                ImageIO.write(image, "png", it)
                it.toByteArray()
            }
        getFileStorage().writeBinaryFile(path, bytes)
        AppSettings.useTileImages.value = false
        AppSettings.useLevelMapImage.value = false
    }

    @After
    fun cleanUpImage() {
        getFileStorage().deleteFile(path)
        AppSettings.useTileImages.value = oldTileImages
        AppSettings.useLevelMapImage.value = oldMapImages
    }

    @Test
    fun eventImagesLoadOnlyFromLevelDirectories() =
        runBlocking {
            val storage = getFileStorage()
            val levelBytes = requireNotNull(storage.readBinaryFile(path))
            val mapPath = "gamedata/user/maps/$fileName"
            storage.writeBinaryFile(mapPath, byteArrayOf(1, 2, 3))
            try {
                assertContentEquals(levelBytes, MapImageProvider.loadEventMapImageBytes(fileName))
                storage.deleteFile(path)
                assertNull(MapImageProvider.loadEventMapImageBytes(fileName))
            } finally {
                storage.deleteFile(mapPath)
            }
        }

    @Test
    fun imageCoversTerrainButStaysUnderGameplayContent() {
        val state = GameState(createLevel())
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.size(70.dp, 80.dp).testTag("mapLayers")) {
                    EventMapTerrain(state, 40f, false, emptyMap(), emptySet())
                    EventMapImages(
                        images = listOf(EventMapImage("image", fileName)),
                        hexSize = 40f,
                        modifier = Modifier.fillMaxSize(),
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color.Yellow, radius = 8.dp.toPx(), center = center)
                    }
                }
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val pixels = composeTestRule.onNodeWithTag("mapLayers").captureToImage().toPixelMap()
            pixels[pixels.width / 2, pixels.height / 4] == Color.Red
        }
        val pixels = composeTestRule.onNodeWithTag("mapLayers").captureToImage().toPixelMap()
        assertEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 4])
        assertEquals(Color.Yellow, pixels[pixels.width / 2, pixels.height / 2])
    }

    @Test
    fun gameplayShowsImageWithoutAnyActiveZonesAndRemovesItWhenHidden() {
        val state = GameState(createLevel())
        state.activeEventMapImages.add(EventMapImage("image", fileName))
        composeTestRule.setContent {
            MaterialTheme {
                Box(Modifier.size(300.dp).testTag("gameMap")) {
                    GameGrid(
                        gameState = state,
                        selectedDefenderType = null,
                        selectedDefenderId = null,
                        selectedTargetId = null,
                        selectedTargetPosition = null,
                        selectedMineAction = null,
                        onCellClick = {},
                        isDemoMode = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val pixels = composeTestRule.onNodeWithTag("gameMap").captureToImage().toPixelMap()
            pixels[pixels.width / 2, pixels.height / 2] == Color.Red
        }
        composeTestRule.runOnIdle { state.activeEventMapImages.clear() }
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            val pixels = composeTestRule.onNodeWithTag("gameMap").captureToImage().toPixelMap()
            pixels[pixels.width / 2, pixels.height / 2] != Color.Red
        }
    }

    private fun createLevel() =
        Level(
            id = 1,
            name = "Test",
            subtitle = "",
            gridWidth = 1,
            gridHeight = 1,
            startPositions = emptyList(),
            targetPositions = emptyList(),
            pathCells = setOf(Position(0, 0)),
            buildAreas = emptySet(),
            attackerWaves = emptyList(),
            initialCoins = 0,
            healthPoints = 1,
            availableTowers = setOf(DefenderType.SPIKE_TOWER),
        )
}
