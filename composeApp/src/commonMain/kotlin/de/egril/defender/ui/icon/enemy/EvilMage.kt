package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw evil mage symbol (similar to wizard but more menacing)
 */
fun DrawScope.drawEvilMageSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
    // Robe/body (not scaled)
    if (outlineColor != null) {
        val robeOutlinePath = Path().apply {
            moveTo(centerX, centerY - size * 0.35f)
            lineTo(centerX - size * 0.25f, centerY + size * 0.3f)
            lineTo(centerX + size * 0.25f, centerY + size * 0.3f)
            close()
        }
        drawPath(robeOutlinePath, outlineColor, style = Stroke(width = 3f))
    }
    val robePath = Path().apply {
        moveTo(centerX, centerY - size * 0.35f)
        lineTo(centerX - size * 0.25f, centerY + size * 0.3f)
        lineTo(centerX + size * 0.25f, centerY + size * 0.3f)
        close()
    }
    drawPath(robePath, Color(0xFF2C0052)) // Very dark purple

    // Head: hood + face + eyes (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, centerY)) }) {
        // Hood
        val hoodPath = Path().apply {
            moveTo(centerX, centerY - size * 0.4f)
            lineTo(centerX - size * 0.3f, centerY - size * 0.05f)
            lineTo(centerX + size * 0.3f, centerY - size * 0.05f)
            close()
        }
        drawPath(hoodPath, Color(0xFF1A0030))

        // Face in shadow
        drawCircle(
            color = Color(0xFF4A0080),
            radius = size * 0.15f,
            center = Offset(centerX, centerY)
        )

        // Glowing purple eyes
        drawCircle(color = Color(0xFFB000FF), radius = size * 0.06f, center = Offset(centerX - size * 0.08f, centerY - size * 0.02f))
        drawCircle(color = Color(0xFFB000FF), radius = size * 0.06f, center = Offset(centerX + size * 0.08f, centerY - size * 0.02f))
    }

    // Staff with dark orb (not scaled)
    drawLine(
        color = Color(0xFF1A1A1A),
        start = Offset(centerX + size * 0.3f, centerY - size * 0.1f),
        end = Offset(centerX + size * 0.4f, centerY + size * 0.4f),
        strokeWidth = 3f
    )
    drawCircle(color = Color(0xFF8B00FF), radius = size * 0.1f, center = Offset(centerX + size * 0.4f, centerY - size * 0.15f))
}
