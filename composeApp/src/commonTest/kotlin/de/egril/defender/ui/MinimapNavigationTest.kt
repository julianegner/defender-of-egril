package de.egril.defender.ui

import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test minimap navigation calculations - converting minimap drag gestures to viewport offsets
 */
class MinimapNavigationTest {
    
    @Test
    fun testMinimapDragToViewportOffsetConversion() {
        // Simulates the calculation from HexagonMinimap when dragging on the minimap
        // Setup: Viewport is zoomed in so only 50% of map is visible
        val containerSize = IntSize(800, 600)
        val contentSize = IntSize(1600, 1200)
        val scale = 1.0f
        val minimapSizeDp = 120f
        
        // Calculate expected values
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale
        val viewportWidthRatio = (containerSize.width.toFloat() / scaledContentWidth).coerceAtMost(1f)
        val viewportHeightRatio = (containerSize.height.toFloat() / scaledContentHeight).coerceAtMost(1f)
        
        assertEquals(0.5f, viewportWidthRatio, 0.001f, "50% of map should be visible in width")
        assertEquals(0.5f, viewportHeightRatio, 0.001f, "50% of map should be visible in height")
        
        // Maximum offsets the viewport can have
        val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
        val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
        
        assertEquals(400f, maxOffsetX, 0.001f, "Max X offset should be 400")
        assertEquals(300f, maxOffsetY, 0.001f, "Max Y offset should be 300")
        
        // Simulate dragging 30 pixels to the right on the minimap
        // This should move the viewport to the right (negative offset since offsetX is positive when panned left)
        val dragAmountX = 30f
        val dragAmountY = 0f
        
        // Current offset (starting at left edge, maximum positive offset)
        val currentOffsetX = maxOffsetX
        val currentOffsetY = 0f
        
        // Convert drag in minimap coordinates to viewport offsets
        val dragXFraction = dragAmountX / (minimapSizeDp * (1f - viewportWidthRatio))
        val dragYFraction = dragAmountY / (minimapSizeDp * (1f - viewportHeightRatio))
        
        // minimap has 120dp, and viewport can move within (1.0 - 0.5) = 0.5 of that = 60dp
        assertEquals(0.5f, dragXFraction, 0.001f, "Dragging 30px on 60px movable area = 0.5 fraction")
        
        // Convert drag fraction to normalized offset change (-1 to 1 range)
        val deltaNormalizedX = dragXFraction * 2f
        val deltaNormalizedY = dragYFraction * 2f
        
        assertEquals(1.0f, deltaNormalizedX, 0.001f, "0.5 fraction should map to 1.0 normalized change")
        
        // Convert normalized offset change to actual offset change
        val deltaOffsetX = -deltaNormalizedX * maxOffsetX
        val deltaOffsetY = -deltaNormalizedY * maxOffsetY
        
        assertEquals(-400f, deltaOffsetX, 0.001f, "Should move by -400 (full range)")
        
        // Apply the offset change
        val newOffsetX = currentOffsetX + deltaOffsetX
        val newOffsetY = currentOffsetY + deltaOffsetY
        
        assertEquals(0f, newOffsetX, 0.001f, "Should move from max positive to 0")
        
        // Constrain to valid range
        val constrainedX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
        val constrainedY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
        
        assertEquals(0f, constrainedX, 0.001f, "Final offset should be 0")
        assertEquals(0f, constrainedY, 0.001f, "Final offset should be 0")
    }
    
    @Test
    fun testMinimapDragConstraints() {
        // Test that dragging beyond the minimap boundaries properly constrains the viewport offset
        val containerSize = IntSize(800, 600)
        val contentSize = IntSize(1600, 1200)
        val scale = 1.0f
        val minimapSizeDp = 120f
        
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale
        val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
        val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
        
        // Test dragging too far right (should constrain to -maxOffsetX)
        val excessiveOffset = maxOffsetX * 2
        val constrainedExcessive = excessiveOffset.coerceIn(-maxOffsetX, maxOffsetX)
        assertEquals(maxOffsetX, constrainedExcessive, 0.001f, "Should constrain to max positive offset")
        
        // Test dragging too far left (should constrain to maxOffsetX)
        val negativeExcessiveOffset = -maxOffsetX * 2
        val constrainedNegativeExcessive = negativeExcessiveOffset.coerceIn(-maxOffsetX, maxOffsetX)
        assertEquals(-maxOffsetX, constrainedNegativeExcessive, 0.001f, "Should constrain to max negative offset")
    }
    
    @Test
    fun testMinimapDragWhenFullyZoomedOut() {
        // When fully zoomed out (scale = 1, content fits in container), viewport shouldn't move
        val containerSize = IntSize(800, 600)
        val contentSize = IntSize(800, 600)
        val scale = 1.0f
        
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale
        val viewportWidthRatio = (containerSize.width.toFloat() / scaledContentWidth).coerceAtMost(1f)
        val viewportHeightRatio = (containerSize.height.toFloat() / scaledContentHeight).coerceAtMost(1f)
        
        // When content fits exactly in container, viewport ratio should be 1.0
        assertEquals(1.0f, viewportWidthRatio, 0.001f, "Viewport should show entire width")
        assertEquals(1.0f, viewportHeightRatio, 0.001f, "Viewport should show entire height")
        
        // Max offset should be minimal (0.01f due to coerceAtLeast)
        val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
        val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
        
        assertEquals(0.01f, maxOffsetX, 0.001f, "Max offset should be minimal")
        assertEquals(0.01f, maxOffsetY, 0.001f, "Max offset should be minimal")
    }
    
    @Test
    fun testMinimapDragWithHighZoom() {
        // When zoomed in 3x, only 1/3 of the map is visible
        val containerSize = IntSize(800, 600)
        val contentSize = IntSize(1600, 1200)
        val scale = 3.0f
        val minimapSizeDp = 120f
        
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale
        val viewportWidthRatio = (containerSize.width.toFloat() / scaledContentWidth).coerceAtMost(1f)
        val viewportHeightRatio = (containerSize.height.toFloat() / scaledContentHeight).coerceAtMost(1f)
        
        // At 3x zoom, viewport should show 1/6 of the map (800 / (1600 * 3) = 800/4800 ≈ 0.167)
        assertTrue(viewportWidthRatio < 0.17f, "Viewport should show less than 17% of width at 3x zoom")
        assertTrue(viewportHeightRatio < 0.17f, "Viewport should show less than 17% of height at 3x zoom")
        
        // Max offset should be larger with higher zoom
        val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
        val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
        
        assertTrue(maxOffsetX > 1000f, "Max X offset should be large at 3x zoom")
        assertTrue(maxOffsetY > 700f, "Max Y offset should be large at 3x zoom")
        
        // Test that drag amount conversion is correct
        // The minimap has 120dp, and viewport can move within (1.0 - viewportRatio) of that
        val movableAreaX = minimapSizeDp * (1f - viewportWidthRatio)
        val movableAreaY = minimapSizeDp * (1f - viewportHeightRatio)
        
        // Movable area should be close to the full minimap size when highly zoomed
        // At 3x zoom with our setup, movableArea = 120 * (1 - 800/4800) = 120 * 0.8333 = 100
        assertTrue(movableAreaX > 99f, "Movable area should be large when zoomed in")
        assertTrue(movableAreaY > 99f, "Movable area should be large when zoomed in")
    }
}
