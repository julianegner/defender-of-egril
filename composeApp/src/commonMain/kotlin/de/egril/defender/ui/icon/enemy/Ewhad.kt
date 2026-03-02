package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw Ewhad symbol (evil arch mage boss) - unique symbol ☠ Ψ
 */
fun DrawScope.drawEwhadSymbol(centerX: Float, centerY: Float, size: Float, headScale: Float = 1.0f) {
    val headCenterY = centerY - size * 0.05f

    // Large dark robe (not scaled)
    val robePath = Path().apply {
        moveTo(centerX, centerY - size * 0.45f)
        lineTo(centerX - size * 0.35f, centerY + size * 0.35f)
        lineTo(centerX + size * 0.35f, centerY + size * 0.35f)
        close()
    }
    drawPath(robePath, Color(0xFF0A0015)) // Almost black with purple tint

    // Elaborate hood with points (not scaled)
    val hoodPath = Path().apply {
        moveTo(centerX, centerY - size * 0.5f)
        lineTo(centerX - size * 0.35f, centerY - size * 0.1f)
        lineTo(centerX - size * 0.3f, centerY - size * 0.15f)
        lineTo(centerX, centerY - size * 0.45f)
        lineTo(centerX + size * 0.3f, centerY - size * 0.15f)
        lineTo(centerX + size * 0.35f, centerY - size * 0.1f)
        close()
    }
    drawPath(hoodPath, Color.Black)

    // Skull face (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        // Skull face (death aspect)
        drawCircle(
            color = Color(0xFFD3D3D3),
            radius = size * 0.2f,
            center = Offset(centerX, headCenterY)
        )

        // Skull eye sockets (glowing red)
        drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX - size * 0.1f, centerY - size * 0.1f))
        drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX + size * 0.1f, centerY - size * 0.1f))
        drawCircle(color = Color(0xFFFF0000), radius = size * 0.04f, center = Offset(centerX - size * 0.1f, centerY - size * 0.1f))
        drawCircle(color = Color(0xFFFF0000), radius = size * 0.04f, center = Offset(centerX + size * 0.1f, centerY - size * 0.1f))
    }

    // Crown/spikes on hood (not scaled)
    for (i in -1..1) {
        val path = Path().apply {
            val x = centerX + i * size * 0.15f
            moveTo(x, centerY - size * 0.45f)
            lineTo(x - size * 0.05f, centerY - size * 0.35f)
            lineTo(x + size * 0.05f, centerY - size * 0.35f)
            close()
        }
        drawPath(path, Color(0xFFFFD700)) // Gold
    }

    // Powerful staff (trident-like Ψ symbol) (not scaled)
    drawLine(
        color = Color(0xFF3A0060),
        start = Offset(centerX + size * 0.35f, centerY - size * 0.2f),
        end = Offset(centerX + size * 0.45f, centerY + size * 0.45f),
        strokeWidth = 4f
    )
    // Trident top (Ψ shape)
    drawLine(
        color = Color(0xFF8B00FF),
        start = Offset(centerX + size * 0.35f, centerY - size * 0.25f),
        end = Offset(centerX + size * 0.45f, centerY - size * 0.35f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF8B00FF),
        start = Offset(centerX + size * 0.45f, centerY - size * 0.25f),
        end = Offset(centerX + size * 0.45f, centerY - size * 0.35f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF8B00FF),
        start = Offset(centerX + size * 0.55f, centerY - size * 0.25f),
        end = Offset(centerX + size * 0.45f, centerY - size * 0.35f),
        strokeWidth = 3f
    )

    // Dark energy aura (not scaled)
    drawCircle(
        color = Color(0xFF4B0082).copy(alpha = 0.3f),
        radius = size * 0.5f,
        center = Offset(centerX, centerY)
    )
}
