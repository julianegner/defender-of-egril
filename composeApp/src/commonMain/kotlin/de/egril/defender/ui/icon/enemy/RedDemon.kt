package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw red demon symbol (slow but tanky with red armor)
 */
fun DrawScope.drawRedDemonSymbol(centerX: Float, centerY: Float, size: Float, headScale: Float = 1.0f) {
    // Entire figure is the head/face (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, centerY)) }) {
        // Large armored body/head
        drawCircle(
            color = Color(0xFF8B0000), // Dark red
            radius = size * 0.35f,
            center = Offset(centerX, centerY)
        )

        // Armor plates
        drawRect(
            color = Color(0xFF4A0000),
            topLeft = Offset(centerX - size * 0.25f, centerY - size * 0.1f),
            size = Size(size * 0.5f, size * 0.2f)
        )

        // Large horns
        val hornPath1 = Path().apply {
            moveTo(centerX - size * 0.25f, centerY - size * 0.2f)
            lineTo(centerX - size * 0.4f, centerY - size * 0.45f)
            lineTo(centerX - size * 0.15f, centerY - size * 0.25f)
            close()
        }
        val hornPath2 = Path().apply {
            moveTo(centerX + size * 0.25f, centerY - size * 0.2f)
            lineTo(centerX + size * 0.4f, centerY - size * 0.45f)
            lineTo(centerX + size * 0.15f, centerY - size * 0.25f)
            close()
        }
        drawPath(hornPath1, Color(0xFF2A0000))
        drawPath(hornPath2, Color(0xFF2A0000))

        // Glowing eyes
        drawCircle(color = Color(0xFFFF4500), radius = size * 0.06f, center = Offset(centerX - size * 0.12f, centerY - size * 0.08f))
        drawCircle(color = Color(0xFFFF4500), radius = size * 0.06f, center = Offset(centerX + size * 0.12f, centerY - size * 0.08f))
    }
}
