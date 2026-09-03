package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw troll symbol (large stone creature, bigger than ogre).
 * Stone-grey coloring with a massive blocky head and rugged body.
 */
fun DrawScope.drawTrollSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val stoneColor = Color(0xFF808080) // Medium grey – stone
    val darkStoneColor = Color(0xFF5A5A5A) // Darker grey – body/shadow
    val rockHighlight = Color(0xFFA0A0A0) // Light grey – highlights

    val headCenterY = centerY - size * 0.1f

    // Head (large blocky rock shape)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.32f - 2f, centerY - size * 0.38f - 2f),
                size = Size(size * 0.64f + 4f, size * 0.38f + 4f),
                style = Stroke(width = 3f),
            )
        }

        // Main head block
        drawRect(
            color = stoneColor,
            topLeft = Offset(centerX - size * 0.32f, centerY - size * 0.38f),
            size = Size(size * 0.64f, size * 0.38f),
        )

        // Rock crack / texture lines on head
        drawLine(
            color = darkStoneColor,
            start = Offset(centerX - size * 0.1f, centerY - size * 0.38f),
            end = Offset(centerX - size * 0.05f, centerY - size * 0.2f),
            strokeWidth = 2f,
        )
        drawLine(
            color = darkStoneColor,
            start = Offset(centerX + size * 0.12f, centerY - size * 0.38f),
            end = Offset(centerX + size * 0.08f, centerY - size * 0.25f),
            strokeWidth = 2f,
        )

        // Deep-set glowing eyes (orange-red – troll fire within stone)
        drawCircle(color = Color(0xFFFF6600), radius = size * 0.07f, center = Offset(centerX - size * 0.14f, centerY - size * 0.22f))
        drawCircle(color = Color(0xFFFF6600), radius = size * 0.07f, center = Offset(centerX + size * 0.14f, centerY - size * 0.22f))
        drawCircle(color = Color(0xFFFFAA00), radius = size * 0.04f, center = Offset(centerX - size * 0.14f, centerY - size * 0.22f))
        drawCircle(color = Color(0xFFFFAA00), radius = size * 0.04f, center = Offset(centerX + size * 0.14f, centerY - size * 0.22f))

        // Jagged mouth / rock fissure
        val mouthPath =
            Path().apply {
                moveTo(centerX - size * 0.18f, centerY - size * 0.06f)
                lineTo(centerX - size * 0.1f, centerY - size * 0.1f)
                lineTo(centerX - size * 0.04f, centerY - size * 0.06f)
                lineTo(centerX + size * 0.04f, centerY - size * 0.1f)
                lineTo(centerX + size * 0.1f, centerY - size * 0.06f)
                lineTo(centerX + size * 0.18f, centerY - size * 0.1f)
            }
        drawPath(mouthPath, darkStoneColor, style = Stroke(width = 2.5f))

        // Stone nose bump
        drawCircle(color = darkStoneColor, radius = size * 0.05f, center = Offset(centerX, centerY - size * 0.15f))

        // Rock highlight on forehead
        drawRect(
            color = rockHighlight,
            topLeft = Offset(centerX - size * 0.28f, centerY - size * 0.36f),
            size = Size(size * 0.1f, size * 0.06f),
        )
    }

    // Massive blocky body
    drawRect(
        color = darkStoneColor,
        topLeft = Offset(centerX - size * 0.35f, centerY + size * 0.02f),
        size = Size(size * 0.7f, size * 0.32f),
    )

    // Body highlight / texture crack
    drawLine(
        color = rockHighlight,
        start = Offset(centerX - size * 0.2f, centerY + size * 0.04f),
        end = Offset(centerX - size * 0.15f, centerY + size * 0.18f),
        strokeWidth = 2f,
    )
}
