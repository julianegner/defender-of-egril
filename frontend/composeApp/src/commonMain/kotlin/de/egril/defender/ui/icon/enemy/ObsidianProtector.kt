package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawObsidianProtectorSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
) {
    val obsidian = Color(0xFF101018)
    val highlight = Color(0xFF555466)
    val outline = Color(0xFFB9A8D8)
    val monolith =
        Path().apply {
            moveTo(centerX - size * 0.31f, centerY - size * 0.39f)
            lineTo(centerX + size * 0.18f, centerY - size * 0.48f)
            lineTo(centerX + size * 0.38f, centerY - size * 0.21f)
            lineTo(centerX + size * 0.29f, centerY + size * 0.34f)
            lineTo(centerX - size * 0.12f, centerY + size * 0.45f)
            lineTo(centerX - size * 0.39f, centerY + size * 0.12f)
            close()
        }
    drawPath(monolith, obsidian)
    drawPath(monolith, outline, style = Stroke(width = size * 0.035f))
    drawLine(
        color = highlight,
        start = Offset(centerX - size * 0.27f, centerY - size * 0.31f),
        end = Offset(centerX - size * 0.08f, centerY + size * 0.34f),
        strokeWidth = size * 0.035f,
    )
    drawLine(
        color = highlight,
        start = Offset(centerX + size * 0.18f, centerY - size * 0.4f),
        end = Offset(centerX + size * 0.03f, centerY + size * 0.37f),
        strokeWidth = size * 0.025f,
    )
    drawCircle(Color(0xFFB54DFF), size * 0.045f, Offset(centerX, centerY - size * 0.05f))
}
