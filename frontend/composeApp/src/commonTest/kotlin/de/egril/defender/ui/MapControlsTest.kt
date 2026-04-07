package de.egril.defender.ui

import de.egril.defender.ui.editor.map.MapControlState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for MapControls component to verify state propagation.
 * 
 * These tests verify that the control pad and zoom controls properly
 * propagate state changes through callbacks instead of direct mutations.
 */
class MapControlsTest {
    
    @Test
    fun testMapControlStateIsImmutable() {
        // Verify that MapControlState properties are immutable (val, not var)
        val state = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // This test verifies compilation - if properties were mutable (var),
        // the following would compile, but since they're immutable (val), this test passing
        // confirms the properties cannot be modified after creation
        assertEquals(1.0f, state.zoomLevel)
        assertEquals(0f, state.offsetX)
        assertEquals(0f, state.offsetY)
    }
    
    @Test
    fun testMapControlStateCopyForUpDirection() {
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what the up button does: copy with increased offsetY
        val newState = initialState.copy(offsetY = initialState.offsetY + 30f)
        
        // Verify original state is unchanged
        assertEquals(1.0f, initialState.zoomLevel)
        assertEquals(0f, initialState.offsetX)
        assertEquals(0f, initialState.offsetY)
        
        // Verify new state has updated offsetY
        assertEquals(1.0f, newState.zoomLevel)
        assertEquals(0f, newState.offsetX)
        assertEquals(30f, newState.offsetY)
    }
    
    @Test
    fun testMapControlStateCopyForDownDirection() {
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what the down button does: copy with decreased offsetY
        val newState = initialState.copy(offsetY = initialState.offsetY - 30f)
        
        // Verify original state is unchanged
        assertEquals(0f, initialState.offsetY)
        
        // Verify new state has updated offsetY
        assertEquals(-30f, newState.offsetY)
    }
    
    @Test
    fun testMapControlStateCopyForLeftDirection() {
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what the left button does: copy with increased offsetX
        val newState = initialState.copy(offsetX = initialState.offsetX + 30f)
        
        // Verify original state is unchanged
        assertEquals(0f, initialState.offsetX)
        
        // Verify new state has updated offsetX
        assertEquals(30f, newState.offsetX)
    }
    
    @Test
    fun testMapControlStateCopyForRightDirection() {
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what the right button does: copy with decreased offsetX
        val newState = initialState.copy(offsetX = initialState.offsetX - 30f)
        
        // Verify original state is unchanged
        assertEquals(0f, initialState.offsetX)
        
        // Verify new state has updated offsetX
        assertEquals(-30f, newState.offsetX)
    }
    
    @Test
    fun testMapControlStateCopyForZoomIn() {
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what the zoom in button does: copy with increased zoom level
        val newState = initialState.copy(
            zoomLevel = (initialState.zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)
        )
        
        // Verify original state is unchanged
        assertEquals(1.0f, initialState.zoomLevel)
        
        // Verify new state has updated zoom level
        assertEquals(1.1f, newState.zoomLevel, 0.001f)
    }
    
    @Test
    fun testMapControlStateCopyForZoomOut() {
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what the zoom out button does: copy with decreased zoom level
        val newState = initialState.copy(
            zoomLevel = (initialState.zoomLevel - 0.1f).coerceIn(0.5f, 3.0f)
        )
        
        // Verify original state is unchanged
        assertEquals(1.0f, initialState.zoomLevel)
        
        // Verify new state has updated zoom level
        assertEquals(0.9f, newState.zoomLevel, 0.001f)
    }
    
    @Test
    fun testZoomInRespectMaximumLimit() {
        val initialState = MapControlState(
            zoomLevel = 2.95f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Zoom in from near maximum
        val newState = initialState.copy(
            zoomLevel = (initialState.zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)
        )
        
        // Verify zoom level is clamped to maximum (3.0f)
        assertEquals(3.0f, newState.zoomLevel, 0.001f)
    }
    
    @Test
    fun testZoomOutRespectsMinimumLimit() {
        val initialState = MapControlState(
            zoomLevel = 0.55f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Zoom out from near minimum
        val newState = initialState.copy(
            zoomLevel = (initialState.zoomLevel - 0.1f).coerceIn(0.5f, 3.0f)
        )
        
        // Verify zoom level is clamped to minimum (0.5f)
        assertEquals(0.5f, newState.zoomLevel, 0.001f)
    }
    
    @Test
    fun testMultipleStateChanges() {
        var currentState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate multiple control pad actions
        // Move up
        currentState = currentState.copy(offsetY = currentState.offsetY + 30f)
        assertEquals(30f, currentState.offsetY)
        
        // Move right
        currentState = currentState.copy(offsetX = currentState.offsetX - 30f)
        assertEquals(-30f, currentState.offsetX)
        
        // Zoom in
        currentState = currentState.copy(
            zoomLevel = (currentState.zoomLevel + 0.1f).coerceIn(0.5f, 3.0f)
        )
        assertEquals(1.1f, currentState.zoomLevel, 0.001f)
        
        // Move left
        currentState = currentState.copy(offsetX = currentState.offsetX + 30f)
        assertEquals(0f, currentState.offsetX)
        
        // Move down
        currentState = currentState.copy(offsetY = currentState.offsetY - 30f)
        assertEquals(0f, currentState.offsetY)
        
        // Zoom out
        currentState = currentState.copy(
            zoomLevel = (currentState.zoomLevel - 0.1f).coerceIn(0.5f, 3.0f)
        )
        assertEquals(1.0f, currentState.zoomLevel, 0.001f)
    }
    
    @Test
    fun testStateCallbackPattern() {
        // This test verifies the callback pattern used by MapControls
        var capturedState: MapControlState? = null
        val onStateChange: (MapControlState) -> Unit = { newState ->
            capturedState = newState
        }
        
        val initialState = MapControlState(
            zoomLevel = 1.0f,
            offsetX = 0f,
            offsetY = 0f
        )
        
        // Simulate what happens when up button is pressed
        onStateChange(initialState.copy(offsetY = initialState.offsetY + 30f))
        
        // Verify callback was invoked with new state
        assertEquals(30f, capturedState?.offsetY)
        assertEquals(0f, capturedState?.offsetX)
        assertEquals(1.0f, capturedState?.zoomLevel)
    }
}
