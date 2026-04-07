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
    
    @Test
    fun testMinimapDragOnSmallMap_OnlyWidthScrollable() {
        // Simulates the "straight map" scenario (45 wide × 11 tall)
        // Where width extends beyond viewport but height fits fully
        // This is the reported bug case
        val containerSize = IntSize(800, 600)
        val contentSize = IntSize(2000, 400)  // Wide but short content
        val scale = 1.0f
        val minimapSizeDp = 120f
        
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale
        val viewportWidthRatio = (containerSize.width.toFloat() / scaledContentWidth).coerceAtMost(1f)
        val viewportHeightRatio = (containerSize.height.toFloat() / scaledContentHeight).coerceAtMost(1f)
        
        // Width should need scrolling (ratio < 1.0)
        assertTrue(viewportWidthRatio < 1.0f, "Width should be partially visible: $viewportWidthRatio")
        assertEquals(0.4f, viewportWidthRatio, 0.001f, "Width ratio should be 800/2000 = 0.4")
        
        // Height should fit fully (ratio = 1.0)
        assertEquals(1.0f, viewportHeightRatio, 0.001f, "Height should be fully visible")
        
        // Max offsets
        val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
        val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
        
        // X offset should be substantial (content wider than container)
        assertEquals(600f, maxOffsetX, 0.001f, "Max X offset should be (2000-800)/2 = 600")
        
        // Y offset should be minimal (content fits in container)
        assertEquals(0.01f, maxOffsetY, 0.001f, "Max Y offset should be minimal (coerceAtLeast)")
        
        // Test drag navigation in X direction should work
        val dragAmountX = 30f
        val currentOffsetX = maxOffsetX  // Start at left edge
        
        val movableX = (1f - viewportWidthRatio).coerceAtLeast(0.001f)
        val dragXFraction = dragAmountX / (minimapSizeDp * movableX)
        
        // Movable area in X should be (1.0 - 0.4) * 120 = 72dp
        assertEquals(72f, minimapSizeDp * movableX, 0.001f, "Movable X area should be 72dp")
        
        // Dragging 30px on 72px movable area = 0.4166 fraction
        assertTrue(dragXFraction > 0.4f && dragXFraction < 0.42f, "Drag fraction should be ~0.416")
        
        val deltaNormalizedX = dragXFraction * 2f
        val deltaOffsetX = -deltaNormalizedX * maxOffsetX
        val newOffsetX = currentOffsetX + deltaOffsetX
        
        // Should move from 600 towards center
        assertTrue(newOffsetX < currentOffsetX, "Offset should decrease (move right)")
        assertTrue(newOffsetX > 0f, "Offset should still be positive")
    }
    
    @Test
    fun testMinimapDragOnSmallMap_OnlyHeightScrollable() {
        // Test the opposite case: height scrollable but width fits fully
        // This ensures symmetry in the fix
        val containerSize = IntSize(800, 600)
        val contentSize = IntSize(600, 2000)  // Tall but narrow content
        val scale = 1.0f
        val minimapSizeDp = 120f
        
        val scaledContentWidth = contentSize.width * scale
        val scaledContentHeight = contentSize.height * scale
        val viewportWidthRatio = (containerSize.width.toFloat() / scaledContentWidth).coerceAtMost(1f)
        val viewportHeightRatio = (containerSize.height.toFloat() / scaledContentHeight).coerceAtMost(1f)
        
        // Width should fit fully (ratio = 1.0)
        assertEquals(1.0f, viewportWidthRatio, 0.001f, "Width should be fully visible")
        
        // Height should need scrolling (ratio < 1.0)
        assertTrue(viewportHeightRatio < 1.0f, "Height should be partially visible: $viewportHeightRatio")
        assertEquals(0.3f, viewportHeightRatio, 0.001f, "Height ratio should be 600/2000 = 0.3")
        
        // Max offsets
        val maxOffsetX = ((scaledContentWidth - containerSize.width) / 2).coerceAtLeast(0.01f)
        val maxOffsetY = ((scaledContentHeight - containerSize.height) / 2).coerceAtLeast(0.01f)
        
        // X offset should be minimal (content fits in container)
        assertEquals(0.01f, maxOffsetX, 0.001f, "Max X offset should be minimal (coerceAtLeast)")
        
        // Y offset should be substantial (content taller than container)
        assertEquals(700f, maxOffsetY, 0.001f, "Max Y offset should be (2000-600)/2 = 700")
        
        // Test drag navigation in Y direction should work
        val dragAmountY = 30f
        val currentOffsetY = maxOffsetY  // Start at top edge
        
        val movableY = (1f - viewportHeightRatio).coerceAtLeast(0.001f)
        val dragYFraction = dragAmountY / (minimapSizeDp * movableY)
        
        // Movable area in Y should be (1.0 - 0.3) * 120 = 84dp
        assertEquals(84f, minimapSizeDp * movableY, 0.001f, "Movable Y area should be 84dp")
        
        // Dragging 30px on 84px movable area ≈ 0.357 fraction
        assertTrue(dragYFraction > 0.35f && dragYFraction < 0.36f, "Drag fraction should be ~0.357")
        
        val deltaNormalizedY = dragYFraction * 2f
        val deltaOffsetY = -deltaNormalizedY * maxOffsetY
        val newOffsetY = currentOffsetY + deltaOffsetY
        
        // Should move from 700 towards center
        assertTrue(newOffsetY < currentOffsetY, "Offset should decrease (move down)")
        assertTrue(newOffsetY > 0f, "Offset should still be positive")
    }
}
