package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test viewport ratio calculations for the minimap
 * These calculations determine the size of the viewport rectangle shown on the minimap
 * The ratio is calculated as: containerSize / (contentSize * scale)
 */
class MinimapViewportRatioTest {
    
    @Test
    fun testViewportRatioAtNormalZoom() {
        // At scale = 1.0 (normal zoom), when container equals content, viewport shows 100% of the map
        val containerSize = 1000f
        val contentSize = 1000f
        val scale = 1.0f
        val expectedRatio = 1.0f
        
        val scaledContentSize = contentSize * scale
        val actualRatio = (containerSize / scaledContentSize).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "At normal zoom (scale=1.0) with equal container/content, viewport should show 100% of map")
    }
    
    @Test
    fun testViewportRatioWhenZoomedIn() {
        // At scale = 2.0 (zoomed in 2x), viewport shows 50% of the map
        val containerSize = 1000f
        val contentSize = 1000f
        val scale = 2.0f
        val expectedRatio = 0.5f  // container / (content * 2.0) = 1000 / 2000 = 0.5
        
        val scaledContentSize = contentSize * scale
        val actualRatio = (containerSize / scaledContentSize).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When zoomed in 2x (scale=2.0), viewport should show 50% of map")
    }
    
    @Test
    fun testViewportRatioWhenZoomedInMax() {
        // At scale = 3.0 (max zoom in), viewport shows 33% of the map
        val containerSize = 1000f
        val contentSize = 1000f
        val scale = 3.0f
        val expectedRatio = 1f / 3f  // ~0.333
        
        val scaledContentSize = contentSize * scale
        val actualRatio = (containerSize / scaledContentSize).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When zoomed in 3x (scale=3.0), viewport should show 33% of map")
    }
    
    @Test
    fun testViewportRatioWhenZoomedOut() {
        // At scale = 0.5 (zoomed out 2x), you see more of the map, but viewport cannot exceed 100%
        val containerSize = 1000f
        val contentSize = 1000f
        val scale = 0.5f
        val expectedRatio = 1.0f  // Clamped to 100%
        // Without clamp: container / (content * 0.5) = 1000 / 500 = 2.0, but clamped to 1.0
        
        val scaledContentSize = contentSize * scale
        val actualRatio = (containerSize / scaledContentSize).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When zoomed out (scale=0.5), viewport should be clamped to 100% of minimap")
    }
    
    @Test
    fun testViewportRatioWithSmallerContainer() {
        // When container is smaller than content (even at normal zoom), viewport shows less than 100%
        val containerSize = 500f
        val contentSize = 1000f
        val scale = 1.0f
        val expectedRatio = 0.5f  // container / (content * 1.0) = 500 / 1000 = 0.5
        
        val scaledContentSize = contentSize * scale
        val actualRatio = (containerSize / scaledContentSize).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When container is half of content at normal zoom, viewport should show 50% of map")
    }
    
    @Test
    fun testViewportRatioIsAlwaysValid() {
        // Test that viewport ratio is always between 0 and 1 for any valid scale and sizes
        val containerSize = 800f
        val contentSize = 1000f
        val scales = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f)
        
        for (scale in scales) {
            val scaledContentSize = contentSize * scale
            val ratio = (containerSize / scaledContentSize).coerceAtMost(1f)
            
            assertTrue(ratio > 0f, "Viewport ratio must be positive for scale=$scale")
            assertTrue(ratio <= 1f, "Viewport ratio must not exceed 1.0 for scale=$scale")
        }
    }
    
    @Test
    fun testViewportRatioIncreasesWhenZoomingOut() {
        // When zooming out FROM zoomed in state, viewport ratio should increase
        val containerSize = 1000f
        val contentSize = 1000f
        val scaleZoomedIn = 2.0f
        val scaleNormal = 1.0f
        
        val ratioZoomedIn = (containerSize / (contentSize * scaleZoomedIn)).coerceAtMost(1f)
        val ratioNormal = (containerSize / (contentSize * scaleNormal)).coerceAtMost(1f)
        
        assertTrue(ratioNormal > ratioZoomedIn, 
            "Viewport ratio should increase when zooming out from zoomed in state")
    }
    
    @Test
    fun testViewportRatioDecreasesWhenZoomingIn() {
        // When zooming in FROM normal state, viewport ratio should decrease
        val containerSize = 1000f
        val contentSize = 1000f
        val scaleNormal = 1.0f
        val scaleZoomedIn = 2.0f
        
        val ratioNormal = (containerSize / (contentSize * scaleNormal)).coerceAtMost(1f)
        val ratioZoomedIn = (containerSize / (contentSize * scaleZoomedIn)).coerceAtMost(1f)
        
        assertTrue(ratioZoomedIn < ratioNormal, 
            "Viewport ratio should decrease when zooming in from normal state")
    }
    
    @Test
    fun testViewportPositionCalculation() {
        // Test that viewport position is correctly calculated
        // When offset is at max positive (panned left), normalizedOffset should be -1 (left edge)
        // When offset is at max negative (panned right), normalizedOffset should be +1 (right edge)
        val containerSize = 500f
        val contentSize = 1000f
        val scale = 2.0f
        val scaledContentSize = contentSize * scale  // 2000
        
        // maxOffset = (2000 - 500) / 2 = 750
        val maxOffset = (scaledContentSize - containerSize) / 2
        
        // Test left edge (offset = +750, showing left side of map)
        val offsetLeft = maxOffset
        val normalizedOffsetLeft = (-offsetLeft / maxOffset).coerceIn(-1f, 1f)
        assertEquals(-1f, normalizedOffsetLeft, 0.001f, "At left edge, normalized offset should be -1")
        
        // Test right edge (offset = -750, showing right side of map)
        val offsetRight = -maxOffset
        val normalizedOffsetRight = (-offsetRight / maxOffset).coerceIn(-1f, 1f)
        assertEquals(1f, normalizedOffsetRight, 0.001f, "At right edge, normalized offset should be 1")
        
        // Test center (offset = 0)
        val offsetCenter = 0f
        val normalizedOffsetCenter = (-offsetCenter / maxOffset).coerceIn(-1f, 1f)
        assertEquals(0f, normalizedOffsetCenter, 0.001f, "At center, normalized offset should be 0")
    }
}
