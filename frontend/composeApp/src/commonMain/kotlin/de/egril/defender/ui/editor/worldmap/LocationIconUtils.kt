package de.egril.defender.ui.editor.worldmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

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
    val AVAILABLE_LOCATION_ICONS: List<String> = LocationIconCatalog.names

    /**
     * Get the drawable resource for a given icon resource name.
     * Returns null if the icon doesn't exist.
     *
     * Accepts names with or without the "location_" prefix (e.g. "volcano" or "location_volcano").
     */
    fun getIconResource(iconResourceName: String?): DrawableResource? {
        if (iconResourceName == null) return null
        return LocationIconCatalog.resourceFor(iconResourceName.removePrefix("location_"))
    }

    /**
     * Check if an icon resource name exists and is valid.
     */
    fun isValidIcon(iconResourceName: String?): Boolean = iconResourceName != null && getIconResource(iconResourceName) != null

    /**
     * Get a display name for an icon resource name.
     */
    fun getIconDisplayName(iconResourceName: String): String = iconResourceName

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
