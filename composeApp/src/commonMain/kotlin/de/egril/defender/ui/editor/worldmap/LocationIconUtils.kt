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
        "fortress"
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
            "fortress" -> Res.drawable.fortress
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

