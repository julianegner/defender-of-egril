package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawRoboticGoblinSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    headScale: Float = 1.0f,
) {
    val headCenterY = centerY - size * 0.1f
    val metal = Color(0xFF8B929A)
    val rustyMetal = Color(0xFF9A5A3A)
    val eyeColor = Color(0xFFFFA000)

    if (headScale == 1.0f) {
        drawRect(
            color = rustyMetal,
            topLeft = Offset(centerX - size * 0.15f, centerY + size * 0.15f),
            size = Size(size * 0.3f, size * 0.25f),
        )
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        drawCircle(
            color = metal,
            radius = size * 0.3f,
            center = Offset(centerX, headCenterY),
        )

        val earPath1 =
            Path().apply {
                moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
                lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
                lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
                close()
            }
        val earPath2 =
            Path().apply {
                moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
                lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
                lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
                close()
            }
        drawPath(earPath1, metal)
        drawPath(earPath2, metal)

        drawCircle(color = eyeColor, radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY - size * 0.15f))
        drawCircle(color = eyeColor, radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY - size * 0.15f))

        drawRect(
            color = rustyMetal,
            topLeft = Offset(centerX - size * 0.06f, centerY - size * 0.04f),
            size = Size(size * 0.12f, size * 0.07f),
        )
    }
}
