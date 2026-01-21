package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw goblin symbol (small creature with pointy ears)
 */
fun DrawScope.drawGoblinSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null) {
    val outlineWidth = 3f
    
    // Draw outline first (behind main drawing)
    if (outlineColor != null) {
        // Head outline (circle)
        drawCircle(
            color = outlineColor,
            radius = size * 0.3f + outlineWidth,
            center = Offset(centerX, centerY - size * 0.1f)
        )
        
        // Pointy ears outline
        val earPath1 = Path().apply {
            moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
            lineTo(centerX - size * 0.45f - outlineWidth, centerY - size * 0.25f - outlineWidth)
            lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
            close()
        }
        val earPath2 = Path().apply {
            moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
            lineTo(centerX + size * 0.45f + outlineWidth, centerY - size * 0.25f - outlineWidth)
            lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
            close()
        }
        drawPath(earPath1, outlineColor)
        drawPath(earPath2, outlineColor)
        
        // Body outline
        drawRect(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.15f - outlineWidth, centerY + size * 0.15f - outlineWidth),
            size = Size(size * 0.3f + outlineWidth * 2, size * 0.25f + outlineWidth * 2)
        )
    }
    
    // Head (circle)
    drawCircle(
        color = Color(0xFF90EE90), // Light green
        radius = size * 0.3f,
        center = Offset(centerX, centerY - size * 0.1f)
    )
    
    // Pointy ears
    val earPath1 = Path().apply {
        moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
        lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
        lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
        close()
    }
    val earPath2 = Path().apply {
        moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
        lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
        lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
        close()
    }
    drawPath(earPath1, Color(0xFF90EE90))
    drawPath(earPath2, Color(0xFF90EE90))
    
    // Eyes
    drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY - size * 0.15f))
    drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY - size * 0.15f))
    
    // Body (small)
    drawRect(
        color = Color(0xFF8B4513), // Brown
        topLeft = Offset(centerX - size * 0.15f, centerY + size * 0.15f),
        size = Size(size * 0.3f, size * 0.25f)
    )
}
