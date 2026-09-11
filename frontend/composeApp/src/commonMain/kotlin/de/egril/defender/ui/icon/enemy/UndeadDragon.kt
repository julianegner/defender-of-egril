package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

fun DrawScope.drawUndeadDragonSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    drawDragonSymbol(
        centerX = centerX,
        centerY = centerY,
        size = size,
        outlineColor = outlineColor,
        headScale = headScale,
    )
    drawCircle(
        color = Color(0xFF8BFFF0).copy(alpha = 0.25f),
        radius = size * 0.52f,
        center = Offset(centerX, centerY),
    )
    drawCircle(
        color = Color(0xFFB8FFF5).copy(alpha = 0.75f),
        radius = size * 0.06f,
        center = Offset(centerX + size * 0.22f, centerY - size * 0.18f),
    )
}
