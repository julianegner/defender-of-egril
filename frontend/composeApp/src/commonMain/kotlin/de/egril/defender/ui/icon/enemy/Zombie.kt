package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawZombieSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val skin = Color(0xFF8AA06C)
    val tunic = Color(0xFF5F4E6B)
    val glow = Color(0xFF9FFFD4)
    val headCenterY = centerY - size * 0.14f

    if (outlineColor != null) {
        drawRect(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.2f, centerY + size * 0.02f),
            size = Size(size * 0.4f, size * 0.3f),
            style = Stroke(width = size * 0.05f),
        )
    }
    drawRect(
        color = tunic,
        topLeft = Offset(centerX - size * 0.2f, centerY + size * 0.02f),
        size = Size(size * 0.4f, size * 0.3f),
    )

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.2f + 2f,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = size * 0.04f),
            )
        }
        drawCircle(
            color = skin,
            radius = size * 0.2f,
            center = Offset(centerX, headCenterY),
        )
        drawCircle(color = Color.Black, radius = size * 0.035f, center = Offset(centerX - size * 0.07f, headCenterY - size * 0.03f))
        drawCircle(color = glow, radius = size * 0.02f, center = Offset(centerX + size * 0.06f, headCenterY - size * 0.02f))
        drawLine(
            color = Color.Black,
            start = Offset(centerX - size * 0.05f, headCenterY + size * 0.06f),
            end = Offset(centerX + size * 0.08f, headCenterY + size * 0.08f),
            strokeWidth = size * 0.03f,
        )
    }

    val arm =
        Path().apply {
            moveTo(centerX + size * 0.05f, centerY + size * 0.04f)
            lineTo(centerX + size * 0.32f, centerY - size * 0.06f)
            lineTo(centerX + size * 0.28f, centerY + size * 0.04f)
            close()
        }
    drawPath(arm, skin)
}
