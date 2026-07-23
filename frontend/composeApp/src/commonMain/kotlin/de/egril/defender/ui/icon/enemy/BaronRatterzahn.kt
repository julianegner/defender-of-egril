package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawBaronRatterzahnSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    headScale: Float = 1.0f,
) {
    val steel = Color(0xFF73777F)
    val darkSteel = Color(0xFF4B4F56)
    val rust = Color(0xFF8E4529)
    val goblinSkin = Color(0xFF8DC680)

    // Mechanical spider body
    drawCircle(color = darkSteel, radius = size * 0.2f, center = Offset(centerX, centerY + size * 0.08f))
    drawCircle(color = steel, radius = size * 0.14f, center = Offset(centerX, centerY + size * 0.02f))
    drawCircle(color = rust, radius = size * 0.05f, center = Offset(centerX + size * 0.09f, centerY + size * 0.03f))

    // Spider legs
    val legOffsets = listOf(-0.26f, -0.16f, 0.16f, 0.26f)
    legOffsets.forEach { x ->
        drawRect(
            color = darkSteel,
            topLeft = Offset(centerX + size * x, centerY + size * 0.14f),
            size = Size(size * 0.05f, size * 0.26f),
        )
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, centerY - size * 0.18f)) }) {
        // Goblin rider
        drawCircle(
            color = goblinSkin,
            radius = size * 0.16f,
            center = Offset(centerX, centerY - size * 0.2f),
        )
        val earLeft =
            Path().apply {
                moveTo(centerX - size * 0.13f, centerY - size * 0.2f)
                lineTo(centerX - size * 0.27f, centerY - size * 0.3f)
                lineTo(centerX - size * 0.14f, centerY - size * 0.3f)
                close()
            }
        val earRight =
            Path().apply {
                moveTo(centerX + size * 0.13f, centerY - size * 0.2f)
                lineTo(centerX + size * 0.27f, centerY - size * 0.3f)
                lineTo(centerX + size * 0.14f, centerY - size * 0.3f)
                close()
            }
        drawPath(earLeft, goblinSkin)
        drawPath(earRight, goblinSkin)
        drawCircle(color = Color.Red, radius = size * 0.035f, center = Offset(centerX - size * 0.06f, centerY - size * 0.23f))
        drawCircle(color = Color.Red, radius = size * 0.035f, center = Offset(centerX + size * 0.06f, centerY - size * 0.23f))
    }
}

