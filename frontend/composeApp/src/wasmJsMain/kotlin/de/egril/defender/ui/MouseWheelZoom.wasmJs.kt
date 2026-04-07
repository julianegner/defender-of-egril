package de.egril.defender.ui

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.IntSize

/**
 * WasmJs implementation of mouse wheel zoom support.
 * Enables zooming in/out using the mouse wheel in the browser.
 */
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.mouseWheelZoom(
    containerSize: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit
): Modifier = this.onPointerEvent(PointerEventType.Scroll) { event ->
    val delta = event.changes.first().scrollDelta
    // Zoom in/out based on scroll direction
    if (delta.y != 0f) {
        val zoomDelta = if (delta.y < 0) 1.1f else 0.9f
        val newScale = (scale * zoomDelta).coerceIn(0.5f, 3f)
        onScaleChange(newScale)

        // Constrain pan after zoom
        val maxOffsetX = (containerSize.width * (newScale - 1) / 2).coerceAtLeast(0f)
        val maxOffsetY = (containerSize.height * (newScale - 1) / 2).coerceAtLeast(0f)
        val newOffsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
        val newOffsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
        onOffsetChange(newOffsetX, newOffsetY)
    }
}
