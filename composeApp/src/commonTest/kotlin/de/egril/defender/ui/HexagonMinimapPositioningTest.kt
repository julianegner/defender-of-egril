package de.egril.defender.ui

import de.egril.defender.model.Position
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test hexagonal positioning calculations used in the minimap
 */
class HexagonMinimapPositioningTest {
    
    @Test
    fun testHexagonPositioningOnEvenRow() {
        // For even rows (y % 2 == 0), there should be no horizontal offset
        val position = Position(3, 0)  // Row 0 is even
        
        // Simulate the calculation from HexagonMinimap
        val baseHexSize = 1.0f
        val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
        val baseHexHeight = 2.0f * baseHexSize
        val baseVerticalSpacing = baseHexHeight * 0.75f
        
        val offsetXCanvas = 0f
        val offsetYCanvas = 0f
        val hexWidth = baseHexWidth
        val hexHeight = baseHexHeight
        val verticalSpacing = baseVerticalSpacing
        
        val row = position.y
        val col = position.x
        
        // Calculate hex center position - even row should have no X offset
        val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
        val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
        val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2
        
        // For even row, offsetXHex should be 0
        assertEquals(0.0f, offsetXHex, "Even rows should have no horizontal offset")
        
        // CenterX should be at position 3.5 hex widths (3 * hexWidth + hexWidth/2)
        val expectedCenterX = 3f * hexWidth + hexWidth / 2
        assertEquals(expectedCenterX, centerX, 0.001f, "Even row X position should match expected")
    }
    
    @Test
    fun testHexagonPositioningOnOddRow() {
        // For odd rows (y % 2 == 1), there should be a horizontal offset of hexWidth/2
        val position = Position(3, 1)  // Row 1 is odd
        
        // Simulate the calculation from HexagonMinimap
        val baseHexSize = 1.0f
        val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
        val baseHexHeight = 2.0f * baseHexSize
        val baseVerticalSpacing = baseHexHeight * 0.75f
        
        val offsetXCanvas = 0f
        val offsetYCanvas = 0f
        val hexWidth = baseHexWidth
        val hexHeight = baseHexHeight
        val verticalSpacing = baseVerticalSpacing
        
        val row = position.y
        val col = position.x
        
        // Calculate hex center position - odd row should have X offset
        val offsetXHex = if (row % 2 == 1) hexWidth / 2 else 0.0f
        val centerX = offsetXCanvas + col * hexWidth + offsetXHex + hexWidth / 2
        val centerY = offsetYCanvas + row * verticalSpacing + hexHeight / 2
        
        // For odd row, offsetXHex should be hexWidth/2
        assertEquals(hexWidth / 2, offsetXHex, 0.001f, "Odd rows should have horizontal offset of hexWidth/2")
        
        // CenterX should be at position 4.0 hex widths (3 * hexWidth + hexWidth/2 + hexWidth/2)
        val expectedCenterX = 3f * hexWidth + hexWidth  // 3 * hexWidth + offsetXHex + hexWidth/2
        assertEquals(expectedCenterX, centerX, 0.001f, "Odd row X position should match expected")
    }
    
    @Test
    fun testVerticalPositioning() {
        // Test that vertical positioning uses verticalSpacing correctly
        val position0 = Position(0, 0)
        val position1 = Position(0, 1)
        val position2 = Position(0, 2)
        
        val baseHexSize = 1.0f
        val baseHexHeight = 2.0f * baseHexSize
        val baseVerticalSpacing = baseHexHeight * 0.75f
        
        val offsetYCanvas = 0f
        val hexHeight = baseHexHeight
        val verticalSpacing = baseVerticalSpacing
        
        // Calculate Y positions
        val centerY0 = offsetYCanvas + position0.y * verticalSpacing + hexHeight / 2
        val centerY1 = offsetYCanvas + position1.y * verticalSpacing + hexHeight / 2
        val centerY2 = offsetYCanvas + position2.y * verticalSpacing + hexHeight / 2
        
        // Verify spacing between rows
        val spacing01 = centerY1 - centerY0
        val spacing12 = centerY2 - centerY1
        
        assertEquals(verticalSpacing, spacing01, 0.001f, "Vertical spacing should be consistent")
        assertEquals(verticalSpacing, spacing12, 0.001f, "Vertical spacing should be consistent")
        
        // Vertical spacing should be 75% of hex height
        assertEquals(baseHexHeight * 0.75f, verticalSpacing, 0.001f, "Vertical spacing should be 75% of hex height")
    }
    
    @Test
    fun testAdjacentHexagonsOnSameRow() {
        // Adjacent hexagons on the same row should be exactly hexWidth apart
        val position1 = Position(0, 0)
        val position2 = Position(1, 0)
        
        val baseHexSize = 1.0f
        val baseHexWidth = (sqrt(3.0) * baseHexSize).toFloat()
        val hexWidth = baseHexWidth
        
        val offsetXCanvas = 0f
        
        val centerX1 = offsetXCanvas + position1.x * hexWidth + hexWidth / 2
        val centerX2 = offsetXCanvas + position2.x * hexWidth + hexWidth / 2
        
        val distance = centerX2 - centerX1
        
        assertEquals(hexWidth, distance, 0.001f, "Adjacent hexagons on same row should be hexWidth apart")
    }
}
