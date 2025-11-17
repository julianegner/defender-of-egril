package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw ogre symbol (very large creature)
 */
fun DrawScope.drawOgreSymbol(centerX: Float, centerY: Float, size: Float) {
    // Huge head
    drawCircle(
        color = Color(0xFFA0522D), // Sienna/brown
        radius = size * 0.35f,
        center = Offset(centerX, centerY - size * 0.05f)
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
    
    // Massive body
    drawRect(
        color = Color(0xFF8B7355), // Burlywood
        topLeft = Offset(centerX - size * 0.3f, centerY + size * 0.25f),
        size = Size(size * 0.6f, size * 0.2f)
    )
}
