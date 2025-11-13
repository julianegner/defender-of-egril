package com.defenderofegril.utils

import androidx.compose.ui.geometry.Offset
import com.defenderofegril.model.Position
import kotlin.test.Test
import kotlin.test.assertEquals

class HexUtilsTest {
    
    /**
     * Test screenToHexGridPosition with known hex center positions
     * Based on the actual rendering layout from HexagonalMapView.kt
     */
    @Test
    fun testScreenToHexGridPosition_withHexCenters() {
        val hexSize = 40f
        val zoomLevel = 1.0f
        val offsetX = 0f
        val offsetY = 0f
        
        // Calculated hex center positions for hexSize=40:
        // Position(0, 0): x=34.64, y=40.00
        // Position(1, 0): x=93.92, y=40.00
        // Position(0, 1): x=63.74, y=13.00
        // Position(1, 1): x=123.02, y=13.00
        // Position(0, 2): x=34.64, y=-14.00
        
        // Test even row positions
        var result = screenToHexGridPosition(
            Offset(34.64f, 40.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0)")
        
        result = screenToHexGridPosition(
            Offset(93.92f, 40.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(1, 0), result, "Should map to Position(1, 0)")
        
        // Test odd row positions
        result = screenToHexGridPosition(
            Offset(63.74f, 13.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 1), result, "Should map to Position(0, 1)")
        
        result = screenToHexGridPosition(
            Offset(123.02f, 13.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(1, 1), result, "Should map to Position(1, 1)")
        
        // Test another even row
        result = screenToHexGridPosition(
            Offset(34.64f, -14.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 2), result, "Should map to Position(0, 2)")
    }
    
    /**
     * Test screenToHexGridPosition with zoom and pan transformations
     */
    @Test
    fun testScreenToHexGridPosition_withZoomAndPan() {
        val hexSize = 40f
        val zoomLevel = 2.0f
        val offsetX = 50f
        val offsetY = 30f
        
        // Position(0, 0) center in content space: (34.64, 40.00)
        // In screen space with zoom=2.0 and offset=(50, 30):
        // screenX = contentX * zoom + offsetX = 34.64 * 2.0 + 50 = 119.28
        // screenY = contentY * zoom + offsetY = 40.00 * 2.0 + 30 = 110.00
        
        val result = screenToHexGridPosition(
            Offset(119.28f, 110.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0) with zoom and pan")
    }
    
    /**
     * Test screenToHexGridPosition with positions near hex boundaries
     */
    @Test
    fun testScreenToHexGridPosition_nearBoundaries() {
        val hexSize = 40f
        val zoomLevel = 1.0f
        val offsetX = 0f
        val offsetY = 0f
        
        // Test a position slightly offset from Position(0, 0) center (34.64, 40.00)
        // Should still round to Position(0, 0)
        var result = screenToHexGridPosition(
            Offset(40.0f, 35.0f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Position near (0,0) center should map to Position(0, 0)")
        
        // Test a position between hex (0, 0) and (1, 0)
        // Closer to (1, 0) at x=93.92, should map there
        result = screenToHexGridPosition(
            Offset(80.0f, 40.0f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(1, 0), result, "Position closer to (1,0) should map to Position(1, 0)")
    }
}
