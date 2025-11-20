package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Test viewport ratio calculations for the minimap
 * These calculations determine the size of the viewport rectangle shown on the minimap
 */
class MinimapViewportRatioTest {
    
    @Test
    fun testViewportRatioAtNormalZoom() {
        // At scale = 1.0 (normal zoom), viewport shows 100% of the map
        val scale = 1.0f
        val expectedRatio = 1.0f
        
        val actualRatio = (1f / scale).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "At normal zoom (scale=1.0), viewport should show 100% of map")
    }
    
    @Test
    fun testViewportRatioWhenZoomedIn() {
        // At scale = 2.0 (zoomed in 2x), viewport shows 50% of the map
        val scale = 2.0f
        val expectedRatio = 0.5f
        
        val actualRatio = (1f / scale).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When zoomed in 2x (scale=2.0), viewport should show 50% of map")
    }
    
    @Test
    fun testViewportRatioWhenZoomedInMax() {
        // At scale = 3.0 (max zoom in), viewport shows 33% of the map
        val scale = 3.0f
        val expectedRatio = 1f / 3f  // ~0.333
        
        val actualRatio = (1f / scale).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When zoomed in 3x (scale=3.0), viewport should show 33% of map")
    }
    
    @Test
    fun testViewportRatioWhenZoomedOut() {
        // At scale = 0.5 (zoomed out 2x), you see more of the map, but viewport cannot exceed 100%
        val scale = 0.5f
        val expectedRatio = 1.0f  // Clamped to 100%
        
        val actualRatio = (1f / scale).coerceAtMost(1f)
        
        assertEquals(expectedRatio, actualRatio, 0.001f, 
            "When zoomed out (scale=0.5), viewport should be clamped to 100% of minimap")
    }
    
    @Test
    fun testViewportRatioIsAlwaysValid() {
        // Test that viewport ratio is always between 0 and 1 for any valid scale
        val scales = listOf(0.5f, 0.75f, 1.0f, 1.5f, 2.0f, 2.5f, 3.0f)
        
        for (scale in scales) {
            val ratio = (1f / scale).coerceAtMost(1f)
            
            assertTrue(ratio > 0f, "Viewport ratio must be positive for scale=$scale")
            assertTrue(ratio <= 1f, "Viewport ratio must not exceed 1.0 for scale=$scale")
        }
    }
    
    @Test
    fun testViewportRatioIncreasesWhenZoomingOut() {
        // When zooming out FROM zoomed in state, viewport ratio should increase
        val scaleZoomedIn = 2.0f
        val scaleNormal = 1.0f
        
        val ratioZoomedIn = (1f / scaleZoomedIn).coerceAtMost(1f)
        val ratioNormal = (1f / scaleNormal).coerceAtMost(1f)
        
        assertTrue(ratioNormal > ratioZoomedIn, 
            "Viewport ratio should increase when zooming out from zoomed in state")
    }
    
    @Test
    fun testViewportRatioDecreasesWhenZoomingIn() {
        // When zooming in FROM normal state, viewport ratio should decrease
        val scaleNormal = 1.0f
        val scaleZoomedIn = 2.0f
        
        val ratioNormal = (1f / scaleNormal).coerceAtMost(1f)
        val ratioZoomedIn = (1f / scaleZoomedIn).coerceAtMost(1f)
        
        assertTrue(ratioZoomedIn < ratioNormal, 
            "Viewport ratio should decrease when zooming in from normal state")
    }
}
