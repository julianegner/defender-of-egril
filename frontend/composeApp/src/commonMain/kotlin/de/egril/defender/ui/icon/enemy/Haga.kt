package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Haga symbol (healing twin, villain).
 * Based on the green witch but with a distinctive golden star on the hat tip to mark her
 * as a unique villain rather than an ordinary green witch.
 */
fun DrawScope.drawHagaSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val headCenterY = centerY + size * 0.1f

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            val hatOutlinePath =
                Path().apply {
                    moveTo(centerX, centerY - size * 0.38f)
                    lineTo(centerX - size * 0.26f, centerY - size * 0.05f)
                    lineTo(centerX + size * 0.26f, centerY - size * 0.05f)
                    close()
                }
            drawPath(hatOutlinePath, outlineColor, style = Stroke(width = 3f))
            drawCircle(
                color = outlineColor,
                radius = size * 0.18f + 2f,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = 2f),
            )
        }

        // Deep emerald green hat (darker than regular green witch to distinguish)
        val hatPath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.38f)
                lineTo(centerX - size * 0.26f, centerY - size * 0.05f)
                lineTo(centerX + size * 0.26f, centerY - size * 0.05f)
                close()
            }
        drawPath(hatPath, Color(0xFF006400)) // Dark green

        // Hat brim
        drawRect(
            color = Color(0xFF006400),
            topLeft = Offset(centerX - size * 0.32f, centerY - size * 0.05f),
            size = Size(size * 0.64f, size * 0.06f),
        )

        // Golden star at the very tip of the hat (villain marker)
        val starTipY = centerY - size * 0.38f
        drawCircle(
            color = Color(0xFFFFD700),
            radius = size * 0.06f,
            center = Offset(centerX, starTipY),
        )

        // Face: slightly warmer than regular green witch
        drawCircle(
            color = Color(0xFFD4F0D4), // Pale green-tinted face
            radius = size * 0.18f,
            center = Offset(centerX, headCenterY),
        )

        // Eyes: vivid lime green
        drawCircle(
            color = Color(0xFF00C800),
            radius = size * 0.052f,
            center = Offset(centerX - size * 0.08f, headCenterY + size * 0.01f),
        )
        drawCircle(
            color = Color(0xFF00C800),
            radius = size * 0.052f,
            center = Offset(centerX + size * 0.08f, headCenterY + size * 0.01f),
        )
    }

    // Healing staff with bright healing orb (not scaled)
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX + size * 0.25f, centerY + size * 0.15f),
        end = Offset(centerX + size * 0.40f, centerY + size * 0.35f),
        strokeWidth = 2f,
    )
    // Bright green healing orb
    drawCircle(
        color = Color(0xFF00FF7F),
        radius = size * 0.08f,
        center = Offset(centerX + size * 0.40f, centerY + size * 0.35f),
    )
    // Inner glow dot
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = size * 0.03f,
        center = Offset(centerX + size * 0.40f, centerY + size * 0.35f),
    )
}
