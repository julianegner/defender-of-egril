package com.defenderofegril.ui.icon.defender

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draw spike symbol (upward pointing spikes)
 */
fun DrawScope.drawSpikeSymbol(centerX: Float, centerY: Float, size: Float) {
    val spikeCount = 3
    val spikeWidth = size / 4
    val spikeHeight = size * 0.8f
    
    for (i in 0 until spikeCount) {
        val x = centerX - size / 2 + (size / spikeCount) * i + spikeWidth / 2
        val path = Path().apply {
            moveTo(x - spikeWidth / 2, centerY + spikeHeight / 3)
            lineTo(x, centerY - spikeHeight / 3)
            lineTo(x + spikeWidth / 2, centerY + spikeHeight / 3)
            close()
        }
        drawPath(path, Color.Yellow)
        drawPath(path, Color.White, style = Stroke(width = 1.5f))
    }
}
