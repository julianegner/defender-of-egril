package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws a shared "villain" marker: a small golden crown centred above an enemy icon, plus a faint
 * golden aura ring behind it. It is drawn for every villain (see [de.egril.defender.model.AttackerType.isVillain])
 * regardless of the villain's own symbol, giving a consistent, at-a-glance way to tell villains apart
 * from regular units on the battlefield (see issue #538).
 *
 * @param centerX  horizontal centre of the icon.
 * @param centerY  vertical centre of the icon.
 * @param iconSize the icon's drawing size (usually min(width, height)).
 */
fun DrawScope.drawVillainMarker(
    centerX: Float,
    centerY: Float,
    iconSize: Float,
) {
    val gold = Color(0xFFFFD24A)
    val goldOutline = Color(0xFF8A6A12)

    // Faint golden aura ring behind the unit so villains "glow" on the map.
    drawCircle(
        color = gold.copy(alpha = 0.22f),
        radius = iconSize * 0.46f,
        center = Offset(centerX, centerY),
    )
    drawCircle(
        color = gold.copy(alpha = 0.5f),
        radius = iconSize * 0.46f,
        center = Offset(centerX, centerY),
        style = Stroke(width = iconSize * 0.02f),
    )

    // Small three-point crown centred above the head.
    val crownWidth = iconSize * 0.34f
    val crownHeight = iconSize * 0.16f
    val crownTop = centerY - iconSize * 0.5f
    val crownBottom = crownTop + crownHeight
    val left = centerX - crownWidth / 2
    val right = centerX + crownWidth / 2

    val crown =
        Path().apply {
            moveTo(left, crownBottom)
            lineTo(left, crownTop + crownHeight * 0.4f)
            lineTo(left + crownWidth * 0.25f, crownBottom - crownHeight * 0.35f)
            lineTo(centerX, crownTop)
            lineTo(right - crownWidth * 0.25f, crownBottom - crownHeight * 0.35f)
            lineTo(right, crownTop + crownHeight * 0.4f)
            lineTo(right, crownBottom)
            close()
        }
    drawPath(crown, gold)
    drawPath(crown, goldOutline, style = Stroke(width = iconSize * 0.015f))
}
