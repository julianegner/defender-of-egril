package de.egril.defender.ui.icon

import androidx.compose.ui.graphics.Path

internal fun wunjoRunePath(
    centerX: Float,
    centerY: Float,
    radius: Float,
): Path =
    Path().apply {
        moveTo(centerX, centerY - radius)
        lineTo(centerX, centerY + radius)
        moveTo(centerX, centerY - radius)
        lineTo(centerX + radius * 0.7f, centerY - radius * 0.4f)
        lineTo(centerX, centerY - radius * 0.2f)
    }
