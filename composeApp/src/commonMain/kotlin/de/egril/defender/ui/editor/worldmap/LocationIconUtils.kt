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
        "location_map",           // Map icon - default/general location
        "location_pushpin",       // Pushpin - mark a specific location
        "location_door",          // Door - entrance/exit location
        "location_crown",         // Crown - important/capital location
        "location_target",        // Target - goal/objective location
        "location_tools",         // Tools - construction/workshop location
        "location_magnifying_glass", // Magnifying glass - discovery/search location
        "location_money",         // Money - merchant/treasury location
        "location_sword",         // Sword - battle/military location
        "location_skull",         // Skull - danger/graveyard location
        "location_test_tube",     // Test tube - alchemy/magic location
        "location_explosion",     // Explosion - destruction/volcano location
        "location_heart",         // Heart - sanctuary/healing location
        "location_unlock",        // Unlock - unlocked/accessible location
        "location_lock",          // Lock - locked/restricted location
        "location_warning"        // Warning - hazardous location
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
            "location_map", "emoji_map" -> Res.drawable.emoji_map
            "location_pushpin", "emoji_pushpin" -> Res.drawable.emoji_pushpin
            "location_door", "emoji_door" -> Res.drawable.emoji_door
            "location_crown", "emoji_crown" -> Res.drawable.emoji_crown
            "location_target", "emoji_target" -> Res.drawable.emoji_target
            "location_tools", "emoji_tools" -> Res.drawable.emoji_tools
            "location_magnifying_glass", "emoji_magnifying_glass" -> Res.drawable.emoji_magnifying_glass
            "location_money", "emoji_money" -> Res.drawable.emoji_money
            "location_sword", "emoji_sword" -> Res.drawable.emoji_sword
            "location_skull", "emoji_skull" -> Res.drawable.emoji_skull
            "location_test_tube", "emoji_test_tube" -> Res.drawable.emoji_test_tube
            "location_explosion", "emoji_explosion" -> Res.drawable.emoji_explosion
            "location_heart", "emoji_heart" -> Res.drawable.emoji_heart
            "location_unlock", "emoji_unlock" -> Res.drawable.emoji_unlock
            "location_lock", "emoji_lock" -> Res.drawable.emoji_lock
            "location_warning", "emoji_warning" -> Res.drawable.emoji_warning
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
     * Converts resource names like "location_map" or "emoji_map" to "Map"
     */
    fun getIconDisplayName(iconResourceName: String): String {
        return iconResourceName
            .removePrefix("location_")
            .removePrefix("emoji_")
            .split("_")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
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

