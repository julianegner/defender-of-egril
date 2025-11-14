package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw ballista symbol (crossbow-like weapon)
 */
fun DrawScope.drawBallistaSymbol(centerX: Float, centerY: Float, size: Float) {
    // Horizontal beam
    drawRect(
        color = Color(0xFF8B4513), // Brown
        topLeft = Offset(centerX - size * 0.5f, centerY - size * 0.08f),
        size = Size(size, size * 0.16f)
    )
    
    // Vertical support
    drawRect(
        color = Color(0xFF654321),
        topLeft = Offset(centerX - size * 0.05f, centerY - size * 0.15f),
        size = Size(size * 0.1f, size * 0.5f)
    )
    
    // Bowstring
    drawLine(
        color = Color(0xFFFFFFDD),
        start = Offset(centerX - size * 0.45f, centerY - size * 0.15f),
        end = Offset(centerX - size * 0.45f, centerY + size * 0.15f),
        strokeWidth = 2f
    )
    
    // Bolt
    drawLine(
        color = Color(0xFF696969),
        start = Offset(centerX - size * 0.3f, centerY),
        end = Offset(centerX + size * 0.4f, centerY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    
    // Bolt head
    val boltPath = Path().apply {
        moveTo(centerX + size * 0.4f, centerY)
        lineTo(centerX + size * 0.3f, centerY - size * 0.08f)
        lineTo(centerX + size * 0.3f, centerY + size * 0.08f)
        close()
    }
    drawPath(boltPath, Color(0xFF696969))
}
