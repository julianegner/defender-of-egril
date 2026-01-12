package de.egril.defender.ui.hexagon

/**
 * Constants for hexagonal grid layout spacing.
 * 
 * These values are critical for proper alignment between:
 * 1. Visual rendering in HexagonalMapView.kt
 * 2. Mouse-to-grid position conversion in HexUtils.kt
 * 
 * IMPORTANT: These values must be kept in sync. Any changes here will automatically
 * affect both the rendering and the mouse position calculations.
 * 
 * The spacing values are used to create overlapping hexagons in the grid layout,
 * which is necessary for proper hexagonal tessellation.
 */
object HexagonalGridConstants {
    /**
     * Additional vertical spacing adjustment for hexagon rows.
     * 
     * This value is ADDED to the base vertical spacing calculation:
     * `verticalArrangement = Arrangement.spacedBy((-hexHeight + verticalSpacing + VERTICAL_SPACING_ADJUSTMENT).dp)`
     * 
     * For hexSize=40:
     * - hexHeight = 80
     * - verticalSpacing = 60 (hexHeight * 0.75)
     * - Final spacing = -80 + 60 - 7 = -27
     */
    const val VERTICAL_SPACING_ADJUSTMENT = -7f
    
    /**
     * Horizontal spacing between hexagons in the same row.
     * 
     * This value controls how much hexagons overlap horizontally:
     * `horizontalArrangement = Arrangement.spacedBy(HORIZONTAL_SPACING.dp)`
     * 
     * For hexSize=40:
     * - hexWidth ≈ 69.28 (hexSize * sqrt(3))
     * - Final spacing = -10
     */
    const val HORIZONTAL_SPACING = -10f
    
    /**
     * Horizontal offset for odd-numbered rows (y % 2 == 1).
     * 
     * This creates the characteristic hexagonal offset pattern where
     * odd rows are shifted horizontally relative to even rows.
     * 
     * Value as a proportion of hexWidth: 0.42
     */
    const val ODD_ROW_OFFSET_RATIO = 0.42f
}
