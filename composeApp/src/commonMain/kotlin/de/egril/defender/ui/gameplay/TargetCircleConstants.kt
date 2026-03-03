package de.egril.defender.ui.gameplay

import androidx.compose.ui.graphics.Color

/**
 * Constants for target circle visualization on tiles.
 * These circles indicate which enemy is selected for attack.
 */
object TargetCircleConstants {
    // Inner circles - drawn on the central target tile for all attack types
    const val INNER_CIRCLE_1_RADIUS = 6f
    const val INNER_CIRCLE_2_RADIUS = 14f
    const val INNER_CIRCLE_3_RADIUS = 22f
    const val INNER_CIRCLE_STROKE_WIDTH = 3f
    
    // Outer circles - drawn on distance 1 neighbor tiles for AREA and LASTING attack types
    // Adjusted radii: middle circle (70f) passes through tile centers,
    // inner and outer circles evenly spaced around it
    const val OUTER_CIRCLE_1_RADIUS = 52f   // Inner ring
    const val OUTER_CIRCLE_2_RADIUS = 70f   // Middle ring (passes through neighbor tile centers)
    const val OUTER_CIRCLE_3_RADIUS = 88f   // Outer ring
    
    // Extended outer circles - drawn on distance 2 neighbor tiles for level 20+ towers
    // These radii are approximately half a hexagon height larger (about 30-35f more)
    // to properly reach the tiles at distance 2 from the center
    const val EXTENDED_OUTER_CIRCLE_1_RADIUS = 117f   // Inner ring for distance 2
    const val EXTENDED_OUTER_CIRCLE_2_RADIUS = 135f   // Middle ring for distance 2
    const val EXTENDED_OUTER_CIRCLE_3_RADIUS = 153f   // Outer ring for distance 2

    const val OUTER_CIRCLE_STROKE_WIDTH = 3f

    // ATTACK_AREA spell targeting preview constants
    /** Hex radius of the ATTACK_AREA spell effect (matches the range in executeSpellEffect) */
    const val ATTACK_AREA_SPELL_RADIUS = 2
    /** Color used for the ATTACK_AREA spell targeting preview circles */
    val ATTACK_AREA_SPELL_COLOR = Color(0xFF9C27B0)  // Purple to distinguish spell from tower attacks
    
    // BOMB spell targeting/range preview constants
    /** Hex radius of the BOMB spell explosion effect */
    const val BOMB_SPELL_RADIUS = 3
    /** Color used for bomb range preview circles (orange/fire) */
    val BOMB_SPELL_COLOR = Color(0xFFFF6F00)
}
