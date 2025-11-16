package de.egril.defender.utils

import androidx.compose.ui.geometry.Offset
import de.egril.defender.model.Position
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
        // Formula: center_y = y * (hexHeight + rowSpacing - 1) + 1 + hexHeight/2
        //        = y * 52 + 41
        // Position(0, 0): x=34.64, y=41.00
        // Position(1, 0): x=93.92, y=41.00
        // Position(0, 1): x=63.74, y=93.00
        // Position(1, 1): x=123.02, y=93.00
        // Position(0, 2): x=34.64, y=145.00
        
        // Test even row positions
        var result = screenToHexGridPosition(
            Offset(34.64f, 41.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0)")
        
        result = screenToHexGridPosition(
            Offset(93.92f, 41.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(1, 0), result, "Should map to Position(1, 0)")
        
        // Test odd row positions
        result = screenToHexGridPosition(
            Offset(63.74f, 93.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 1), result, "Should map to Position(0, 1)")
        
        result = screenToHexGridPosition(
            Offset(123.02f, 93.00f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(1, 1), result, "Should map to Position(1, 1)")
        
        // Test another even row
        result = screenToHexGridPosition(
            Offset(34.64f, 145.00f), 
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
        
        // Position(0, 0) center in content space: (34.64, 41.00)
        // In screen space with zoom=2.0 and offset=(50, 30):
        // screenX = contentX * zoom + offsetX = 34.64 * 2.0 + 50 = 119.28
        // screenY = contentY * zoom + offsetY = 41.00 * 2.0 + 30 = 112.00
        
        val result = screenToHexGridPosition(
            Offset(119.28f, 112.00f), 
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
        
        // Test a position slightly offset from Position(0, 0) center (34.64, 41.00)
        // Should still round to Position(0, 0)
        var result = screenToHexGridPosition(
            Offset(40.0f, 50.0f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Position near (0,0) center should map to Position(0, 0)")
        
        // Test a position between hex (0, 0) and (1, 0)
        // Closer to (1, 0) at x=93.92, should map there
        result = screenToHexGridPosition(
            Offset(80.0f, 41.0f), 
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(1, 0), result, "Position closer to (1,0) should map to Position(1, 0)")
    }
    
    /**
     * Test screenToHexGridPosition with various zoom levels
     * This test validates that the coordinate conversion works correctly with zoom
     */
    @Test
    fun testScreenToHexGridPosition_variousZoomLevels() {
        val hexSize = 40f
        val offsetX = 0f
        val offsetY = 0f
        
        // Position(0, 0) center in content space: (34.64, 41.00)
        val contentX = 34.64f
        val contentY = 41.00f
        
        // Test with zoom = 0.5x (zoomed out)
        var zoomLevel = 0.5f
        var screenX = contentX * zoomLevel + offsetX  // 17.32
        var screenY = contentY * zoomLevel + offsetY  // 20.5
        var result = screenToHexGridPosition(
            Offset(screenX, screenY),
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0) at zoom 0.5x")
        
        // Test with zoom = 1.5x (zoomed in)
        zoomLevel = 1.5f
        screenX = contentX * zoomLevel + offsetX  // 51.96
        screenY = contentY * zoomLevel + offsetY  // 61.5
        result = screenToHexGridPosition(
            Offset(screenX, screenY),
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0) at zoom 1.5x")
        
        // Test with zoom = 3.0x (max zoom)
        zoomLevel = 3.0f
        screenX = contentX * zoomLevel + offsetX  // 103.92
        screenY = contentY * zoomLevel + offsetY  // 123.0
        result = screenToHexGridPosition(
            Offset(screenX, screenY),
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0) at zoom 3.0x")
    }
    
    /**
     * Test screenToHexGridPosition with zoom and centering adjustment
     * This simulates what happens in MapEditorView when content is centered
     */
    @Test
    fun testScreenToHexGridPosition_withZoomAndCentering() {
        val hexSize = 40f
        val zoomLevel = 2.0f
        
        // Simulate container and content sizes
        val containerWidth = 800f
        val containerHeight = 600f
        val contentWidth = 400f
        val contentHeight = 300f
        
        // Calculate scaled dimensions
        val scaledWidth = contentWidth * zoomLevel  // 800
        val scaledHeight = contentHeight * zoomLevel  // 600
        
        // Calculate centering offsets
        val centeringOffsetX = (containerWidth - scaledWidth) / 2f  // 0
        val centeringOffsetY = (containerHeight - scaledHeight) / 2f  // 0
        
        // Position(0, 0) center in content space: (34.64, 41.00)
        val contentX = 34.64f
        val contentY = 41.00f
        
        // With no pan offset
        val offsetX = 0f
        val offsetY = 0f
        
        // Screen position (what user sees after zoom and centering)
        val rawScreenX = contentX * zoomLevel + offsetX  // 69.28
        val rawScreenY = contentY * zoomLevel + offsetY  // 82.0
        
        // Adjust for centering (what MapEditorView does)
        val adjustedScreenX = rawScreenX + centeringOffsetX  // 69.28
        val adjustedScreenY = rawScreenY + centeringOffsetY  // 82.0
        
        val result = screenToHexGridPosition(
            Offset(adjustedScreenX, adjustedScreenY),
            offsetX, offsetY, zoomLevel, hexSize
        )
        assertEquals(Position(0, 0), result, "Should map to Position(0, 0) with zoom and centering")
    }
}
