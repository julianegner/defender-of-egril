package de.egril.defender.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for Android mouse wheel zoom (no-op implementation)
 */
class MouseWheelZoomAndroidTest {
    
    @Test
    fun `mouseWheelZoom returns same modifier on Android`() {
        val modifier = Modifier
        val containerSize = IntSize(800, 600)
        var scaleCalled = false
        var offsetCalled = false
        
        val resultModifier = modifier.mouseWheelZoom(
            containerSize = containerSize,
            scale = 1.0f,
            offsetX = 0f,
            offsetY = 0f,
            onScaleChange = { scaleCalled = true },
            onOffsetChange = { _, _ -> offsetCalled = true }
        )
        
        // On Android, mouseWheelZoom is a no-op, so it should return the same modifier
        assertEquals(modifier, resultModifier)
    }
    
    @Test
    fun `mouseWheelZoom does not invoke callbacks on Android`() {
        var scaleChangeCount = 0
        var offsetChangeCount = 0
        
        Modifier.mouseWheelZoom(
            containerSize = IntSize(800, 600),
            scale = 1.0f,
            offsetX = 0f,
            offsetY = 0f,
            onScaleChange = { scaleChangeCount++ },
            onOffsetChange = { _, _ -> offsetChangeCount++ }
        )
        
        // Callbacks should not be invoked (Android uses touch gestures instead)
        assertEquals(0, scaleChangeCount)
        assertEquals(0, offsetChangeCount)
    }
}
