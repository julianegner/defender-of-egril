package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw goblin symbol (small creature with pointy ears)
 */
fun DrawScope.drawGoblinSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
    val outlineWidth = 2f
    val pathOutlineWidth = 3f  // Thicker for paths to match visual appearance
    val headCenterY = centerY - size * 0.1f

    // Body outline (not scaled, hidden in bighead mode)
    if (outlineColor != null && headScale == 1.0f) {
        val bodyPath = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(
                left = centerX - size * 0.15f,
                top = centerY + size * 0.15f,
                right = centerX + size * 0.15f,
                bottom = centerY + size * 0.4f
            ))
        }
        drawPath(bodyPath, outlineColor, style = Stroke(width = pathOutlineWidth))
    }

    // Head elements with scaling
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            // Head outline (circle)
            drawCircle(
                color = outlineColor,
                radius = size * 0.3f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth)
            )

            // Pointy ears outline
            val earPath1 = Path().apply {
                moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
                lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
                lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
                close()
            }
            val earPath2 = Path().apply {
                moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
                lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
                lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
                close()
            }
            drawPath(earPath1, outlineColor, style = Stroke(width = pathOutlineWidth))
            drawPath(earPath2, outlineColor, style = Stroke(width = pathOutlineWidth))
        }

        // Head (circle)
        drawCircle(
            color = Color(0xFF90EE90), // Light green
            radius = size * 0.3f,
            center = Offset(centerX, headCenterY)
        )

        // Pointy ears
        val earPath1 = Path().apply {
            moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
            lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
            lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
            close()
        }
        val earPath2 = Path().apply {
            moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
            lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
            lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
            close()
        }
        drawPath(earPath1, Color(0xFF90EE90))
        drawPath(earPath2, Color(0xFF90EE90))

        // Eyes
        drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY - size * 0.15f))
        drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY - size * 0.15f))
    }

    // Body (small, not scaled) - hidden in bighead mode
    if (headScale == 1.0f) {
        drawRect(
            color = Color(0xFF8B4513), // Brown
            topLeft = Offset(centerX - size * 0.15f, centerY + size * 0.15f),
            size = Size(size * 0.3f, size * 0.25f)
        )
    }
}
