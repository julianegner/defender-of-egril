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
import kotlin.random.Random

/**
 * Provides tile background images based on tile type.
 * Loads random images from tile type folders when available,
 * falls back to null if folder is empty or tile images are disabled.
 */
object TileImageProvider {
    // Cache of available images per tile type to avoid repeated file system checks
    private val imageCache = mutableMapOf<TileType, List<String>>()
    
    /**
     * Attempts to load a random tile image for the given tile type.
     * Returns null if:
     * - Tile images are disabled in settings
     * - No images are available for this tile type
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
     * Loads a random tile image for the given type from files/tiles/{TileType}/
     * Returns null if no images are found or loading fails
     * 
     * This function discovers available images by trying common filename patterns.
     * When new images are added to the tile type folders, they will be automatically
     * discovered if they follow common naming conventions.
     */
    private fun loadTileImageForType(tileType: TileType): ImageBitmap? {
        // Get or discover available images for this tile type
        val availableImages = imageCache.getOrPut(tileType) {
            discoverImagesForTileType(tileType)
        }
        
        if (availableImages.isEmpty()) {
            return null
        }
        
        // Randomly select an image
        val selectedImage = availableImages.random()
        
        return try {
            runBlocking {
                val bytes = Res.readBytes("files/tiles/${tileType.name.lowercase()}/$selectedImage")
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        } catch (e: Exception) {
            // Failed to load, return null
            null
        }
    }
    
    /**
     * Discovers available images in a tile type folder by trying common filename patterns.
     * This allows images to be added without code changes.
     */
    private fun discoverImagesForTileType(tileType: TileType): List<String> {
        val discovered = mutableListOf<String>()
        val folderName = tileType.name.lowercase()
        
        // Try common naming patterns
        val patterns = listOf(
            // Pattern: name1.png, name2.png, etc.
            listOf("grass", "dirt", "stone", "ground", "tile", "floor", "earth", "sand", "rock", "path", 
                   "mountains", "mountain", "hills", "hill", "forest", "trees", "water", "snow"),
            // Pattern: image1.png, image2.png, etc.
            listOf("image", "bg", "background", "tex", "texture"),
            // Pattern: 1.png, 2.png, etc.
            listOf("")
        )
        
        // Try each pattern with numbers 1-20
        for (pattern in patterns.flatten()) {
            for (i in 1..20) {
                val filename = if (pattern.isEmpty()) "$i.png" else "$pattern$i.png"
                if (tryLoadImage(folderName, filename)) {
                    discovered.add(filename)
                }
            }
        }
        
        // Also try pattern without numbers for single files
        for (pattern in patterns.flatten()) {
            if (pattern.isNotEmpty()) {
                val filename = "$pattern.png"
                if (tryLoadImage(folderName, filename)) {
                    discovered.add(filename)
                }
            }
        }
        
        return discovered.distinct()
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
     * Clears the image cache. Call this if images are added/removed at runtime.
     */
    fun clearCache() {
        imageCache.clear()
    }
}
