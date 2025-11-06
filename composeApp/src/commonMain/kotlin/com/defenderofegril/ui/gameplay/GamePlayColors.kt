package com.defenderofegril.ui.gameplay

import androidx.compose.ui.graphics.Color

/**
 * Color palette for gameplay UI components.
 * Centralizes all color values used throughout the gameplay screens for consistency and maintainability.
 */
object GamePlayColors {
    // Primary action and state colors
    val Success = Color(0xFF4CAF50)      // Green - positive actions, ready state, target
    val Warning = Color(0xFFFF9800)      // Orange - warnings, building, spawns
    val Error = Color(0xFFF44336)        // Red - errors, enemies, danger
    val Info = Color(0xFF2196F3)         // Blue - info, towers ready
    
    // Extended palette for specific uses
    val InfoDark = Color(0xFF1976D2)     // Dark blue - selected towers
    val InfoLight = Color(0xFF7986CB)    // Light blue - towers with no actions
    val WarningDeep = Color(0xFFFF5722)  // Deep orange - urgent warnings, fireball effects
    val WarningDark = Color(0xFFFF6F00)  // Dark orange - special warnings
    val ErrorDark = Color(0xFFD32F2F)    // Dark red - attack buttons, active enemies
    val Yellow = Color(0xFFFFEB3B)       // Yellow - selected items, attack type
    
    // Map terrain colors
    val BuildIsland = Color(0xFF8BC34A)  // Light green - build islands
    val BuildStrip = Color(0xFFA5D6A7)   // Medium green - build strips adjacent to path
    val Path = Color(0xFFFFF8DC)         // Cream/beige - enemy path
    val NonPlayable = Color(0xFFE0E0E0)  // Light gray - off-path non-playable areas
    val Building = Color(0xFF9E9E9E)     // Gray - towers under construction
    
    // Background colors for cards
    val EnemyCardBackground = Color(0xFFFFEBEE)     // Light red - enemy info cards
    val UpcomingCardBackground = Color(0xFFFFF3E0)  // Light orange - upcoming/planned items
    val DangerCardBackground = Color(0xFFFFCDD2)    // Light red-pink - danger/warning cards
    
    // Special effect colors
    val Trap = Color(0xFF8B4513)         // Brown - dwarven mine traps
}
