package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Razzik the Rocketeer, a goblin engineer with a shoulder-mounted rocket launcher.
 * The launcher's exhaust glow and tip flame mark him as the rocket threat on the field.
 */
fun DrawScope.drawRazzikTheRocketeerSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 2.5f
    val headCenterY = centerY - size * 0.05f

    val armorColor = Color(0xFF3A3A44)
    val armorHighlight = Color(0xFF5A5A66)
    val faceColor = Color(0xFF7FB069)
    val eyeColor = Color(0xFFFFD000)
    val launcherColor = Color(0xFF6B2E1A)
    val flameColor = Color(0xFFFF6A00)
    val glowColor = Color(0xFFFF6A00).copy(alpha = 0.25f)

    drawCircle(color = glowColor, radius = size * 0.48f, center = Offset(centerX, centerY))

    if (headScale == 1.0f) {
        // Armored torso
        val torso =
            Path().apply {
                moveTo(centerX - size * 0.26f, centerY + size * 0.40f)
                lineTo(centerX - size * 0.20f, centerY + size * 0.04f)
                lineTo(centerX + size * 0.20f, centerY + size * 0.04f)
                lineTo(centerX + size * 0.26f, centerY + size * 0.40f)
                close()
            }
        if (outlineColor != null) drawPath(torso, outlineColor, style = Stroke(width = pathOutlineWidth))
        drawPath(torso, armorColor)
        drawLine(
            color = armorHighlight,
            start = Offset(centerX, centerY + size * 0.06f),
            end = Offset(centerX, centerY + size * 0.40f),
            strokeWidth = 3f,
        )

        // Shoulder rocket launcher with a flame at its tip
        val launcherBody = Size(size * 0.46f, size * 0.12f)
        val launcherTopLeft = Offset(centerX - size * 0.12f, centerY - size * 0.16f)
        drawRect(color = launcherColor, topLeft = launcherTopLeft, size = launcherBody)
        if (outlineColor != null) {
            drawRect(
                color = outlineColor,
                topLeft = launcherTopLeft,
                size = launcherBody,
                style = Stroke(width = pathOutlineWidth),
            )
        }
        drawCircle(color = flameColor, radius = size * 0.07f, center = Offset(centerX + size * 0.34f, centerY - size * 0.10f))
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.19f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }
        drawCircle(color = faceColor, radius = size * 0.19f, center = Offset(centerX, headCenterY))

        // Iron helmet cap
        val helmet =
            Path().apply {
                moveTo(centerX - size * 0.21f, headCenterY - size * 0.01f)
                quadraticTo(centerX, headCenterY - size * 0.36f, centerX + size * 0.21f, headCenterY - size * 0.01f)
                close()
            }
        drawPath(helmet, armorColor)

        // Glowing eyes
        drawCircle(color = eyeColor, radius = size * 0.035f, center = Offset(centerX - size * 0.07f, headCenterY + size * 0.02f))
        drawCircle(color = eyeColor, radius = size * 0.035f, center = Offset(centerX + size * 0.07f, headCenterY + size * 0.02f))
    }
}
