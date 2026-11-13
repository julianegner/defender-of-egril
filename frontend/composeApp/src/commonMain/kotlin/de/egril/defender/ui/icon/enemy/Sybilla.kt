package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Grand Coven-Mother Sybilla symbol.
 * A crowned witch wearing a two-toned hat (left half green, right half dark red) to
 * represent her mastery of both healing and tower-disruption magic.
 */
fun DrawScope.drawSybillaSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val headCenterY = centerY + size * 0.12f

    // Head: two-toned hat + face + eyes (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Optional outline
        if (outlineColor != null) {
            val hatOutlinePath =
                Path().apply {
                    moveTo(centerX, centerY - size * 0.42f)
                    lineTo(centerX - size * 0.28f, centerY - size * 0.04f)
                    lineTo(centerX + size * 0.28f, centerY - size * 0.04f)
                    close()
                }
            drawPath(hatOutlinePath, outlineColor, style = Stroke(width = 3f))
            drawCircle(
                color = outlineColor,
                radius = size * 0.20f + 2f,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = 2f),
            )
        }

        // Left half of hat: forest green (healer side)
        val hatLeftPath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.42f)
                lineTo(centerX - size * 0.28f, centerY - size * 0.04f)
                lineTo(centerX, centerY - size * 0.04f)
                close()
            }
        drawPath(hatLeftPath, Color(0xFF228B22))

        // Right half of hat: dark red (disruptor side)
        val hatRightPath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.42f)
                lineTo(centerX + size * 0.28f, centerY - size * 0.04f)
                lineTo(centerX, centerY - size * 0.04f)
                close()
            }
        drawPath(hatRightPath, Color(0xFF8B0000))

        // Hat brim (two-toned)
        drawRect(
            color = Color(0xFF228B22),
            topLeft = Offset(centerX - size * 0.32f, centerY - size * 0.04f),
            size = Size(size * 0.32f, size * 0.06f),
        )
        drawRect(
            color = Color(0xFF8B0000),
            topLeft = Offset(centerX, centerY - size * 0.04f),
            size = Size(size * 0.32f, size * 0.06f),
        )

        // Crown (three small points above the hat brim)
        val crownColor = Color(0xFFFFD700) // Gold
        val crownBaseY = centerY - size * 0.04f
        // Left point
        val crownLeft =
            Path().apply {
                moveTo(centerX - size * 0.14f, crownBaseY)
                lineTo(centerX - size * 0.20f, crownBaseY - size * 0.10f)
                lineTo(centerX - size * 0.08f, crownBaseY)
                close()
            }
        drawPath(crownLeft, crownColor)
        // Centre point (tallest)
        val crownCenter =
            Path().apply {
                moveTo(centerX - size * 0.06f, crownBaseY)
                lineTo(centerX, crownBaseY - size * 0.14f)
                lineTo(centerX + size * 0.06f, crownBaseY)
                close()
            }
        drawPath(crownCenter, crownColor)
        // Right point
        val crownRight =
            Path().apply {
                moveTo(centerX + size * 0.08f, crownBaseY)
                lineTo(centerX + size * 0.20f, crownBaseY - size * 0.10f)
                lineTo(centerX + size * 0.14f, crownBaseY)
                close()
            }
        drawPath(crownRight, crownColor)

        // Face: pale skin with high authority bearing
        drawCircle(
            color = Color(0xFFF5DEB3), // Wheat/pale skin
            radius = size * 0.20f,
            center = Offset(centerX, headCenterY),
        )

        // Eyes: one green (left), one red (right) – reflecting dual nature
        drawCircle(
            color = Color(0xFF32CD32),
            radius = size * 0.055f,
            center = Offset(centerX - size * 0.09f, headCenterY + size * 0.01f),
        )
        drawCircle(
            color = Color(0xFFDC143C),
            radius = size * 0.055f,
            center = Offset(centerX + size * 0.09f, headCenterY + size * 0.01f),
        )
    }

    // Staff: taller and more ornate than a regular witch (not scaled)
    drawLine(
        color = Color(0xFF4A2800),
        start = Offset(centerX + size * 0.24f, centerY + size * 0.12f),
        end = Offset(centerX + size * 0.40f, centerY + size * 0.38f),
        strokeWidth = 3f,
    )
    // Dual orb at tip: half green, half red
    drawCircle(
        color = Color(0xFF228B22),
        radius = size * 0.075f,
        center = Offset(centerX + size * 0.38f, centerY + size * 0.34f),
    )
    drawArc(
        color = Color(0xFF8B0000),
        startAngle = -90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(centerX + size * 0.305f, centerY + size * 0.265f),
        size = Size(size * 0.15f, size * 0.15f),
    )
}
