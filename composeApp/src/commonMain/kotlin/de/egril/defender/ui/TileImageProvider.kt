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
        
        // For now, return null since we don't have any images yet
        // When images are added to files/tiles/{TileType}/ folders,
        // this will load them randomly
        return remember(tileType) {
            loadTileImageForType(tileType)
        }
    }
    
    /**
     * Loads a random tile image for the given type from files/tiles/{TileType}/
     * Returns null if no images are found or loading fails
     */
    private fun loadTileImageForType(tileType: TileType): ImageBitmap? {
        return try {
            // Get list of available images for this tile type
            // Note: In practice, we would need to enumerate files in the directory
            // For now, we return null since the folders are empty
            // This can be extended when images are added
            
            // Example implementation when images are added:
            // val tileFolderPath = "files/tiles/${tileType.name}"
            // val imageFiles = listAvailableImagesInFolder(tileFolderPath)
            // if (imageFiles.isEmpty()) return null
            // val randomImage = imageFiles.random()
            // loadImageBitmap("$tileFolderPath/$randomImage")
            
            null
        } catch (e: Exception) {
            // Failed to load tile image, return null to use color fallback
            null
        }
    }
    
    /**
     * Loads an image from resources and converts it to ImageBitmap
     */
    private fun loadImageBitmap(path: String): ImageBitmap? {
        return try {
            runBlocking {
                val bytes = Res.readBytes(path)
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            }
        } catch (e: Exception) {
            null
        }
    }
}
