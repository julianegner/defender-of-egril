package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw red witch symbol (witch in red, focused on tower disruption)
 */
fun DrawScope.drawRedWitchSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
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
        // Red witch hat
        val hatPath = Path().apply {
            moveTo(centerX, centerY - size * 0.35f)
            lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
            lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
            close()
        }
        drawPath(hatPath, Color(0xFF8B0000)) // Dark red

        // Hat brim
        drawRect(
            color = Color(0xFF8B0000),
            topLeft = Offset(centerX - size * 0.3f, centerY - size * 0.05f),
            size = Size(size * 0.6f, size * 0.06f)
        )

        // Face
        drawCircle(
            color = Color(0xFFFFE4B5), // Light skin
            radius = size * 0.18f,
            center = Offset(centerX, headCenterY)
        )

        // Eyes
        drawCircle(color = Color(0xFFDC143C), radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY + size * 0.08f))
        drawCircle(color = Color(0xFFDC143C), radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY + size * 0.08f))
    }

    // Wand (not scaled)
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX - size * 0.25f, centerY + size * 0.15f),
        end = Offset(centerX - size * 0.4f, centerY + size * 0.35f),
        strokeWidth = 2f
    )
    // Red star on wand
    drawCircle(color = Color.Red, radius = size * 0.06f, center = Offset(centerX - size * 0.4f, centerY + size * 0.35f))
}
