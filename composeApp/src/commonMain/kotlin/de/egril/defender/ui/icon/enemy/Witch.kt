package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw witch symbol (pointed hat with broom)
 */
fun DrawScope.drawWitchSymbol(centerX: Float, centerY: Float, size: Float, headScale: Float = 1.0f) {
    val headCenterY = centerY + size * 0.1f

    // Head: hat + face + eyes (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Witch hat
        val hatPath = Path().apply {
            moveTo(centerX, centerY - size * 0.35f)
            lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
            lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
            close()
        }
        drawPath(hatPath, Color.Black)

        // Hat brim
        drawRect(
            color = Color.Black,
            topLeft = Offset(centerX - size * 0.3f, centerY - size * 0.05f),
            size = Size(size * 0.6f, size * 0.06f)
        )

        // Face
        drawCircle(
            color = Color(0xFF90EE90), // Greenish
            radius = size * 0.18f,
            center = Offset(centerX, headCenterY)
        )

        // Eyes
        drawCircle(color = Color(0xFFFF4500), radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY + size * 0.08f))
        drawCircle(color = Color(0xFFFF4500), radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY + size * 0.08f))
    }

    // Broom (not scaled)
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX - size * 0.3f, centerY + size * 0.05f),
        end = Offset(centerX - size * 0.42f, centerY + size * 0.35f),
        strokeWidth = 2f
    )
    // Broom bristles
    for (i in 0..3) {
        drawLine(
            color = Color(0xFFDAA520),
            start = Offset(centerX - size * 0.42f, centerY + size * 0.3f),
            end = Offset(centerX - size * 0.48f + i * size * 0.03f, centerY + size * 0.42f),
            strokeWidth = 1.5f
        )
    }
}
