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
    const val OUTER_CIRCLE_1_RADIUS = 40f
    const val OUTER_CIRCLE_2_RADIUS = 70f
    const val OUTER_CIRCLE_3_RADIUS = 100f

    // const val OUTER_CIRCLE_1_RADIUS = 80f
    // const val OUTER_CIRCLE_2_RADIUS = 110f
    // const val OUTER_CIRCLE_3_RADIUS = 140f
    const val OUTER_CIRCLE_STROKE_WIDTH = 3f
}
