package de.egril.defender.ui.editor.worldmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Utility functions for managing location icons on the world map.
 * 
 * Location icons are drawable resources (PNG images) that can be used
 * to visually represent locations on the world map.
 */
object LocationIconUtils {
    
    /**
     * List of available location icon resource names (without file extensions).
     * These icons are suitable for displaying on the world map as location markers.
     * 
     * Uses icons with "location_" prefix. Falls back to "emoji_" prefix for compatibility.
     */
    val AVAILABLE_LOCATION_ICONS = listOf(
        "fortress",
        "city",
        "creek",
        "creek_valley",
        "cross",
        "dance",
        "forest",
        "maelstrom",
        "prison",
        "prison2",
        "river",
        "round_tower",
        "scroll",
        "spiral",
        "square_tower",
        "village",
        "woods"
    )
    
    /**
     * Get the drawable resource for a given icon resource name.
     * Returns null if the icon doesn't exist.
     * 
     * Supports both "location_" and "emoji_" prefixes for backward compatibility.
     */
    fun getIconResource(iconResourceName: String?): DrawableResource? {
        if (iconResourceName == null) return null
        
        // Try to get the resource with the given name
        // Falls back to emoji_ prefix if location_ doesn't exist
        return when (iconResourceName) {
            "location_fortress" -> Res.drawable.location_fortress
            "location_city" -> Res.drawable.location_city
            "location_creek" -> Res.drawable.location_creek
            "location_creek_valley" -> Res.drawable.location_creek_valley
            "location_cross" -> Res.drawable.location_cross
            "location_dance" -> Res.drawable.location_dance
            "location_forest" -> Res.drawable.location_forest
            "location_maelstrom" -> Res.drawable.location_maelstrom
            "location_prison" -> Res.drawable.location_prison
            "location_prison2" -> Res.drawable.location_prison2
            "location_river" -> Res.drawable.location_river
            "location_round_tower" -> Res.drawable.location_round_tower
            "location_scroll" -> Res.drawable.location_scroll
            "location_spiral" -> Res.drawable.location_spiral
            "location_square_tower" -> Res.drawable.location_square_tower
            "location_village"-> Res.drawable.location_village
            "location_woods" -> Res.drawable.location_woods
            "fortress" -> Res.drawable.location_fortress
            "city" -> Res.drawable.location_city
            "creek" -> Res.drawable.location_creek
            "creek_valley" -> Res.drawable.location_creek_valley
            "cross" -> Res.drawable.location_cross
            "dance" -> Res.drawable.location_dance
            "forest" -> Res.drawable.location_forest
            "maelstrom" -> Res.drawable.location_maelstrom
            "prison" -> Res.drawable.location_prison
            "prison2" -> Res.drawable.location_prison2
            "river" -> Res.drawable.location_river
            "round_tower" -> Res.drawable.location_round_tower
            "scroll" -> Res.drawable.location_scroll
            "spiral" -> Res.drawable.location_spiral
            "square_tower" -> Res.drawable.location_square_tower
            "village"-> Res.drawable.location_village
            "woods" -> Res.drawable.location_woods
            else -> null // Icon doesn't exist or not in allowed list
        }
    }
    
    /**
     * Check if an icon resource name exists and is valid.
     */
    fun isValidIcon(iconResourceName: String?): Boolean {
        return iconResourceName != null && getIconResource(iconResourceName) != null
    }
    
    /**
     * Get a display name for an icon resource name.
     */
    fun getIconDisplayName(iconResourceName: String): String {
        return iconResourceName
    }
    
    /**
     * Load an icon painter for a given resource name.
     * Returns null if the icon doesn't exist.
     */
    @Composable
    fun loadIconPainter(iconResourceName: String?): Painter? {
        val resource = getIconResource(iconResourceName) ?: return null
        return painterResource(resource)
    }
}

