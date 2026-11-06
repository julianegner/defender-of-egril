package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw a small spiderling icon used for Araxxa's summoned swarm units.
 */
fun DrawScope.drawSpiderlingSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val bodyColor = Color(0xFF2F1B0C)
    val eyeColor = Color(0xFFB6FF6A)
    val pathOutlineWidth = 3f
    val bodyCenterY = centerY - size * 0.02f

    withTransform({ scale(headScale, headScale, Offset(centerX, bodyCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.18f,
                center = Offset(centerX, bodyCenterY - size * 0.12f),
                style = Stroke(width = 2f),
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.22f, bodyCenterY - size * 0.1f),
                size = Size(size * 0.44f, size * 0.34f),
                style = Stroke(width = 2f),
            )
        }

        repeat(4) { index ->
            val legYOffset = (-0.14f + index * 0.1f) * size
            val legLength = size * (0.18f + index * 0.015f)
            drawLine(
                color = bodyColor,
                start = Offset(centerX - size * 0.08f, bodyCenterY + legYOffset),
                end = Offset(centerX - size * 0.08f - legLength, bodyCenterY + legYOffset - size * 0.05f),
                strokeWidth = size * 0.06f,
            )
            drawLine(
                color = bodyColor,
                start = Offset(centerX + size * 0.08f, bodyCenterY + legYOffset),
                end = Offset(centerX + size * 0.08f + legLength, bodyCenterY + legYOffset - size * 0.05f),
                strokeWidth = size * 0.06f,
            )
        }

        drawOval(
            color = bodyColor,
            topLeft = Offset(centerX - size * 0.2f, bodyCenterY - size * 0.08f),
            size = Size(size * 0.4f, size * 0.3f),
        )
        drawCircle(
            color = bodyColor,
            radius = size * 0.14f,
            center = Offset(centerX, bodyCenterY - size * 0.12f),
        )
        drawCircle(color = eyeColor, radius = size * 0.03f, center = Offset(centerX - size * 0.05f, bodyCenterY - size * 0.14f))
        drawCircle(color = eyeColor, radius = size * 0.03f, center = Offset(centerX + size * 0.05f, bodyCenterY - size * 0.14f))
    }
}

/**
 * Draw Araxxa the Giant Spider villain.
 */
fun DrawScope.drawAraxxaSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val abdomenColor = Color(0xFF1D130B)
    val thoraxColor = Color(0xFF3B2416)
    val stripeColor = Color(0xFFDDD2B4)
    val eyeColor = Color(0xFFFF3D6E)
    val bodyCenterY = centerY - size * 0.06f

    // Large legs outside the head-scale transform so big-head mode does not distort them too much.
    repeat(4) { index ->
        val legYOffset = (-0.22f + index * 0.14f) * size
        val outerYOffset = legYOffset - size * 0.08f
        drawLine(
            color = thoraxColor,
            start = Offset(centerX - size * 0.12f, bodyCenterY + legYOffset),
            end = Offset(centerX - size * 0.45f, bodyCenterY + outerYOffset),
            strokeWidth = size * 0.065f,
        )
        drawLine(
            color = thoraxColor,
            start = Offset(centerX + size * 0.12f, bodyCenterY + legYOffset),
            end = Offset(centerX + size * 0.45f, bodyCenterY + outerYOffset),
            strokeWidth = size * 0.065f,
        )
    }

    withTransform({ scale(headScale, headScale, Offset(centerX, bodyCenterY)) }) {
        if (outlineColor != null) {
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.25f, bodyCenterY - size * 0.22f),
                size = Size(size * 0.5f, size * 0.4f),
                style = Stroke(width = 2f),
            )
            drawOval(
                color = outlineColor,
                topLeft = Offset(centerX - size * 0.33f, bodyCenterY - size * 0.02f),
                size = Size(size * 0.66f, size * 0.42f),
                style = Stroke(width = 2f),
            )
        }

        drawOval(
            color = thoraxColor,
            topLeft = Offset(centerX - size * 0.23f, bodyCenterY - size * 0.2f),
            size = Size(size * 0.46f, size * 0.36f),
        )
        drawOval(
            color = abdomenColor,
            topLeft = Offset(centerX - size * 0.31f, bodyCenterY),
            size = Size(size * 0.62f, size * 0.38f),
        )
        drawRect(
            color = stripeColor,
            topLeft = Offset(centerX - size * 0.05f, bodyCenterY + size * 0.04f),
            size = Size(size * 0.1f, size * 0.26f),
        )
        drawCircle(color = eyeColor, radius = size * 0.045f, center = Offset(centerX - size * 0.08f, bodyCenterY - size * 0.08f))
        drawCircle(color = eyeColor, radius = size * 0.045f, center = Offset(centerX + size * 0.08f, bodyCenterY - size * 0.08f))
        drawCircle(color = eyeColor, radius = size * 0.03f, center = Offset(centerX - size * 0.16f, bodyCenterY - size * 0.01f))
        drawCircle(color = eyeColor, radius = size * 0.03f, center = Offset(centerX + size * 0.16f, bodyCenterY - size * 0.01f))
    }
}
