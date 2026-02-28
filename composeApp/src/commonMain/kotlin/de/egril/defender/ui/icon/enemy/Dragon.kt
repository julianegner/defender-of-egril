package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw dragon symbol (large flying beast)
 */
fun DrawScope.drawDragonSymbol(centerX: Float, centerY: Float, size: Float, headScale: Float = 1.0f) {
    val headCenterX = centerX + size * 0.25f
    val headCenterY = centerY - size * 0.15f

    // Dragon body (not scaled)
    drawOval(
        color = Color(0xFF8B0000),  // Dark red
        topLeft = Offset(centerX - size * 0.2f, centerY - size * 0.1f),
        size = Size(size * 0.4f, size * 0.2f)
    )

    // Wings (not scaled)
    val leftWing = Path().apply {
        moveTo(centerX - size * 0.1f, centerY)
        lineTo(centerX - size * 0.4f, centerY - size * 0.3f)
        lineTo(centerX - size * 0.2f, centerY - size * 0.1f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(centerX + size * 0.1f, centerY)
        lineTo(centerX + size * 0.4f, centerY - size * 0.3f)
        lineTo(centerX + size * 0.2f, centerY - size * 0.1f)
        close()
    }
    drawPath(leftWing, Color(0xFF654321))  // Dark brown wings
    drawPath(rightWing, Color(0xFF654321))

    // Tail (not scaled)
    drawLine(
        color = Color(0xFF8B0000),
        start = Offset(centerX - size * 0.2f, centerY),
        end = Offset(centerX - size * 0.35f, centerY + size * 0.2f),
        strokeWidth = 4f
    )

    // Dragon head with eye (scaled)
    withTransform({ scale(headScale, headScale, Offset(headCenterX, headCenterY)) }) {
        drawCircle(
            color = Color(0xFF8B0000),
            radius = size * 0.15f,
            center = Offset(headCenterX, headCenterY)
        )
        drawCircle(color = Color.Yellow, radius = size * 0.04f, center = Offset(centerX + size * 0.25f, centerY - size * 0.18f))
    }

    // Fire breath (not scaled)
    for (i in 0..2) {
        drawCircle(
            color = Color(0xFFFF4500).copy(alpha = 0.6f),  // Orange-red
            radius = size * 0.08f,
            center = Offset(centerX + size * 0.4f + i * size * 0.1f, centerY - size * 0.15f + i * size * 0.05f)
        )
    }
}
