package de.egril.defender.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import de.egril.defender.editor.TileType
import de.egril.defender.ui.settings.AppSettings
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.tile_build_area
import defender_of_egril.composeapp.generated.resources.tile_no_play
import defender_of_egril.composeapp.generated.resources.tile_path
import defender_of_egril.composeapp.generated.resources.tile_river
import defender_of_egril.composeapp.generated.resources.tile_river_maelstrom
import org.jetbrains.compose.resources.painterResource

/**
 * Provides tile background images based on tile type.
 * Uses normal drawable resources with tile_ prefix.
 */
object TileImageProvider {
    
    /**
     * Attempts to load a tile image for the given tile type.
     * Returns null if:
     * - Tile images are disabled in settings
     * - No image is available for this tile type
     * 
     * For RIVER tiles, pass isMaelstrom=true to get the maelstrom variant.
     */
    @Composable
    fun getTilePainter(tileType: TileType, isMaelstrom: Boolean = false): Painter? {
        val useTileImages = AppSettings.useTileImages.value
        
        if (!useTileImages) {
            return null
        }
        
        return when (tileType) {
            TileType.PATH -> painterResource(Res.drawable.tile_path)
            TileType.BUILD_AREA -> painterResource(Res.drawable.tile_build_area)
            @Suppress("DEPRECATION")
            TileType.ISLAND -> painterResource(Res.drawable.tile_build_area)  // Deprecated: treat as BUILD_AREA
            TileType.NO_PLAY -> painterResource(Res.drawable.tile_no_play)
            TileType.RIVER -> if (isMaelstrom) {
                painterResource(Res.drawable.tile_river_maelstrom)
            } else {
                painterResource(Res.drawable.tile_river)
            }
            TileType.SPAWN_POINT -> null  // No tile image yet
            TileType.TARGET -> null  // No tile image yet
        }
    }
    
    /**
     * Clears the cache (no longer needed with normal drawables)
     */
    fun clearCache() {
        // No-op: normal drawables are managed by Compose Resources
    }
}
