package de.egril.defender.ui.gameplay

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
    
    // Outer circles - drawn on neighbor tiles for AREA and LASTING attack types
    // Adjusted radii: middle circle (70f) passes through tile centers,
    // inner and outer circles evenly spaced around it
    const val OUTER_CIRCLE_1_RADIUS = 52f   // Inner ring
    const val OUTER_CIRCLE_2_RADIUS = 70f   // Middle ring (passes through neighbor tile centers)
    const val OUTER_CIRCLE_3_RADIUS = 88f   // Outer ring

    const val OUTER_CIRCLE_STROKE_WIDTH = 3f
}
