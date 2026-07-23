package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

fun DrawScope.drawRoboticGoblinSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    headScale: Float = 1.0f,
) {
    val headCenterY = centerY - size * 0.1f
    val metal = Color(0xFF8D9095)
    val rust = Color(0xFF9B4E2F)

    if (headScale == 1.0f) {
        drawRect(
            color = Color(0xFF4E545D),
            topLeft = Offset(centerX - size * 0.16f, centerY + size * 0.14f),
            size = Size(size * 0.32f, size * 0.25f),
        )
        drawRect(
            color = rust,
            topLeft = Offset(centerX - size * 0.08f, centerY + size * 0.22f),
            size = Size(size * 0.16f, size * 0.08f),
        )
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        drawRect(
            color = metal,
            topLeft = Offset(centerX - size * 0.28f, centerY - size * 0.38f),
            size = Size(size * 0.56f, size * 0.44f),
        )
        drawRect(
            color = Color(0xFF666B72),
            topLeft = Offset(centerX - size * 0.2f, centerY - size * 0.26f),
            size = Size(size * 0.4f, size * 0.16f),
        )
        drawCircle(color = Color(0xFFFFC300), radius = size * 0.06f, center = Offset(centerX - size * 0.11f, centerY - size * 0.17f))
        drawCircle(color = Color(0xFFFFC300), radius = size * 0.06f, center = Offset(centerX + size * 0.11f, centerY - size * 0.17f))
        drawRect(
            color = rust,
            topLeft = Offset(centerX - size * 0.06f, centerY - size * 0.04f),
            size = Size(size * 0.12f, size * 0.07f),
        )
    }
}

