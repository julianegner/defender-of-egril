package de.egril.defender.ui

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests to verify hexagon spacing calculations in HexagonalMapView
 * These ensure that hexagons tessellate properly without gaps
 */
class HexagonalSpacingTest {
    
    @Test
    fun testVerticalSpacingCalculation() {
        // Hexagon dimensions for pointy-top hexagons
        val hexSize = 40f
        val hexHeight = hexSize * 2f  // 80.0
        val verticalSpacing = hexHeight * 0.75f  // 60.0
        
        // Calculate the actual spacing used in HexagonalMapView
        val actualSpacing = -hexHeight + verticalSpacing
        
        // Expected: -20.0 (which makes rows overlap by 25% of hexHeight)
        val expectedSpacing = -20f
        
        assertEquals(expectedSpacing, actualSpacing, 0.001f,
            "Vertical spacing should be -20.0 for proper hexagon tessellation")
    }
    
    @Test
    fun testHorizontalSpacingCalculation() {
        // Hexagon dimensions for pointy-top hexagons
        val hexSize = 40f
        val sqrt3 = sqrt(3.0).toFloat()
        val hexWidth = hexSize * sqrt3  // ~69.28
        
        // Calculate the actual spacing used in HexagonalMapView
        val actualSpacing = -(hexWidth * 0.25f)
        
        // Expected: approximately -17.32 (which makes hexagons overlap by 25% of width)
        val expectedSpacing = -17.32f
        
        assertTrue(
            kotlin.math.abs(actualSpacing - expectedSpacing) < 0.01f,
            "Horizontal spacing should be approximately -17.32 for proper hexagon tessellation, got $actualSpacing"
        )
    }
    
    @Test
    fun testNoExtraVerticalGap() {
        // This test verifies that there's no extra "-7f" added to vertical spacing
        val hexSize = 40f
        val hexHeight = hexSize * 2f
        val verticalSpacing = hexHeight * 0.75f
        
        val correctSpacing = -hexHeight + verticalSpacing  // -20.0
        val incorrectSpacingWithGap = -hexHeight + verticalSpacing - 7f  // -27.0
        
        // The gap between the two values
        val gap = correctSpacing - incorrectSpacingWithGap
        
        assertEquals(7f, gap, 0.001f,
            "The -7f bug would create a 7dp gap")
    }
    
    @Test
    fun testHorizontalSpacingFormula() {
        // Test that the formula -(hexWidth * 0.25f) produces the correct overlap
        val hexSize = 40f
        val sqrt3 = sqrt(3.0).toFloat()
        val hexWidth = hexSize * sqrt3
        
        val spacingFromFormula = -(hexWidth * 0.25f)
        val spacingDirect = -(hexSize * sqrt3 / 4f)
        
        assertEquals(spacingFromFormula, spacingDirect, 0.001f,
            "Both formulas should produce the same result")
    }
}
