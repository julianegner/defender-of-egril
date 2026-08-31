package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Zythar the Riftcaller — a dark sorcerer who tears holes in reality to summon rift portals.
 *
 * Rendered as a dark-robed spellcaster with blue-tinted portal energy, glowing orange eyes,
 * and a bifurcated rift staff that crackles with opposing blue (entry) and orange (exit) energy.
 */
fun DrawScope.drawZytharTheRiftcallerSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val pathOutlineWidth = 2.5f
    val headCenterY = centerY + size * 0.08f

    val robeColor = Color(0xFF0A0A1E)
    val robeHighlightColor = Color(0xFF1A1A40)
    val faceColor = Color(0xFFB8A0C0)
    val eyeColor = Color(0xFFFF8000) // orange eyes
    val auraColorBlue = Color(0xFF0040FF).copy(alpha = 0.22f)
    val auraColorOrange = Color(0xFFFF6000).copy(alpha = 0.18f)
    val portalBlue = Color(0xFF2080FF)
    val portalOrange = Color(0xFFFF7000)
    val staffColor = Color(0xFF2A1810)

    // Dual-colour aura (blue left / orange right)
    drawCircle(
        color = auraColorBlue,
        radius = size * 0.50f,
        center = Offset(centerX - size * 0.06f, centerY),
    )
    drawCircle(
        color = auraColorOrange,
        radius = size * 0.44f,
        center = Offset(centerX + size * 0.06f, centerY),
    )

    if (headScale == 1.0f) {
        // Robe body
        val robePath =
            Path().apply {
                moveTo(centerX, centerY - size * 0.04f)
                lineTo(centerX - size * 0.28f, centerY + size * 0.38f)
                lineTo(centerX + size * 0.28f, centerY + size * 0.38f)
                close()
            }
        if (outlineColor != null) drawPath(robePath, outlineColor, style = Stroke(width = pathOutlineWidth))
        drawPath(robePath, robeColor)
        // Robe highlight stripe
        val robe2 =
            Path().apply {
                moveTo(centerX, centerY - size * 0.04f)
                lineTo(centerX - size * 0.06f, centerY + size * 0.38f)
                lineTo(centerX + size * 0.06f, centerY + size * 0.38f)
                close()
            }
        drawPath(robe2, robeHighlightColor)

        // Bifurcated rift staff (blue tip left, orange tip right)
        val staffBase = Offset(centerX + size * 0.22f, centerY + size * 0.38f)
        val staffJoin = Offset(centerX + size * 0.30f, centerY + size * 0.00f)
        val staffTipBlue = Offset(centerX + size * 0.18f, centerY - size * 0.22f)
        val staffTipOrange = Offset(centerX + size * 0.40f, centerY - size * 0.18f)
        drawLine(color = staffColor, start = staffBase, end = staffJoin, strokeWidth = 3f, cap = StrokeCap.Round)
        drawLine(color = portalBlue, start = staffJoin, end = staffTipBlue, strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(color = portalOrange, start = staffJoin, end = staffTipOrange, strokeWidth = 2.5f, cap = StrokeCap.Round)
        // Staff tip orbs
        drawCircle(color = portalBlue, radius = size * 0.055f, center = staffTipBlue)
        drawCircle(color = portalOrange, radius = size * 0.05f, center = staffTipOrange)
    }

    // Head with optional scaling
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.20f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }
        drawCircle(color = faceColor, radius = size * 0.20f, center = Offset(centerX, headCenterY))

        // Glowing orange eyes
        val eyeOffsetX = size * 0.075f
        val eyeOffsetY = size * 0.02f
        drawCircle(color = eyeColor, radius = size * 0.042f, center = Offset(centerX - eyeOffsetX, headCenterY - eyeOffsetY))
        drawCircle(color = eyeColor, radius = size * 0.042f, center = Offset(centerX + eyeOffsetX, headCenterY - eyeOffsetY))

        // Dark hood top
        val hoodPath =
            Path().apply {
                moveTo(centerX - size * 0.22f, headCenterY)
                quadraticTo(centerX, headCenterY - size * 0.32f, centerX + size * 0.22f, headCenterY)
                close()
            }
        drawPath(hoodPath, robeColor)
    }
}

/**
 * Draw a demonling — a tiny imp-like scout summoned by Zythar.
 * Rendered as a miniature demon head with small curved horns and glowing orange eyes.
 */
fun DrawScope.drawDemonlingSymbol(
    centerX: Float,
    centerY: Float,
    size: Float,
    outlineColor: Color? = null,
    headScale: Float = 1.0f,
) {
    val outlineWidth = 2f
    val headCenterY = centerY - size * 0.05f

    val skinColor = Color(0xFF6B1010)
    val hornColor = Color(0xFF3A0808)
    val eyeColor = Color(0xFFFF6000)

    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            drawCircle(
                color = outlineColor,
                radius = size * 0.22f + outlineWidth / 2,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = outlineWidth),
            )
        }
        drawCircle(color = skinColor, radius = size * 0.22f, center = Offset(centerX, headCenterY))

        // Small curved horns
        val leftHornPath =
            Path().apply {
                moveTo(centerX - size * 0.08f, headCenterY - size * 0.18f)
                quadraticTo(
                    centerX - size * 0.18f, headCenterY - size * 0.36f,
                    centerX - size * 0.10f, headCenterY - size * 0.40f,
                )
            }
        val rightHornPath =
            Path().apply {
                moveTo(centerX + size * 0.08f, headCenterY - size * 0.18f)
                quadraticTo(
                    centerX + size * 0.18f, headCenterY - size * 0.36f,
                    centerX + size * 0.10f, headCenterY - size * 0.40f,
                )
            }
        drawPath(leftHornPath, hornColor, style = Stroke(width = 3f, cap = StrokeCap.Round))
        drawPath(rightHornPath, hornColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Glowing eyes
        val eyeOffX = size * 0.08f
        drawCircle(color = eyeColor, radius = size * 0.045f, center = Offset(centerX - eyeOffX, headCenterY))
        drawCircle(color = eyeColor, radius = size * 0.045f, center = Offset(centerX + eyeOffX, headCenterY))
    }
}
