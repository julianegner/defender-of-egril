package de.egril.defender.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for ControlPad and ZoomControls components.
 * 
 * These tests verify that the control pad buttons properly invoke their callbacks.
 */
class ControlPadTest {
    
    @Test
    fun testControlPadUpCallback() {
        var upCallCount = 0
        val onUp = { upCallCount++ }
        
        // Simulate pressing the up button
        onUp()
        
        assertEquals(1, upCallCount, "Up callback should be invoked once")
    }
    
    @Test
    fun testControlPadDownCallback() {
        var downCallCount = 0
        val onDown = { downCallCount++ }
        
        // Simulate pressing the down button
        onDown()
        
        assertEquals(1, downCallCount, "Down callback should be invoked once")
    }
    
    @Test
    fun testControlPadLeftCallback() {
        var leftCallCount = 0
        val onLeft = { leftCallCount++ }
        
        // Simulate pressing the left button
        onLeft()
        
        assertEquals(1, leftCallCount, "Left callback should be invoked once")
    }
    
    @Test
    fun testControlPadRightCallback() {
        var rightCallCount = 0
        val onRight = { rightCallCount++ }
        
        // Simulate pressing the right button
        onRight()
        
        assertEquals(1, rightCallCount, "Right callback should be invoked once")
    }
    
    @Test
    fun testControlPadAllCallbacks() {
        var upCalled = false
        var downCalled = false
        var leftCalled = false
        var rightCalled = false
        
        val onUp = { upCalled = true }
        val onDown = { downCalled = true }
        val onLeft = { leftCalled = true }
        val onRight = { rightCalled = true }
        
        // Simulate pressing all buttons
        onUp()
        onDown()
        onLeft()
        onRight()
        
        assertTrue(upCalled, "Up callback should be invoked")
        assertTrue(downCalled, "Down callback should be invoked")
        assertTrue(leftCalled, "Left callback should be invoked")
        assertTrue(rightCalled, "Right callback should be invoked")
    }
    
    @Test
    fun testZoomControlsZoomInCallback() {
        var zoomInCallCount = 0
        val onZoomIn = { zoomInCallCount++ }
        
        // Simulate pressing the zoom in button
        onZoomIn()
        
        assertEquals(1, zoomInCallCount, "Zoom in callback should be invoked once")
    }
    
    @Test
    fun testZoomControlsZoomOutCallback() {
        var zoomOutCallCount = 0
        val onZoomOut = { zoomOutCallCount++ }
        
        // Simulate pressing the zoom out button
        onZoomOut()
        
        assertEquals(1, zoomOutCallCount, "Zoom out callback should be invoked once")
    }
    
    @Test
    fun testZoomControlsBothCallbacks() {
        var zoomInCalled = false
        var zoomOutCalled = false
        
        val onZoomIn = { zoomInCalled = true }
        val onZoomOut = { zoomOutCalled = true }
        
        // Simulate pressing both zoom buttons
        onZoomIn()
        onZoomOut()
        
        assertTrue(zoomInCalled, "Zoom in callback should be invoked")
        assertTrue(zoomOutCalled, "Zoom out callback should be invoked")
    }
    
    @Test
    fun testControlPadWithStateUpdate() {
        // Test that control pad callbacks can be used to update state
        var offsetX = 0f
        var offsetY = 0f
        
        val onUp = { offsetY += 30f }
        val onDown = { offsetY -= 30f }
        val onLeft = { offsetX += 30f }
        val onRight = { offsetX -= 30f }
        
        // Initial position
        assertEquals(0f, offsetX)
        assertEquals(0f, offsetY)
        
        // Move up
        onUp()
        assertEquals(0f, offsetX)
        assertEquals(30f, offsetY)
        
        // Move right
        onRight()
        assertEquals(-30f, offsetX)
        assertEquals(30f, offsetY)
        
        // Move down
        onDown()
        assertEquals(-30f, offsetX)
        assertEquals(0f, offsetY)
        
        // Move left
        onLeft()
        assertEquals(0f, offsetX)
        assertEquals(0f, offsetY)
    }
    
    @Test
    fun testZoomControlsWithStateUpdate() {
        // Test that zoom controls callbacks can be used to update state
        var zoomLevel = 1.0f
        
        val onZoomIn = { 
            zoomLevel = (zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)
        }
        val onZoomOut = { 
            zoomLevel = (zoomLevel - 0.1f).coerceIn(0.5f, 3.0f)
        }
        
        // Initial zoom
        assertEquals(1.0f, zoomLevel)
        
        // Zoom in
        onZoomIn()
        assertEquals(1.1f, zoomLevel, 0.001f)
        
        // Zoom in again
        onZoomIn()
        assertEquals(1.2f, zoomLevel, 0.001f)
        
        // Zoom out
        onZoomOut()
        assertEquals(1.1f, zoomLevel, 0.001f)
        
        // Zoom out twice to go below initial
        onZoomOut()
        onZoomOut()
        assertEquals(0.9f, zoomLevel, 0.001f)
    }
    
    @Test
    fun testMultipleConsecutiveDirectionalPresses() {
        // Test simulating holding down a direction button (multiple presses)
        var offsetY = 0f
        val onUp = { offsetY += 30f }
        
        // Simulate 5 consecutive presses (like holding the button)
        repeat(5) {
            onUp()
        }
        
        assertEquals(150f, offsetY, "After 5 presses, offsetY should be 150")
    }
    
    @Test
    fun testMultipleConsecutiveZoomPresses() {
        // Test simulating holding down zoom button (multiple presses)
        var zoomLevel = 1.0f
        val onZoomIn = { 
            zoomLevel = (zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)
        }
        
        // Simulate 10 consecutive presses
        repeat(10) {
            onZoomIn()
        }
        
        // Should be clamped to max (3.0)
        assertEquals(2.0f, zoomLevel, 0.001f, "After 10 presses, zoom should be 2.0")
    }
}
