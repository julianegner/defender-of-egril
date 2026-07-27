package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw a Dragon-Terror — a small, fast flying dragon summoned by Ignis-Va.
 *
 * Visually smaller and more compact than the regular [drawDragonSymbol]: lean orange body, narrow
 * bat-like wings, bright ember-yellow eyes, and a short tail with a flame tip to suggest swift
 * aerial movement.
 */
fun DrawScope.drawDragonTerrorSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val bodyColor = Color(0xFFCC4400) // Bright burnt-orange body
    val wingColor = Color(0xFF8B2500) // Darker orange-brown wings
    val eyeColor = Color(0xFFFFCC00) // Bright yellow eyes
    val headCenterX = centerX + size * 0.22f
    val headCenterY = centerY - size * 0.14f

    // Compact body
    if (outlineColor != null) {
        drawOval(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.18f, centerY - size * 0.10f),
            size = Size(size * 0.36f + 4f, size * 0.18f + 4f),
            style = Stroke(width = 3f),
        )
    }
    drawOval(
        color = bodyColor,
        topLeft = Offset(centerX - size * 0.18f, centerY - size * 0.10f),
        size = Size(size * 0.36f, size * 0.18f),
    )

    // Narrow bat-like wings (smaller than the regular dragon's)
    val leftWing =
        Path().apply {
            moveTo(centerX - size * 0.06f, centerY - size * 0.04f)
            lineTo(centerX - size * 0.36f, centerY - size * 0.30f)
            lineTo(centerX - size * 0.16f, centerY - size * 0.08f)
            close()
        }
    val rightWing =
        Path().apply {
            moveTo(centerX + size * 0.06f, centerY - size * 0.04f)
            lineTo(centerX + size * 0.36f, centerY - size * 0.30f)
            lineTo(centerX + size * 0.16f, centerY - size * 0.08f)
            close()
        }
    drawPath(leftWing, wingColor)
    drawPath(rightWing, wingColor)

    // Short tail with flame tip
    drawLine(
        color = bodyColor,
        start = Offset(centerX - size * 0.18f, centerY + size * 0.02f),
        end = Offset(centerX - size * 0.30f, centerY + size * 0.18f),
        strokeWidth = 3.5f,
    )
    // Flame tip
    drawCircle(
        color = Color(0xFFFF6600).copy(alpha = 0.8f),
        radius = size * 0.05f,
        center = Offset(centerX - size * 0.32f, centerY + size * 0.22f),
    )

    // Head (scaled for "big head" mode)
    withTransform({ scale(headScale, headScale, Offset(headCenterX, headCenterY)) }) {
        drawCircle(
            color = bodyColor,
            radius = size * 0.13f,
            center = Offset(headCenterX, headCenterY),
        )
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.13f,
                center = Offset(headCenterX, headCenterY),
                style = Stroke(width = 2.5f),
            )
        }
        // Ember-yellow eye
        drawCircle(
            color = eyeColor,
            radius = size * 0.045f,
            center = Offset(headCenterX + size * 0.04f, headCenterY - size * 0.04f),
        )
    }

    // Small fire breath puffs ahead of the head
    drawCircle(
        color = Color(0xFFFF4500).copy(alpha = 0.55f),
        radius = size * 0.06f,
        center = Offset(centerX + size * 0.38f, centerY - size * 0.12f),
    )
    drawCircle(
        color = Color(0xFFFFBF00).copy(alpha = 0.45f),
        radius = size * 0.04f,
        center = Offset(centerX + size * 0.46f, centerY - size * 0.14f),
    )
}
