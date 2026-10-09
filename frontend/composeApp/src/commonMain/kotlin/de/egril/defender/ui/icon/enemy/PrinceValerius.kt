package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawPrinceValeriusSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val robe = Color(0xFF28304D)
    val frost = Color(0xFF99E6FF)
    val bone = Color(0xFFF4F6F8)
    val crown = Color(0xFFDCC16A)
    val headCenterY = centerY - size * 0.16f
    val outlineWidth = size * 0.045f

    val cloak =
        Path().apply {
            moveTo(centerX, centerY - size * 0.02f)
            lineTo(centerX - size * 0.28f, centerY + size * 0.36f)
            lineTo(centerX + size * 0.28f, centerY + size * 0.36f)
            close()
        }
    if (outlineColor != null) {
        drawPath(cloak, outlineColor, style = Stroke(width = outlineWidth))
    }
    drawPath(cloak, robe)
    drawLine(
        color = frost,
        start = Offset(centerX, centerY),
        end = Offset(centerX, centerY + size * 0.34f),
        strokeWidth = outlineWidth * 0.7f,
    )

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        drawCircle(
            color = bone,
            radius = size * 0.17f,
            center = Offset(centerX, headCenterY),
        )
        drawCircle(color = Color.Black, radius = size * 0.04f, center = Offset(centerX - size * 0.06f, headCenterY - size * 0.02f))
        drawCircle(color = Color.Black, radius = size * 0.04f, center = Offset(centerX + size * 0.06f, headCenterY - size * 0.02f))
        drawCircle(color = frost, radius = size * 0.018f, center = Offset(centerX - size * 0.06f, headCenterY - size * 0.02f))
        drawCircle(color = frost, radius = size * 0.018f, center = Offset(centerX + size * 0.06f, headCenterY - size * 0.02f))
    }

    drawRect(
        color = crown,
        topLeft = Offset(centerX - size * 0.14f, headCenterY - size * 0.22f),
        size = Size(size * 0.28f, size * 0.05f),
    )
    repeat(3) { index ->
        val spikeX = centerX + (index - 1) * size * 0.09f
        val spike =
            Path().apply {
                moveTo(spikeX, headCenterY - size * 0.34f)
                lineTo(spikeX - size * 0.04f, headCenterY - size * 0.18f)
                lineTo(spikeX + size * 0.04f, headCenterY - size * 0.18f)
                close()
            }
        drawPath(spike, crown)
    }
}
