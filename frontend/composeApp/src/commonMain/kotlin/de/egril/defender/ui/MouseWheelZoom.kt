package de.egril.defender.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Platform-specific modifier for mouse wheel zoom support.
 * On desktop, this enables mouse wheel scrolling to zoom in/out.
 * On mobile platforms, this is a no-op since touch gestures handle zooming.
 */
expect fun Modifier.mouseWheelZoom(
    containerSize: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit
): Modifier
