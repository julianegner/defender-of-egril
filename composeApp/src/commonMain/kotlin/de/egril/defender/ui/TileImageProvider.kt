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
 * Loads images from tile type folders when available,
 * falls back to null if folder is empty or tile images are disabled.
 * 
 * Performance optimizations:
 * - Uses single image per tile type (no randomization) for faster loading
 * - Caches loaded ImageBitmaps to avoid repeated decoding
 * - Uses lazy discovery with early exit on first found image
 */
object TileImageProvider {
    // Cache of discovered filenames per tile type
    private val filenameCache = mutableMapOf<TileType, String?>()
    
    // Cache of loaded ImageBitmaps to avoid repeated decoding
    private val imageBitmapCache = mutableMapOf<String, ImageBitmap>()
    
    /**
     * Attempts to load a tile image for the given tile type.
     * Returns null if:
     * - Tile images are disabled in settings
     * - No images are available for this tile type
     * - Loading fails
     * 
     * Uses a single image per tile type for optimal performance.
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
     * Loads the tile image for the given type from files/tiles/{TileType}/
     * Returns null if no images are found or loading fails
     * 
     * Uses the first discovered image for consistency and performance.
     */
    private fun loadTileImageForType(tileType: TileType): ImageBitmap? {
        // Get or discover the image filename for this tile type
        val filename = filenameCache.getOrPut(tileType) {
            discoverFirstImageForTileType(tileType)
        }
        
        if (filename == null) {
            return null
        }
        
        val folderName = tileType.name.lowercase()
        val cacheKey = "files/tiles/$folderName/$filename"
        
        // Check if we already have this image loaded
        imageBitmapCache[cacheKey]?.let { return it }
        
        // Load and cache the image
        return try {
            runBlocking {
                val bytes = Res.readBytes(cacheKey)
                val bitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()
                imageBitmapCache[cacheKey] = bitmap
                bitmap
            }
        } catch (e: Exception) {
            // Failed to load, return null
            null
        }
    }
    
    /**
     * Discovers the first available image in a tile type folder.
     * Returns early on first match for performance.
     * This allows images to be added without code changes.
     */
    private fun discoverFirstImageForTileType(tileType: TileType): String? {
        val folderName = tileType.name.lowercase()
        
        // Try common naming patterns in order of likelihood
        val patterns = listOf(
            // Most common patterns first
            "grass", "mountains", "mountain", "dirt", "stone", "ground", 
            "hills", "hill", "path", "tile", "floor", "earth", "sand", 
            "rock", "forest", "trees", "water", "snow",
            // Generic patterns
            "image", "bg", "background", "tex", "texture",
            // Numbered without prefix
            ""
        )
        
        // Try each pattern with numbers 1-10 (reduced from 20 for faster discovery)
        for (pattern in patterns) {
            for (i in 1..10) {
                val filename = if (pattern.isEmpty()) "$i.png" else "$pattern$i.png"
                if (tryLoadImage(folderName, filename)) {
                    return filename // Return immediately on first match
                }
            }
        }
        
        // Also try pattern without numbers for single files
        for (pattern in patterns) {
            if (pattern.isNotEmpty()) {
                val filename = "$pattern.png"
                if (tryLoadImage(folderName, filename)) {
                    return filename
                }
            }
        }
        
        return null
    }
    
    /**
     * Tries to load an image to check if it exists
     */
    private fun tryLoadImage(folder: String, filename: String): Boolean {
        return try {
            runBlocking {
                Res.readBytes("files/tiles/$folder/$filename")
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Clears all caches. Call this if images are added/removed at runtime.
     */
    fun clearCache() {
        filenameCache.clear()
        imageBitmapCache.clear()
    }
}
