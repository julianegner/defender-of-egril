package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform

/**
 * Draw ogre symbol (very large creature)
 */
fun DrawScope.drawOgreSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null, headScale: Float = 1.0f) {
    val headCenterY = centerY - size * 0.05f

    // Head with facial features (scaled)
    withTransform({ scale(headScale, headScale, Offset(centerX, headCenterY)) }) {
        if (outlineColor != null) {
            // Head outline
            drawCircle(
                color = outlineColor,
                radius = size * 0.35f + 2f,
                center = Offset(centerX, headCenterY),
                style = Stroke(width = 3f)
            )
        }
        // Huge head
        drawCircle(
            color = Color(0xFFA0522D), // Sienna/brown
            radius = size * 0.35f,
            center = Offset(centerX, headCenterY)
        )

        // Big eyes
        drawCircle(color = Color.White, radius = size * 0.08f, center = Offset(centerX - size * 0.15f, centerY - size * 0.1f))
        drawCircle(color = Color.White, radius = size * 0.08f, center = Offset(centerX + size * 0.15f, centerY - size * 0.1f))
        drawCircle(color = Color.Black, radius = size * 0.04f, center = Offset(centerX - size * 0.15f, centerY - size * 0.1f))
        drawCircle(color = Color.Black, radius = size * 0.04f, center = Offset(centerX + size * 0.15f, centerY - size * 0.1f))

        // Mouth
        drawLine(
            color = Color.Black,
            start = Offset(centerX - size * 0.15f, centerY + size * 0.1f),
            end = Offset(centerX + size * 0.15f, centerY + size * 0.1f),
            strokeWidth = 3f
        )
    }

    // Massive body (not scaled)
    drawRect(
        color = Color(0xFF8B7355), // Burlywood
        topLeft = Offset(centerX - size * 0.3f, centerY + size * 0.25f),
        size = Size(size * 0.6f, size * 0.2f)
    )
}
