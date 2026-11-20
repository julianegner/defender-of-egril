package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Ignis-Va, the Dragonvoice — a sinister dragon cultist who is half human, half dragon.
 *
 * Her icon shows a figure with draconic features: scaled amber skin, small horns, a dragon-crest
 * headdress, glowing ember eyes, and flame-licked robes in deep crimson and orange-gold. She is
 * visually distinct from both the regular dragon (a beast) and other humanoid villains.
 */
fun DrawScope.drawIgnisVaSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val robeColor = Color(0xFF8B1A1A) // Deep crimson robes
    val scaleColor = Color(0xFFB8620A) // Amber dragon scales
    val hornColor = Color(0xFFD4873A) // Warm orange-brown horns
    val eyeColor = Color(0xFFFF6600) // Ember-orange glowing eyes
    val flameOuter = Color(0xFFFF4500).copy(alpha = 0.7f) // Orange-red fire
    val flameInner = Color(0xFFFFBF00).copy(alpha = 0.8f) // Golden flame core
    val headCenterY = centerY - size * 0.12f
    val strokeW = 3f

    // Flowing crimson robes (body)
    if (outlineColor != null) {
        drawOval(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.28f, centerY + size * 0.04f),
            size = Size(size * 0.56f, size * 0.38f),
            style = Stroke(width = strokeW),
        )
    }
    drawOval(
        color = robeColor,
        topLeft = Offset(centerX - size * 0.28f, centerY + size * 0.04f),
        size = Size(size * 0.56f, size * 0.38f),
    )

    // Flame hem — small fire tongues at the bottom of the robe
    val flamePath =
        Path().apply {
            moveTo(centerX - size * 0.22f, centerY + size * 0.38f)
            lineTo(centerX - size * 0.12f, centerY + size * 0.52f)
            lineTo(centerX - size * 0.02f, centerY + size * 0.36f)
            lineTo(centerX + size * 0.06f, centerY + size * 0.50f)
            lineTo(centerX + size * 0.16f, centerY + size * 0.36f)
            lineTo(centerX + size * 0.24f, centerY + size * 0.44f)
            close()
        }
    drawPath(flamePath, flameOuter)
    val flameInnerPath =
        Path().apply {
            moveTo(centerX - size * 0.14f, centerY + size * 0.40f)
            lineTo(centerX - size * 0.08f, centerY + size * 0.50f)
            lineTo(centerX + size * 0.02f, centerY + size * 0.38f)
            lineTo(centerX + size * 0.10f, centerY + size * 0.47f)
            lineTo(centerX + size * 0.18f, centerY + size * 0.38f)
            close()
        }
    drawPath(flameInnerPath, flameInner)

    // Dragon-crest headdress (two swept-back horns above the head)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        val leftHorn =
            Path().apply {
                moveTo(centerX - size * 0.14f, headCenterY - size * 0.20f)
                lineTo(centerX - size * 0.32f, headCenterY - size * 0.44f)
                lineTo(centerX - size * 0.06f, headCenterY - size * 0.28f)
                close()
            }
        val rightHorn =
            Path().apply {
                moveTo(centerX + size * 0.14f, headCenterY - size * 0.20f)
                lineTo(centerX + size * 0.32f, headCenterY - size * 0.44f)
                lineTo(centerX + size * 0.06f, headCenterY - size * 0.28f)
                close()
            }
        drawPath(leftHorn, hornColor)
        drawPath(rightHorn, hornColor)

        // Head — amber dragon-scale complexion
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.24f + strokeW,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = strokeW),
            )
        }
        drawCircle(color = scaleColor, radius = size * 0.24f, center = Offset(centerX, headCenterY))

        // Scale texture hint — small dashes
        drawLine(
            color = Color(0xFF8B4513).copy(alpha = 0.5f),
            start = Offset(centerX - size * 0.16f, headCenterY - size * 0.08f),
            end = Offset(centerX - size * 0.08f, headCenterY - size * 0.04f),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color(0xFF8B4513).copy(alpha = 0.5f),
            start = Offset(centerX + size * 0.08f, headCenterY - size * 0.08f),
            end = Offset(centerX + size * 0.16f, headCenterY - size * 0.04f),
            strokeWidth = 2f,
        )

        // Glowing ember eyes
        drawCircle(
            color = eyeColor,
            radius = size * 0.065f,
            center = Offset(centerX - size * 0.1f, headCenterY - size * 0.04f),
        )
        drawCircle(
            color = eyeColor,
            radius = size * 0.065f,
            center = Offset(centerX + size * 0.1f, headCenterY - size * 0.04f),
        )
        // Bright eye cores
        drawCircle(
            color = Color.Yellow,
            radius = size * 0.03f,
            center = Offset(centerX - size * 0.1f, headCenterY - size * 0.04f),
        )
        drawCircle(
            color = Color.Yellow,
            radius = size * 0.03f,
            center = Offset(centerX + size * 0.1f, headCenterY - size * 0.04f),
        )

        // Small draconic fangs / chin detail
        drawLine(
            color = Color(0xFFFFECB3),
            start = Offset(centerX - size * 0.06f, headCenterY + size * 0.18f),
            end = Offset(centerX - size * 0.04f, headCenterY + size * 0.26f),
            strokeWidth = 2.5f,
        )
        drawLine(
            color = Color(0xFFFFECB3),
            start = Offset(centerX + size * 0.06f, headCenterY + size * 0.18f),
            end = Offset(centerX + size * 0.04f, headCenterY + size * 0.26f),
            strokeWidth = 2.5f,
        )
    }
}
