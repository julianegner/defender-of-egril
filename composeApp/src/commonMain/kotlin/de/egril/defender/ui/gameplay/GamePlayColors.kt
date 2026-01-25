package de.egril.defender.ui.gameplay

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.egril.defender.ui.settings.AppSettings

/**
 * Color palette for gameplay UI components.
 * Centralizes all color values used throughout the gameplay screens for consistency and maintainability.
 * Provides dark mode variants with softer colors for comfortable night viewing.
 */
object GamePlayColors {
    // Primary action and state colors (light mode)
    private val SuccessLight = Color(0xFF4CAF50)      // Green - positive actions, ready state, target
    private val WarningLight = Color(0xFFFF9800)      // Orange - warnings, building, spawns
    private val ErrorLight = Color(0xFFF44336)        // Red - errors, enemies, danger
    private val InfoLightMode = Color(0xFF2196F3)     // Blue - info, towers ready
    
    // Primary action and state colors (dark mode - much softer, less bright)
    private val SuccessDarkMode = Color(0xFF2E7D32)   // Much darker green
    private val WarningDarkMode = Color(0xFFEF6C00)   // Much darker orange
    private val ErrorDarkMode = Color(0xFFB71C1C)     // Much darker red
    private val InfoDarkMode = Color(0xFF0D47A1)      // Much darker blue
    
    // Extended palette for specific uses (light mode)
    private val InfoDeepLight = Color(0xFF1976D2)     // Dark blue - selected towers
    private val InfoTintLight = Color(0xFF7986CB)     // Light blue - towers with no actions
    private val WarningDeepLight = Color(0xFFFF5722)  // Deep orange - urgent warnings, fireball effects
    private val WarningIntenseLight = Color(0xFFFF6F00)  // Dark orange - special warnings
    private val ErrorDeepLight = Color(0xFFD32F2F)    // Dark red - attack buttons, active enemies
    private val YellowLight = Color(0xFFFFEB3B)       // Yellow - selected items, attack type
    
    // Extended palette for specific uses (dark mode - much softer)
    private val InfoDeepDarkMode = Color(0xFF3A5A8F)      // Lighter blue for selected towers (better contrast in dark mode)
    private val InfoTintDarkMode = Color(0xFF5C6BC0)      // Darker light blue for no actions
    private val WarningDeepDarkMode = Color(0xFFE65100)   // Much darker deep orange
    private val WarningIntenseDarkMode = Color(0xFFEF6C00)   // Much darker orange
    private val ErrorDeepDarkMode = Color(0xFF8B0000)     // Much darker red
    private val YellowDarkMode = Color(0xFFC4A000)        // Much darker yellow
    
    // Map terrain colors (light mode)
    private val BuildIslandLight = Color(0xFF8BC34A)  // Light green - build islands
    private val BuildStripLight = Color(0xFFA5D6A7)   // Medium green - build strips adjacent to path
    private val PathLight = Color(0xFFFFF8DC)         // Cream/beige - enemy path
    private val NonPlayableLight = Color(0xFFE0E0E0)  // Light gray - off-path non-playable areas
    private val BuildingLight = Color(0xFF9E9E9E)     // Gray - towers under construction
    
    // Map terrain colors (dark mode - darker, less bright)
    private val BuildIslandDarkMode = Color(0xFF2E5C1A)   // Much darker green - build islands (was #558B2F)
    private val BuildStripDarkMode = Color(0xFF3D6B2C)    // Darker medium-dark green - build strips (was #66BB6A)
    private val PathDarkMode = Color(0xFF3E3528)          // Dark brown-beige - enemy path
    private val NonPlayableDarkMode = Color(0xFF2C2C2C)   // Dark gray - off-path non-playable areas
    private val BuildingDarkMode = Color(0xFF5F5F5F)      // Medium gray - towers under construction
    
    // Background colors for cards (light mode)
    private val EnemyCardBackgroundLight = Color(0xFFFFEBEE)     // Light red - enemy info cards
    private val UpcomingCardBackgroundLight = Color(0xFFFFF3E0)  // Light orange - upcoming/planned items
    private val DangerCardBackgroundLight = Color(0xFFFFCDD2)    // Light red-pink - danger/warning cards
    
    // Background colors for cards (dark mode - darker)
    private val EnemyCardBackgroundDarkMode = Color(0xFF4A2C2C)      // Dark red - enemy info cards
    private val UpcomingCardBackgroundDarkMode = Color(0xFF4A3C28)   // Dark orange - upcoming/planned items
    private val DangerCardBackgroundDarkMode = Color(0xFF5C3333)     // Dark red-pink - danger/warning cards
    
    // Special effect colors (light mode)
    private val TrapLight = Color(0xFF8B4513)         // Brown - dwarven mine traps
    private val RiverLight = Color(0xFF4A90E2)        // Blue - river tiles
    
    // Special effect colors (dark mode - slightly softer)
    private val TrapDarkMode = Color(0xFF9E6A3F)      // Softer brown - dwarven mine traps
    private val RiverDarkMode = Color(0xFF2E5C8A)     // Darker blue - river tiles
    
    // Buildable tile highlight colors (light mode) - lighter green for better distinction
    private val BuildableHighlightLight = Color(0xFF81C784)  // Light green - buildable tiles when tower selected
    
    // Buildable tile highlight colors (dark mode) - lighter green for better distinction
    private val BuildableHighlightDarkMode = Color(0xFF4CAF50)  // Medium green - buildable tiles when tower selected
    
    // Disabled button colors (light mode)
    private val DisabledButtonLight = Color(0xFF9E9E9E)        // Gray - disabled button background
    private val DisabledButtonTextLight = Color(0xFF757575)    // Dark gray - disabled button text
    
    // Disabled button colors (dark mode)
    private val DisabledButtonDarkMode = Color(0xFF424242)     // Dark gray - disabled button background
    private val DisabledButtonTextDarkMode = Color(0xFF9E9E9E) // Medium gray - disabled button text
    
    // Public properties that adapt to dark mode
    val Success: Color
        @Composable get() = if (AppSettings.isDarkMode.value) SuccessDarkMode else SuccessLight
    
    val Warning: Color
        @Composable get() = if (AppSettings.isDarkMode.value) WarningDarkMode else WarningLight
    
    val Error: Color
        @Composable get() = if (AppSettings.isDarkMode.value) ErrorDarkMode else ErrorLight
    
    val Info: Color
        @Composable get() = if (AppSettings.isDarkMode.value) InfoDarkMode else InfoLightMode
    
    val InfoDark: Color
        @Composable get() = if (AppSettings.isDarkMode.value) InfoDeepDarkMode else InfoDeepLight
    
    val InfoLight: Color
        @Composable get() = if (AppSettings.isDarkMode.value) InfoTintDarkMode else InfoTintLight
    
    val WarningDeep: Color
        @Composable get() = if (AppSettings.isDarkMode.value) WarningDeepDarkMode else WarningDeepLight
    
    val WarningDark: Color
        @Composable get() = if (AppSettings.isDarkMode.value) WarningIntenseDarkMode else WarningIntenseLight
    
    val ErrorDark: Color
        @Composable get() = if (AppSettings.isDarkMode.value) ErrorDeepDarkMode else ErrorDeepLight
    
    val Yellow: Color
        @Composable get() = if (AppSettings.isDarkMode.value) YellowDarkMode else YellowLight
    
    val BuildIsland: Color
        @Composable get() = if (AppSettings.isDarkMode.value) BuildIslandDarkMode else BuildIslandLight
    
    val BuildStrip: Color
        @Composable get() = if (AppSettings.isDarkMode.value) BuildStripDarkMode else BuildStripLight
    
    val Path: Color
        @Composable get() = if (AppSettings.isDarkMode.value) PathDarkMode else PathLight
    
    val NonPlayable: Color
        @Composable get() = if (AppSettings.isDarkMode.value) NonPlayableDarkMode else NonPlayableLight
    
    val Building: Color
        @Composable get() = if (AppSettings.isDarkMode.value) BuildingDarkMode else BuildingLight
    
    val EnemyCardBackground: Color
        @Composable get() = if (AppSettings.isDarkMode.value) EnemyCardBackgroundDarkMode else EnemyCardBackgroundLight
    
    val UpcomingCardBackground: Color
        @Composable get() = if (AppSettings.isDarkMode.value) UpcomingCardBackgroundDarkMode else UpcomingCardBackgroundLight
    
    val DangerCardBackground: Color
        @Composable get() = if (AppSettings.isDarkMode.value) DangerCardBackgroundDarkMode else DangerCardBackgroundLight
    
    val Trap: Color
        @Composable get() = if (AppSettings.isDarkMode.value) TrapDarkMode else TrapLight
    
    val River: Color
        @Composable get() = if (AppSettings.isDarkMode.value) RiverDarkMode else RiverLight
    
    val DisabledButton: Color
        @Composable get() = if (AppSettings.isDarkMode.value) DisabledButtonDarkMode else DisabledButtonLight
    
    val DisabledButtonText: Color
        @Composable get() = if (AppSettings.isDarkMode.value) DisabledButtonTextDarkMode else DisabledButtonTextLight
    
    val BuildableHighlight: Color
        @Composable get() = if (AppSettings.isDarkMode.value) BuildableHighlightDarkMode else BuildableHighlightLight
}
