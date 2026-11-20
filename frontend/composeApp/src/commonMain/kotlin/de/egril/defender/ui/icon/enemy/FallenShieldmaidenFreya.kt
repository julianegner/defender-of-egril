package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Fallen Shieldmaiden Freya, an undead death knight with a towering enchanted shield.
 */
fun DrawScope.drawFallenShieldmaidenFreyaSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val armorDark = Color(0xFF2A2F3A)
    val armorLight = Color(0xFF5A6478)
    val bone = Color(0xFFF2EFE6)
    val glow = Color(0xFF6CC8FF)
    val shieldFill = Color(0xFF3C465A)
    val shieldTrim = Color(0xFF7DD7FF)
    val outlineWidth = size * 0.045f
    val headCenterY = centerY - size * 0.16f

    val shield =
        Path().apply {
            moveTo(centerX - size * 0.42f, centerY - size * 0.18f)
            lineTo(centerX - size * 0.12f, centerY - size * 0.26f)
            lineTo(centerX - size * 0.04f, centerY + size * 0.04f)
            cubicTo(
                centerX - size * 0.05f,
                centerY + size * 0.3f,
                centerX - size * 0.2f,
                centerY + size * 0.45f,
                centerX - size * 0.33f,
                centerY + size * 0.5f,
            )
            cubicTo(
                centerX - size * 0.46f,
                centerY + size * 0.45f,
                centerX - size * 0.54f,
                centerY + size * 0.27f,
                centerX - size * 0.5f,
                centerY + size * 0.02f,
            )
            close()
        }
    if (outlineColor != null) {
        drawPath(shield, outlineColor, style = Stroke(width = outlineWidth))
    }
    drawPath(shield, shieldFill)
    drawPath(shield, shieldTrim, style = Stroke(width = outlineWidth * 0.7f))
    drawLine(
        color = shieldTrim,
        start = Offset(centerX - size * 0.37f, centerY - size * 0.08f),
        end = Offset(centerX - size * 0.18f, centerY + size * 0.34f),
        strokeWidth = outlineWidth * 0.55f,
    )

    if (outlineColor != null) {
        drawRect(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.18f, centerY + size * 0.06f),
            size = Size(size * 0.46f, size * 0.3f),
            style = Stroke(width = outlineWidth),
        )
    }
    drawRect(
        color = armorDark,
        topLeft = Offset(centerX - size * 0.18f, centerY + size * 0.06f),
        size = Size(size * 0.46f, size * 0.3f),
    )
    drawRect(
        color = armorLight,
        topLeft = Offset(centerX - size * 0.02f, centerY + size * 0.1f),
        size = Size(size * 0.12f, size * 0.22f),
    )

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        val helm =
            Path().apply {
                moveTo(centerX - size * 0.22f, headCenterY - size * 0.16f)
                lineTo(centerX, headCenterY - size * 0.34f)
                lineTo(centerX + size * 0.22f, headCenterY - size * 0.16f)
                lineTo(centerX + size * 0.18f, headCenterY + size * 0.02f)
                lineTo(centerX - size * 0.18f, headCenterY + size * 0.02f)
                close()
            }
        if (outlineColor != null) {
            drawPath(helm, outlineColor, style = Stroke(width = outlineWidth))
        }
        drawPath(helm, armorLight)

        drawCircle(
            color = bone,
            radius = size * 0.17f,
            center = Offset(centerX, headCenterY),
        )
        drawCircle(color = Color.Black, radius = size * 0.045f, center = Offset(centerX - size * 0.07f, headCenterY - size * 0.02f))
        drawCircle(color = Color.Black, radius = size * 0.045f, center = Offset(centerX + size * 0.07f, headCenterY - size * 0.02f))
        drawCircle(color = glow, radius = size * 0.02f, center = Offset(centerX - size * 0.07f, headCenterY - size * 0.02f))
        drawCircle(color = glow, radius = size * 0.02f, center = Offset(centerX + size * 0.07f, headCenterY - size * 0.02f))
    }

    drawLine(
        color = armorLight,
        start = Offset(centerX + size * 0.18f, centerY + size * 0.08f),
        end = Offset(centerX + size * 0.34f, centerY + size * 0.34f),
        strokeWidth = outlineWidth * 0.8f,
    )
    drawLine(
        color = shieldTrim,
        start = Offset(centerX + size * 0.34f, centerY + size * 0.34f),
        end = Offset(centerX + size * 0.28f, centerY + size * 0.46f),
        strokeWidth = outlineWidth * 0.7f,
    )
}
