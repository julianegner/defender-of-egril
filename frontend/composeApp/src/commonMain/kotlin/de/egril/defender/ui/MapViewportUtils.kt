package de.egril.defender.ui

import androidx.compose.ui.unit.IntSize

/**
 * Constrains pan offsets to keep content visible within the viewport.
 * This ensures that the user can't pan beyond the edges of the content.
 * 
 * @param newOffsetX The new horizontal offset
 * @param newOffsetY The new vertical offset
 * @param currentScale The current zoom scale
 * @param containerSize The size of the viewport container
 * @param contentSize The size of the actual content
 * @return A pair of constrained (offsetX, offsetY) values
 */
fun constrainMapOffsets(
    newOffsetX: Float,
    newOffsetY: Float,
    currentScale: Float,
    containerSize: IntSize,
    contentSize: IntSize
): Pair<Float, Float> {
    // If content size hasn't been measured yet, don't constrain
    if (contentSize.width == 0 || contentSize.height == 0) {
        return Pair(newOffsetX, newOffsetY)
    }
    
    val contentWidth = contentSize.width * currentScale
    val contentHeight = contentSize.height * currentScale

    val maxOffsetX = if (contentWidth > containerSize.width) {
        (contentWidth - containerSize.width) / 2
    } else {
        (containerSize.width * (currentScale - 1) / 2).coerceAtLeast(0f)
    }

    val maxOffsetY = if (contentHeight > containerSize.height) {
        (contentHeight - containerSize.height) / 2
    } else {
        (containerSize.height * (currentScale - 1) / 2).coerceAtLeast(0f)
    }

    return Pair(
        newOffsetX.coerceIn(-maxOffsetX, maxOffsetX),
        newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
    )
}
