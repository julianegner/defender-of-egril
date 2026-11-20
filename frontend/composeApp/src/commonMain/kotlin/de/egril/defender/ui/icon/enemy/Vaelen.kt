package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawVaelenSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
) {
    val gold = Color(0xFFD4AF37)
    val crimson = Color(0xFFB5162D)
    val armor =
        Path().apply {
            moveTo(centerX, centerY - size * 0.43f)
            lineTo(centerX - size * 0.32f, centerY - size * 0.14f)
            lineTo(centerX - size * 0.38f, centerY + size * 0.28f)
            lineTo(centerX, centerY + size * 0.46f)
            lineTo(centerX + size * 0.38f, centerY + size * 0.28f)
            lineTo(centerX + size * 0.32f, centerY - size * 0.14f)
            close()
        }
    drawPath(armor, Color(0xFF11111A))
    drawPath(armor, gold, style = Stroke(width = size * 0.045f))

    val cape =
        Path().apply {
            moveTo(centerX - size * 0.25f, centerY - size * 0.18f)
            lineTo(centerX - size * 0.43f, centerY + size * 0.36f)
            lineTo(centerX - size * 0.13f, centerY + size * 0.24f)
            close()
            moveTo(centerX + size * 0.25f, centerY - size * 0.18f)
            lineTo(centerX + size * 0.43f, centerY + size * 0.36f)
            lineTo(centerX + size * 0.13f, centerY + size * 0.24f)
            close()
        }
    drawPath(cape, crimson)

    drawCircle(Color(0xFF24202B), size * 0.19f, Offset(centerX, centerY - size * 0.17f))
    drawCircle(gold, size * 0.19f, Offset(centerX, centerY - size * 0.17f), style = Stroke(size * 0.035f))
    drawLine(crimson, Offset(centerX - size * 0.09f, centerY - size * 0.15f), Offset(centerX + size * 0.09f, centerY - size * 0.15f), size * 0.04f)
    drawLine(gold, Offset(centerX, centerY - size * 0.03f), Offset(centerX, centerY + size * 0.24f), size * 0.035f)
    drawCircle(crimson, size * 0.075f, Offset(centerX - size * 0.37f, centerY - size * 0.28f))
    drawCircle(gold, size * 0.075f, Offset(centerX - size * 0.37f, centerY - size * 0.28f), style = Stroke(size * 0.025f))
    drawCircle(crimson, size * 0.075f, Offset(centerX + size * 0.37f, centerY - size * 0.28f))
    drawCircle(gold, size * 0.075f, Offset(centerX + size * 0.37f, centerY - size * 0.28f), style = Stroke(size * 0.025f))
    drawCircle(crimson, size * 0.075f, Offset(centerX, centerY + size * 0.43f))
    drawCircle(gold, size * 0.075f, Offset(centerX, centerY + size * 0.43f), style = Stroke(size * 0.025f))
}
