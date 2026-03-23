package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw skeleton symbol (skull and bones)
 */
fun DrawScope.drawSkeletonSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
    val headCenterY = centerY - size * 0.1f

    // Skull with facial features (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Skull
        drawCircle(
            color = Color.White,
            radius = size * 0.25f,
            center = Offset(centerX, headCenterY)
        )

        // Eye sockets (black)
        drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX - size * 0.12f, centerY - size * 0.15f))
        drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX + size * 0.12f, centerY - size * 0.15f))

        // Nose hole (triangle)
        val nosePath = Path().apply {
            moveTo(centerX, centerY - size * 0.05f)
            lineTo(centerX - size * 0.05f, centerY + size * 0.05f)
            lineTo(centerX + size * 0.05f, centerY + size * 0.05f)
            close()
        }
        drawPath(nosePath, Color.Black)
    }

    // Crossbones (not scaled)
    if (outlineColor != null) {
        // Outline drawn first (thicker) so white bones are visible against any background
        drawLine(
            color = outlineColor,
            start = Offset(centerX - size * 0.3f, centerY + size * 0.25f),
            end = Offset(centerX + size * 0.3f, centerY + size * 0.35f),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = outlineColor,
            start = Offset(centerX - size * 0.3f, centerY + size * 0.35f),
            end = Offset(centerX + size * 0.3f, centerY + size * 0.25f),
            strokeWidth = 7f,
            cap = StrokeCap.Round
        )
    }
    drawLine(
        color = Color.White,
        start = Offset(centerX - size * 0.3f, centerY + size * 0.25f),
        end = Offset(centerX + size * 0.3f, centerY + size * 0.35f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(centerX - size * 0.3f, centerY + size * 0.35f),
        end = Offset(centerX + size * 0.3f, centerY + size * 0.25f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
}
