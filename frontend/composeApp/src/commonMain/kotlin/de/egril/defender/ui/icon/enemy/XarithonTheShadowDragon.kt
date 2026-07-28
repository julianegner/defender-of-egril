package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Xarithon the Shadow Dragon — a massive void-black dragon wreathed in dark energy.
 *
 * Visual design: deep black/dark-violet body with faintly glowing purple-shadow eyes and
 * translucent shadowy wings. A crown of dark spines frames the head. The icon is deliberately
 * darker and larger than the regular Dragon icon to convey a finale-boss presence.
 */
fun DrawScope.drawXarithonTheShadowDragonSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val bodyColor = Color(0xFF1A0A2E) // Deep void-black with purple tint
    val wingColor = Color(0xFF2D1B4E) // Dark violet wings
    val spineColor = Color(0xFF4B1A7A) // Dark purple spines
    val eyeGlow = Color(0xFFB000FF) // Bright purple glow
    val shadowAura = Color(0xFF5A00AA).copy(alpha = 0.35f) // Translucent shadow halo
    val outlineStroke = 2.5f

    val headCenterX = centerX + size * 0.22f
    val headCenterY = centerY - size * 0.18f

    // Shadow aura halo behind the whole dragon
    drawCircle(
        color = shadowAura,
        radius = size * 0.48f,
        center = Offset(centerX, centerY - size * 0.04f),
    )

    // Massive body (larger and more imposing than regular dragon)
    if (outlineColor != null) {
        drawOval(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.26f - outlineStroke, centerY - size * 0.14f - outlineStroke),
            size = Size(size * 0.52f + outlineStroke * 2, size * 0.28f + outlineStroke * 2),
            style = Stroke(width = outlineStroke),
        )
    }
    drawOval(
        color = bodyColor,
        topLeft = Offset(centerX - size * 0.26f, centerY - size * 0.14f),
        size = Size(size * 0.52f, size * 0.28f),
    )

    // Dark, tattered wings — left wing
    val leftWing =
        Path().apply {
            moveTo(centerX - size * 0.12f, centerY - size * 0.02f)
            lineTo(centerX - size * 0.50f, centerY - size * 0.40f)
            lineTo(centerX - size * 0.36f, centerY - size * 0.28f)
            lineTo(centerX - size * 0.44f, centerY - size * 0.10f)
            lineTo(centerX - size * 0.22f, centerY - size * 0.16f)
            close()
        }
    val rightWing =
        Path().apply {
            moveTo(centerX + size * 0.12f, centerY - size * 0.02f)
            lineTo(centerX + size * 0.50f, centerY - size * 0.40f)
            lineTo(centerX + size * 0.36f, centerY - size * 0.28f)
            lineTo(centerX + size * 0.44f, centerY - size * 0.10f)
            lineTo(centerX + size * 0.22f, centerY - size * 0.16f)
            close()
        }
    drawPath(leftWing, wingColor)
    drawPath(rightWing, wingColor)

    // Thick shadowy tail curling downward
    val tailPath =
        Path().apply {
            moveTo(centerX - size * 0.24f, centerY + size * 0.10f)
            lineTo(centerX - size * 0.36f, centerY + size * 0.26f)
            lineTo(centerX - size * 0.30f, centerY + size * 0.34f)
        }
    drawPath(tailPath, bodyColor.copy(alpha = 0.0f)) // invisible path anchor
    drawLine(
        color = bodyColor,
        start = Offset(centerX - size * 0.24f, centerY + size * 0.10f),
        end = Offset(centerX - size * 0.38f, centerY + size * 0.30f),
        strokeWidth = 6f,
    )
    drawLine(
        color = spineColor,
        start = Offset(centerX - size * 0.38f, centerY + size * 0.30f),
        end = Offset(centerX - size * 0.28f, centerY + size * 0.38f),
        strokeWidth = 4f,
    )

    // Crown of dark spines along the back of the head
    withTransform({ scale(headScale, headScale, Offset(headCenterX, headCenterY)) }) {
        for (i in -1..1) {
            val spineBaseX = headCenterX + i * size * 0.12f
            val spineBaseY = headCenterY - size * 0.22f
            drawLine(
                color = spineColor,
                start = Offset(spineBaseX, spineBaseY),
                end = Offset(spineBaseX + i * size * 0.06f, spineBaseY - size * 0.16f - i * i * size * 0.04f),
                strokeWidth = 3f,
            )
        }

        // Head — void-black with dark-purple tint
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.20f + outlineStroke,
                center = Offset(headCenterX, headCenterY),
                style = Stroke(width = outlineStroke),
            )
        }
        drawCircle(color = bodyColor, radius = size * 0.20f, center = Offset(headCenterX, headCenterY))

        // Scale-texture hints
        drawLine(
            color = spineColor.copy(alpha = 0.6f),
            start = Offset(headCenterX - size * 0.14f, headCenterY - size * 0.06f),
            end = Offset(headCenterX - size * 0.06f, headCenterY - size * 0.02f),
            strokeWidth = 1.5f,
        )
        drawLine(
            color = spineColor.copy(alpha = 0.6f),
            start = Offset(headCenterX + size * 0.06f, headCenterY - size * 0.06f),
            end = Offset(headCenterX + size * 0.14f, headCenterY - size * 0.02f),
            strokeWidth = 1.5f,
        )

        // Glowing purple eyes — bright core + outer glow
        val leftEyeX = headCenterX - size * 0.10f
        val rightEyeX = headCenterX + size * 0.10f
        val eyeY = headCenterY - size * 0.04f
        drawCircle(
            color = eyeGlow.copy(alpha = 0.40f),
            radius = size * 0.08f,
            center = Offset(leftEyeX, eyeY),
        )
        drawCircle(color = eyeGlow, radius = size * 0.05f, center = Offset(leftEyeX, eyeY))
        drawCircle(color = Color.White, radius = size * 0.02f, center = Offset(leftEyeX, eyeY))

        drawCircle(
            color = eyeGlow.copy(alpha = 0.40f),
            radius = size * 0.08f,
            center = Offset(rightEyeX, eyeY),
        )
        drawCircle(color = eyeGlow, radius = size * 0.05f, center = Offset(rightEyeX, eyeY))
        drawCircle(color = Color.White, radius = size * 0.02f, center = Offset(rightEyeX, eyeY))

        // Dark maw / fangs
        drawLine(
            color = Color(0xFF9966CC),
            start = Offset(headCenterX - size * 0.06f, headCenterY + size * 0.16f),
            end = Offset(headCenterX - size * 0.04f, headCenterY + size * 0.24f),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color(0xFF9966CC),
            start = Offset(headCenterX + size * 0.06f, headCenterY + size * 0.16f),
            end = Offset(headCenterX + size * 0.04f, headCenterY + size * 0.24f),
            strokeWidth = 2f,
        )
    }

    // Shadow flame breath — wispy violet-black tendrils in front of the mouth
    val flameColor = Color(0xFF6600CC).copy(alpha = 0.65f)
    for (i in 0..2) {
        drawCircle(
            color = flameColor,
            radius = size * 0.07f - i * size * 0.01f,
            center = Offset(
                headCenterX + size * 0.22f + i * size * 0.10f,
                headCenterY - size * 0.12f + i * size * 0.04f,
            ),
        )
    }
}
