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

/**
 * Provides map background image painters for game levels.
 * Loads PNG images from resources (bundled maps) or working directory (user maps).
 */
object MapImageProvider {

    /**
     * Try to load map image bytes for the given map ID.
     * First tries the official maps dir, then user maps dir, then bundled resources.
     */
    suspend fun loadMapImageBytes(mapId: String): ByteArray? {
        val storage = try { getFileStorage() } catch (e: Exception) { null }

        if (storage != null) {
            val officialBytes = storage.readBinaryFile("gamedata/official/maps/$mapId.png")
            if (officialBytes != null) return officialBytes

            val userBytes = storage.readBinaryFile("gamedata/user/maps/$mapId.png")
            if (userBytes != null) return userBytes
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
 * Composable that loads a map image painter for the given mapId.
 */
@Composable
fun rememberMapImagePainter(mapId: String?): Painter? {
    val useLevelMapImage = AppSettings.useLevelMapImage.value
    var painter by remember(mapId, useLevelMapImage) { mutableStateOf<Painter?>(null) }

    LaunchedEffect(mapId, useLevelMapImage) {
        if (!useLevelMapImage || mapId == null) {
            painter = null
            return@LaunchedEffect
        }

        val bytes = MapImageProvider.loadMapImageBytes(mapId)
        if (bytes != null) {
            val bitmap = MapImageProvider.decodeImageBitmap(bytes)
            painter = if (bitmap != null) BitmapPainter(bitmap) else null
        } else {
            painter = null
        }
    }

    return if (useLevelMapImage) painter else null
}
