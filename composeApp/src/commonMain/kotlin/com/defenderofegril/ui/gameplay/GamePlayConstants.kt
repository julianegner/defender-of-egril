package com.defenderofegril.ui.gameplay

import androidx.compose.ui.unit.dp

/**
 * Constants used across gameplay UI components for consistent styling.
 * Centralizes commonly used values for icon sizes, spacing, and padding.
 * 
 * For color constants, see [GamePlayColors].
 */
object GamePlayConstants {
    /**
     * Standard icon sizes used throughout the gameplay UI.
     */
    object IconSizes {
        /** Small icons (e.g., in stat displays) */
        val Small = 12.dp
        
        /** Medium icons (e.g., in compact headers) */
        val Medium = 16.dp
        
        /** Large icons (e.g., in expanded headers) */
        val Large = 20.dp
    }
    
    /**
     * Standard spacing values for consistent layout.
     */
    object Spacing {
        /** Space between icon and text in IconTextRow pattern */
        val IconText = 4.dp
        
        /** Standard spacing between items */
        val Items = 8.dp
        
        /** Spacing between sections */
        val Sections = 12.dp
    }
}
