package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw blue demon symbol (fast demon with blue flames)
 */
fun DrawScope.drawBlueDemonSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
    val headCenterY = centerY - size * 0.05f

    // Head with horns and eyes (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            val headOutlinePath = Path().apply {
                moveTo(centerX, centerY - size * 0.3f)
                lineTo(centerX + size * 0.2f, centerY - size * 0.1f)
                lineTo(centerX + size * 0.15f, centerY + size * 0.1f)
                lineTo(centerX, centerY + size * 0.2f)
                lineTo(centerX - size * 0.15f, centerY + size * 0.1f)
                lineTo(centerX - size * 0.2f, centerY - size * 0.1f)
                close()
            }
            drawPath(headOutlinePath, outlineColor, style = Stroke(width = 3f))
        }
        // Demon head (angular)
        val headPath = Path().apply {
            moveTo(centerX, centerY - size * 0.3f)
            lineTo(centerX + size * 0.2f, centerY - size * 0.1f)
            lineTo(centerX + size * 0.15f, centerY + size * 0.1f)
            lineTo(centerX, centerY + size * 0.2f)
            lineTo(centerX - size * 0.15f, centerY + size * 0.1f)
            lineTo(centerX - size * 0.2f, centerY - size * 0.1f)
            close()
        }
        drawPath(headPath, Color(0xFF0080FF)) // Bright blue

        // Horns
        val hornPath1 = Path().apply {
            moveTo(centerX - size * 0.15f, centerY - size * 0.25f)
            lineTo(centerX - size * 0.25f, centerY - size * 0.4f)
            lineTo(centerX - size * 0.1f, centerY - size * 0.3f)
            close()
        }
        val hornPath2 = Path().apply {
            moveTo(centerX + size * 0.15f, centerY - size * 0.25f)
            lineTo(centerX + size * 0.25f, centerY - size * 0.4f)
            lineTo(centerX + size * 0.1f, centerY - size * 0.3f)
            close()
        }
        drawPath(hornPath1, Color(0xFF004080)) // Dark blue
        drawPath(hornPath2, Color(0xFF004080))

        // Glowing eyes
        drawCircle(color = Color.Cyan, radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY - size * 0.1f))
        drawCircle(color = Color.Cyan, radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY - size * 0.1f))
    }

    // Blue flame aura / wings (not scaled)
    val flamePath = Path().apply {
        moveTo(centerX - size * 0.15f, centerY + size * 0.1f)
        cubicTo(
            centerX - size * 0.3f, centerY,
            centerX - size * 0.35f, centerY + size * 0.2f,
            centerX - size * 0.2f, centerY + size * 0.3f
        )
    }
    drawPath(flamePath, Color(0xFF40A0FF), style = Stroke(width = 2f))
    val flamePath2 = Path().apply {
        moveTo(centerX + size * 0.15f, centerY + size * 0.1f)
        cubicTo(
            centerX + size * 0.3f, centerY,
            centerX + size * 0.35f, centerY + size * 0.2f,
            centerX + size * 0.2f, centerY + size * 0.3f
        )
    }
    drawPath(flamePath2, Color(0xFF40A0FF), style = Stroke(width = 2f))
}
