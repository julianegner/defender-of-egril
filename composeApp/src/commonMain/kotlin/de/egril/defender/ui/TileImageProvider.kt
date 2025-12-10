package de.egril.defender.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import de.egril.defender.editor.TileType
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image

/**
 * Provides tile background images based on tile type.
 * Loads images from files/tiles/ folder when available,
 * falls back to null if tile images are disabled or file not found.
 * 
 * Uses a simple naming convention: files/tiles/{tiletype}.png
 * where {tiletype} is the lowercase name of the TileType enum value.
 */
object TileImageProvider {
    // Cache of loaded ImageBitmaps to avoid repeated decoding
    private val imageBitmapCache = mutableMapOf<TileType, ImageBitmap?>()
    
    /**
     * Attempts to load a tile image for the given tile type.
     * Returns null if:
     * - Tile images are disabled in settings
     * - No image is available for this tile type
     * - Loading fails
     */
    @Composable
    fun getTileImage(tileType: TileType): ImageBitmap? {
        val useTileImages = AppSettings.useTileImages.value
        
        if (!useTileImages) {
            return null
        }
        
        return remember(tileType) {
            loadTileImageForType(tileType)
        }
    }
    
    /**
     * Loads the tile image for the given type from files/tiles/{tiletype}.png
     * Returns null if no image is found or loading fails
     */
    private fun loadTileImageForType(tileType: TileType): ImageBitmap? {
        // Check cache first
        if (imageBitmapCache.containsKey(tileType)) {
            return imageBitmapCache[tileType]
        }
        
        val filename = "${tileType.name.lowercase()}.png"
        val path = "files/tiles/$filename"
        
        // Try to load and cache the image
        val bitmap = try {
            runBlocking {
                val bytes = Res.readBytes(path)
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        } catch (e: Exception) {
            // Failed to load, cache null to avoid repeated attempts
            null
        }
        
        imageBitmapCache[tileType] = bitmap
        return bitmap
    }
    
    /**
     * Clears the image cache. Call this if images are added/removed at runtime.
     */
    fun clearCache() {
        imageBitmapCache.clear()
    }
}
