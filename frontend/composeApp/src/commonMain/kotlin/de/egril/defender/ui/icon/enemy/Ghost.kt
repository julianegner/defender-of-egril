package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawGhostSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
) {
    val ghostPath =
        Path().apply {
            moveTo(centerX - size * 0.22f, centerY + size * 0.32f)
            quadraticTo(centerX - size * 0.34f, centerY + size * 0.08f, centerX - size * 0.22f, centerY - size * 0.18f)
            quadraticTo(centerX - size * 0.12f, centerY - size * 0.36f, centerX, centerY - size * 0.36f)
            quadraticTo(centerX + size * 0.12f, centerY - size * 0.36f, centerX + size * 0.22f, centerY - size * 0.18f)
            quadraticTo(centerX + size * 0.34f, centerY + size * 0.08f, centerX + size * 0.22f, centerY + size * 0.32f)
            quadraticTo(centerX + size * 0.12f, centerY + size * 0.27f, centerX + size * 0.04f, centerY + size * 0.34f)
            quadraticTo(centerX, centerY + size * 0.28f, centerX - size * 0.05f, centerY + size * 0.35f)
            quadraticTo(centerX - size * 0.12f, centerY + size * 0.28f, centerX - size * 0.22f, centerY + size * 0.32f)
            close()
        }

    if (outlineColor != null) {
        drawPath(
            path = ghostPath,
            color = outlineColor,
            style = Stroke(width = size * 0.045f),
        )
    }

    drawPath(path = ghostPath, color = Color.White)

    drawOval(
        color = Color.Black,
        topLeft = Offset(centerX - size * 0.14f, centerY - size * 0.19f),
        size = Size(size * 0.08f, size * 0.16f),
    )
    drawOval(
        color = Color.Black,
        topLeft = Offset(centerX + size * 0.06f, centerY - size * 0.19f),
        size = Size(size * 0.08f, size * 0.16f),
    )

    val mouthPath =
        Path().apply {
            moveTo(centerX - size * 0.07f, centerY - size * 0.01f)
            lineTo(centerX + size * 0.07f, centerY - size * 0.01f)
            quadraticTo(centerX + size * 0.05f, centerY + size * 0.12f, centerX, centerY + size * 0.16f)
            quadraticTo(centerX - size * 0.05f, centerY + size * 0.12f, centerX - size * 0.07f, centerY - size * 0.01f)
            close()
        }
    drawPath(path = mouthPath, color = Color.Black)
}
