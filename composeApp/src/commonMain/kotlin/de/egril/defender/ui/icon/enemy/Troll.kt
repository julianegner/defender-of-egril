package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw troll symbol (large, rocky creature - bridge builder)
 */
fun DrawScope.drawTrollSymbol(centerX: Float, centerY: Float, size: Float) {
    // Large rocky head
    drawCircle(
        color = Color(0xFF696969), // DimGray - rocky appearance
        radius = size * 0.33f,
        center = Offset(centerX, centerY - size * 0.05f)
    )
    
    // Eyes
    drawCircle(color = Color(0xFFFFFF00), radius = size * 0.07f, center = Offset(centerX - size * 0.13f, centerY - size * 0.08f)) // Yellow eyes
    drawCircle(color = Color(0xFFFFFF00), radius = size * 0.07f, center = Offset(centerX + size * 0.13f, centerY - size * 0.08f))
    drawCircle(color = Color.Black, radius = size * 0.03f, center = Offset(centerX - size * 0.13f, centerY - size * 0.08f))
    drawCircle(color = Color.Black, radius = size * 0.03f, center = Offset(centerX + size * 0.13f, centerY - size * 0.08f))
    
    // Large mouth
    drawLine(
        color = Color.Black,
        start = Offset(centerX - size * 0.18f, centerY + size * 0.08f),
        end = Offset(centerX + size * 0.18f, centerY + size * 0.08f),
        strokeWidth = 3f
    )
    
    // Tusks/fangs
    drawLine(
        color = Color.White,
        start = Offset(centerX - size * 0.08f, centerY + size * 0.08f),
        end = Offset(centerX - size * 0.1f, centerY + size * 0.18f),
        strokeWidth = 4f
    )
    drawLine(
        color = Color.White,
        start = Offset(centerX + size * 0.08f, centerY + size * 0.08f),
        end = Offset(centerX + size * 0.1f, centerY + size * 0.18f),
        strokeWidth = 4f
    )
    
    // Rocky/muscular body
    drawRect(
        color = Color(0xFF808080), // Gray - rocky appearance
        topLeft = Offset(centerX - size * 0.28f, centerY + size * 0.22f),
        size = Size(size * 0.56f, size * 0.22f)
    )
}
