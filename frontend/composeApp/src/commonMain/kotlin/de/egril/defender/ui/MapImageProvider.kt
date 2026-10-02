package de.egril.defender.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import de.egril.defender.editor.getFileStorage
import de.egril.defender.editor.readPlatformRepositoryBytes
import de.egril.defender.model.EventMapImage
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides map background image painters for game levels.
 * Loads PNG images from resources (bundled maps) or working directory (user maps).
 */
object MapImageProvider {
    /** Stable asset name for the zone at [zoneIndex] in the map's saved zone list. */
    fun tileZoneImageFileName(
        mapId: String,
        zoneIndex: Int,
    ): String = "$mapId.zone-$zoneIndex.png"

    /**
     * Try to load map image bytes for the given map ID.
     * First tries the official maps dir, then user maps dir, then community maps dir,
     * then bundled resources.
     */
    suspend fun loadMapImageBytes(mapId: String): ByteArray? = loadImageBytes("$mapId.png")

    suspend fun loadTileZoneImageBytes(
        mapId: String,
        zoneIndex: Int,
    ): ByteArray? = loadImageBytes(tileZoneImageFileName(mapId, zoneIndex))

    /** Load an event image from the level directories, separately from map and zone backgrounds. */
    suspend fun loadEventMapImageBytes(fileName: String): ByteArray? {
        require(EventMapImage.isValidFileName(fileName)) { "Invalid repository image file name: $fileName" }
        return loadImageBytes(fileName, "levels") ?: readPlatformRepositoryBytes("levels/$fileName")
    }

    private suspend fun loadImageBytes(
        fileName: String,
        directory: String = "maps",
    ): ByteArray? {
        val storage =
            try {
                getFileStorage()
            } catch (e: Exception) {
                null
            }

        if (storage != null) {
            val officialBytes = storage.readBinaryFile("gamedata/official/$directory/$fileName")
            if (officialBytes != null) return officialBytes

            val userBytes = storage.readBinaryFile("gamedata/user/$directory/$fileName")
            if (userBytes != null) return userBytes

            val communityBytes = storage.readBinaryFile("gamedata/community/$directory/$fileName")
            if (communityBytes != null) return communityBytes
        }

        return try {
            Res.readBytes("files/repository/$directory/$fileName")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decode PNG bytes to ImageBitmap (platform-specific).
     */
    fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
        try {
            decodeMapImageBitmap(bytes)
        } catch (e: Exception) {
            println("MapImageProvider: Failed to decode image: ${e.message}")
            null
        }
}

data class MapTileZoneImageState(
    val painters: Map<String, Painter>,
    val isLoading: Boolean,
)

/** Load previously generated zone PNGs once per map. No image generation occurs during play. */
@Composable
fun rememberMapTileZoneImageState(
    mapId: String?,
    zones: List<de.egril.defender.model.TileZone>,
): MapTileZoneImageState {
    val useLevelMapImage = AppSettings.useLevelMapImage.value
    val zoneIds = zones.map { it.id }
    var painters by remember(mapId, zoneIds, useLevelMapImage) { mutableStateOf<Map<String, Painter>>(emptyMap()) }
    var isLoading by remember(mapId, zoneIds, useLevelMapImage) {
        mutableStateOf(useLevelMapImage && mapId != null && zones.isNotEmpty())
    }
    LaunchedEffect(mapId, zoneIds, useLevelMapImage) {
        if (!useLevelMapImage || mapId == null) {
            painters = emptyMap()
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        painters =
            withContext(Dispatchers.Default) {
                zones
                    .mapIndexedNotNull { index, zone ->
                        MapImageProvider.loadTileZoneImageBytes(mapId, index)?.let { bytes ->
                            MapImageProvider.decodeImageBitmap(bytes)?.let { zone.id to BitmapPainter(it) }
                        }
                    }.toMap()
            }
        isLoading = false
    }
    return MapTileZoneImageState(painters, isLoading)
}

/**
 * Platform-specific image decoding.
 */
expect fun decodeMapImageBitmap(bytes: ByteArray): ImageBitmap?

/**
 * Holds the state of a map image load: the painter (once loaded) and whether loading is still in progress.
 */
data class MapImageState(
    val painter: Painter?,
    val isLoading: Boolean,
)

/**
 * Composable that loads a map image painter for the given mapId and exposes the loading state.
 */
@Composable
fun rememberMapImageState(mapId: String?): MapImageState {
    val useLevelMapImage = AppSettings.useLevelMapImage.value
    var painter by remember(mapId, useLevelMapImage) { mutableStateOf<Painter?>(null) }
    var isLoading by remember(mapId, useLevelMapImage) { mutableStateOf(useLevelMapImage && mapId != null) }

    LaunchedEffect(mapId, useLevelMapImage) {
        if (!useLevelMapImage || mapId == null) {
            painter = null
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        val result =
            withContext(Dispatchers.Default) {
                val bytes = MapImageProvider.loadMapImageBytes(mapId)
                if (bytes != null) {
                    val bitmap = MapImageProvider.decodeImageBitmap(bytes)
                    if (bitmap != null) BitmapPainter(bitmap) else null
                } else {
                    null
                }
            }
        painter = result
        isLoading = false
    }

    return if (useLevelMapImage && mapId != null) MapImageState(painter, isLoading) else MapImageState(null, false)
}

/**
 * Composable that loads a map image painter for the given mapId.
 */
@Composable
fun rememberMapImagePainter(mapId: String?): Painter? = rememberMapImageState(mapId).painter
