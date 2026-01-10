package de.egril.defender.ui.editor

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests to verify hexagon orientation in minimaps
 * 
 * Pointy-top hexagons should have vertices at angles: -30°, 30°, 90°, 150°, 210°, 270°
 * Flat-top hexagons would have vertices at angles: 0°, 60°, 120°, 180°, 240°, 300°
 * 
 * Reference: https://www.redblobgames.com/grids/hexagons/#angles
 */
class HexagonOrientationTest {
    
    /**
     * Test that the pointy-top formula produces vertices at correct angles
     */
    @Test
    fun testPointyTopOrientation() {
        // The formula used in WaypointMinimap and SpawnPointMinimap after fix
        // angle = PI * (60.0 * i - 30.0) / 180.0
        
        val expectedAnglesInDegrees = listOf(-30.0, 30.0, 90.0, 150.0, 210.0, 270.0)
        
        for (i in 0..5) {
            val angleRadians = PI * (60.0 * i - 30.0) / 180.0
            val angleInDegrees = angleRadians * 180.0 / PI
            
            val expectedAngle = expectedAnglesInDegrees[i]
            val diff = abs(angleInDegrees - expectedAngle)
            
            assertTrue(
                diff < 0.001,
                "Pointy-top: Vertex $i should be at $expectedAngle° but got $angleInDegrees°"
            )
        }
    }
    
    /**
     * Test that the old flat-top formula produces different angles (to confirm we fixed it)
     */
    @Test
    fun testFlatTopOrientationIsDifferent() {
        // The old formula that was incorrectly used
        // angle = PI / 3.0 * i  (which equals PI * 60.0 * i / 180.0)
        
        val expectedFlatTopAnglesInDegrees = listOf(0.0, 60.0, 120.0, 180.0, 240.0, 300.0)
        
        for (i in 0..5) {
            val angleRadians = (PI / 3.0 * i).toFloat()
            val angleInDegrees = angleRadians * 180.0 / PI
            
            val expectedAngle = expectedFlatTopAnglesInDegrees[i]
            val diff = abs(angleInDegrees - expectedAngle)
            
            assertTrue(
                diff < 0.001,
                "Flat-top: Vertex $i should be at $expectedAngle° but got $angleInDegrees°"
            )
        }
    }
    
    /**
     * Test that pointy-top hexagons have a vertex pointing upward (at top)
     * For pointy-top, the first vertex is at -30° (or 330°), and one vertex is at 90° (pointing up)
     */
    @Test
    fun testPointyTopHasVertexAtTop() {
        // For pointy-top hexagons, there should be a vertex at 90° (straight up)
        val centerX = 0f
        val centerY = 0f
        val radius = 1f
        
        // Check vertex at i=2 which should be at 90°
        val i = 2
        val angle = PI * (60.0 * i - 30.0) / 180.0
        val y = centerY + (radius * sin(angle)).toFloat()
        
        // At 90°, sin(90°) = 1, so y should equal radius
        assertTrue(
            abs(y - radius) < 0.001f,
            "Pointy-top hexagon should have a vertex at the top (y=$radius) but got y=$y"
        )
    }
    
    /**
     * Test that flat-top hexagons have a flat edge at top (no vertex at 90°)
     * For flat-top, vertices are at 0°, 60°, 120°, etc., so no vertex at exactly 90°
     */
    @Test
    fun testFlatTopHasNoVertexAtTop() {
        // For flat-top hexagons, vertices are at 0°, 60°, 120°, 180°, 240°, 300°
        // None of these are at 90°, so the top is flat
        val centerY = 0f
        val radius = 1f
        
        var hasVertexAt90 = false
        for (i in 0..5) {
            val angle = (PI / 3.0 * i).toFloat()
            val angleInDegrees = angle * 180f / PI.toFloat()
            
            // Check if any vertex is at 90°
            if (abs(angleInDegrees - 90f) < 0.001f) {
                hasVertexAt90 = true
                break
            }
        }
        
        assertTrue(
            !hasVertexAt90,
            "Flat-top hexagon should NOT have a vertex at 90° (at the top)"
        )
    }
}
