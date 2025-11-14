package de.egril.defender.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize

/**
 * Android implementation of mouse wheel zoom support.
 * This is a no-op since Android uses touch gestures for zooming (handled via detectTransformGestures).
 */
actual fun Modifier.mouseWheelZoom(
    containerSize: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    onScaleChange: (Float) -> Unit,
    onOffsetChange: (Float, Float) -> Unit
): Modifier = this
