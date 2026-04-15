package de.egril.defender.ui.gameplay

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Constants used across gameplay UI components for consistent styling.
 * Centralizes commonly used values for icon sizes, spacing, padding, and text sizes.
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
        
        /** Extra large icons (e.g., tower type icons) */
        val ExtraLarge = 28.dp
    }
    
    /**
     * Standard spacing values for consistent layout.
     */
    object Spacing {
        /** Space between icon and text in IconTextRow pattern */
        val IconText = 4.dp
        
        /** Small spacing */
        val Small = 6.dp
        
        /** Standard spacing between items */
        val Items = 8.dp
        
        /** Spacing between sections */
        val Sections = 12.dp
    }
    
    /**
     * Standard padding values for consistent layout.
     */
    object Padding {
        /** Small padding (e.g., compact buttons) */
        val Small = 4.dp
        
        /** Medium padding (e.g., cards) */
        val Medium = 6.dp
        
        /** Standard padding */
        val Standard = 8.dp
        
        /** Large padding */
        val Large = 12.dp
    }
    
    /**
     * Standard button dimensions.
     */
    object ButtonSizes {
        /** Standard button height for action buttons */
        val ActionHeight = 60.dp
        
        /** Standard button width for tower action buttons */
        val ActionWidth = 240.dp
        
        /** Compact button height */
        val CompactHeight = 32.dp
    }
    
    /**
     * Standard text sizes used throughout the gameplay UI.
     */
    object TextSizes {
        /** Small text (e.g., labels) */
        val Small = 10.sp
        
        /** Body text */
        val Body = 12.sp
        
        /** Medium text */
        val Medium = 14.sp
        
        /** Large text */
        val Large = 16.sp
        
        /** Title text */
        val Title = 18.sp
    }
    
    /**
     * Icon sizes used for elements displayed on map tiles.
     * These sizes are used for traps, barricades, and other tile-based elements.
     */
    object TileIconSizes {
        /** Icon size for traps on tiles (dwarven traps, magical traps) */
        val Trap = 48.dp
        
        /** Icon size for barricades on tiles */
        val Barricade = 48.dp
        
        /** Icon size for trap preview when hovering during placement mode */
        val TrapPreview = 48.dp

        /** Icon size for static healing effect overlay (shown when animations are disabled) */
        val HealingEffect = 24.dp

        /** Icon size for static damage effect overlay (shown when animations are disabled) */
        val DamageEffect = 24.dp
    }

    /**
     * Timing constants (milliseconds) used to sequence overlapping animations.
     * These values match the durations encoded in the corresponding Lottie JSON files.
     */
    object AnimationTimings {
        /** Duration of the tower attack impact flash animation (~670 ms / 20 frames @ 30 fps). */
        const val ATTACK_IMPACT_DURATION_MS = 670L

        /** Delay before the arrow/bolt hit animation so the projectile visibly arrives first. */
        const val ARROW_FLIGHT_DELAY_MS = 900L

        /** Delay before the ballista rock hit animation so the rock visibly arrives first. */
        const val BALLISTA_FLIGHT_DELAY_MS = 1000L

        /** Duration of the enemy death Lottie animation (30 frames @ 30 fps = 1 000 ms). */
        const val ENEMY_DEATH_ANIMATION_DURATION_MS = 1000L

        /** Pause between the death animation finishing and the coin-gain animation starting. */
        const val COIN_GAIN_DELAY_AFTER_DEATH_MS = 400L
    }
}
