package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawGoblinRunnerSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    drawGoblinSymbol(centerX, centerY, size, outlineColor, headScale)

    val outlineWidth = 2f
    val plumePath =
        Path().apply {
            moveTo(centerX + size * 0.05f, centerY - size * 0.28f)
            quadraticTo(centerX + size * 0.38f, centerY - size * 0.62f, centerX + size * 0.16f, centerY - size * 0.08f)
            quadraticTo(centerX + size * 0.46f, centerY - size * 0.18f, centerX + size * 0.22f, centerY + size * 0.02f)
        }

    withTransform({ scale(headScale, headScale, Offset(centerX, centerY - size * 0.1f)) }) {
        if (outlineColor != null) {
            drawPath(plumePath, outlineColor, style = Stroke(width = outlineWidth * 1.4f))
            drawCircle(
                color = outlineColor,
                radius = size * 0.065f + outlineWidth / 2,
                center = Offset(centerX - size * 0.13f, centerY - size * 0.06f),
                style = Stroke(width = outlineWidth),
            )
            drawCircle(
                color = outlineColor,
                radius = size * 0.065f + outlineWidth / 2,
                center = Offset(centerX + size * 0.15f, centerY - size * 0.02f),
                style = Stroke(width = outlineWidth),
            )
        }

        drawPath(plumePath, Color(0xFFFFD54F), style = Stroke(width = outlineWidth * 1.1f))
        drawCircle(
            color = Color(0xFFE53935),
            radius = size * 0.065f,
            center = Offset(centerX - size * 0.13f, centerY - size * 0.06f),
        )
        drawCircle(
            color = Color(0xFFFFF176),
            radius = size * 0.065f,
            center = Offset(centerX + size * 0.15f, centerY - size * 0.02f),
        )
    }
}
