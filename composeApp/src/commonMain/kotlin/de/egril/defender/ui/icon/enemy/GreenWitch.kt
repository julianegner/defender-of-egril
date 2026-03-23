package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw green witch symbol (healer witch)
 */
fun DrawScope.drawGreenWitchSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
    val headCenterY = centerY + size * 0.1f

    // Head: hat + face + eyes (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            val hatOutlinePath = Path().apply {
                moveTo(centerX, centerY - size * 0.35f)
                lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
                lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
                close()
            }
            drawPath(hatOutlinePath, outlineColor, style = Stroke(width = 3f))
            drawCircle(
                color = outlineColor,
                radius = size * 0.18f + 2f,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = 2f)
            )
        }
        // Green witch hat
        val hatPath = Path().apply {
            moveTo(centerX, centerY - size * 0.35f)
            lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
            lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
            close()
        }
        drawPath(hatPath, Color(0xFF228B22)) // Forest green

        // Hat brim
        drawRect(
            color = Color(0xFF228B22),
            topLeft = Offset(centerX - size * 0.3f, centerY - size * 0.05f),
            size = Size(size * 0.6f, size * 0.06f)
        )

        // Face
        drawCircle(
            color = Color(0xFFE0FFE0), // Very light green
            radius = size * 0.18f,
            center = Offset(centerX, headCenterY)
        )

        // Eyes
        drawCircle(color = Color(0xFF32CD32), radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY + size * 0.08f))
        drawCircle(color = Color(0xFF32CD32), radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY + size * 0.08f))
    }

    // Healing staff (not scaled)
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX + size * 0.25f, centerY + size * 0.15f),
        end = Offset(centerX + size * 0.4f, centerY + size * 0.35f),
        strokeWidth = 2f
    )
    // Green healing orb
    drawCircle(color = Color(0xFF00FF00), radius = size * 0.07f, center = Offset(centerX + size * 0.4f, centerY + size * 0.35f))
}
