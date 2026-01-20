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
     * Only includes emoji icons that make sense for geographic locations.
     */
    val AVAILABLE_LOCATION_ICONS = listOf(
        "emoji_map",           // Map icon - default/general location
        "emoji_pushpin",       // Pushpin - mark a specific location
        "emoji_door",          // Door - entrance/exit location
        "emoji_crown",         // Crown - important/capital location
        "emoji_target",        // Target - goal/objective location
        "emoji_tools",         // Tools - construction/workshop location
        "emoji_magnifying_glass", // Magnifying glass - discovery/search location
        "emoji_money",         // Money - merchant/treasury location
        "emoji_sword",         // Sword - battle/military location
        "emoji_skull",         // Skull - danger/graveyard location
        "emoji_test_tube",     // Test tube - alchemy/magic location
        "emoji_explosion",     // Explosion - destruction/volcano location
        "emoji_heart",         // Heart - sanctuary/healing location
        "emoji_unlock",        // Unlock - unlocked/accessible location
        "emoji_lock",          // Lock - locked/restricted location
        "emoji_warning"        // Warning - hazardous location
    )
    
    /**
     * Get the drawable resource for a given icon resource name.
     * Returns null if the icon doesn't exist.
     */
    fun getIconResource(iconResourceName: String?): DrawableResource? {
        if (iconResourceName == null) return null
        
        return when (iconResourceName) {
            "emoji_map" -> Res.drawable.emoji_map
            "emoji_pushpin" -> Res.drawable.emoji_pushpin
            "emoji_door" -> Res.drawable.emoji_door
            "emoji_crown" -> Res.drawable.emoji_crown
            "emoji_target" -> Res.drawable.emoji_target
            "emoji_tools" -> Res.drawable.emoji_tools
            "emoji_magnifying_glass" -> Res.drawable.emoji_magnifying_glass
            "emoji_money" -> Res.drawable.emoji_money
            "emoji_sword" -> Res.drawable.emoji_sword
            "emoji_skull" -> Res.drawable.emoji_skull
            "emoji_test_tube" -> Res.drawable.emoji_test_tube
            "emoji_explosion" -> Res.drawable.emoji_explosion
            "emoji_heart" -> Res.drawable.emoji_heart
            "emoji_unlock" -> Res.drawable.emoji_unlock
            "emoji_lock" -> Res.drawable.emoji_lock
            "emoji_warning" -> Res.drawable.emoji_warning
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
     * Converts resource names like "emoji_map" to "Map"
     */
    fun getIconDisplayName(iconResourceName: String): String {
        return iconResourceName
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
