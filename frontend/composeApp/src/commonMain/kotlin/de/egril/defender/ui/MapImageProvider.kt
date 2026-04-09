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
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides map background image painters for game levels.
 * Loads PNG images from resources (bundled maps) or working directory (user maps).
 */
object MapImageProvider {

    /**
     * Try to load map image bytes for the given map ID.
     * First tries the official maps dir, then user maps dir, then community maps dir,
     * then bundled resources.
     */
    suspend fun loadMapImageBytes(mapId: String): ByteArray? {
        val storage = try { getFileStorage() } catch (e: Exception) { null }

        if (storage != null) {
            val officialBytes = storage.readBinaryFile("gamedata/official/maps/$mapId.png")
            if (officialBytes != null) return officialBytes

            val userBytes = storage.readBinaryFile("gamedata/user/maps/$mapId.png")
            if (userBytes != null) return userBytes

            val communityBytes = storage.readBinaryFile("gamedata/community/maps/$mapId.png")
            if (communityBytes != null) return communityBytes
        }

        return try {
            Res.readBytes("files/repository/maps/$mapId.png")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Decode PNG bytes to ImageBitmap (platform-specific).
     */
    fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
        return try {
            decodeMapImageBitmap(bytes)
        } catch (e: Exception) {
            println("MapImageProvider: Failed to decode image: ${e.message}")
            null
        }
    }
}

/**
 * Platform-specific image decoding.
 */
expect fun decodeMapImageBitmap(bytes: ByteArray): ImageBitmap?

/**
 * Holds the state of a map image load: the painter (once loaded) and whether loading is still in progress.
 */
data class MapImageState(val painter: Painter?, val isLoading: Boolean)

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
        val result = withContext(Dispatchers.Default) {
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
