package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draw bow symbol (curved bow with string)
 */
fun DrawScope.drawBowSymbol(centerX: Float, centerY: Float, size: Float) {
    val bowHeight = size * 0.8f
    val bowWidth = size * 0.5f
    
    // Bow arc (spanned bow with pronounced curve)
    val path = Path().apply {
        moveTo(centerX + bowWidth / 2, centerY - bowHeight / 2)
        cubicTo(
            centerX - bowWidth * 0.1f, centerY - bowHeight / 4,
            centerX - bowWidth * 0.1f, centerY + bowHeight / 4,
            centerX + bowWidth / 2, centerY + bowHeight / 2
        )
    }
    drawPath(path, Color(0xFFD2691E), style = Stroke(width = 3f, cap = StrokeCap.Round))
    
    // Bow string
    drawLine(
        color = Color(0xFFFFFFDD),
        start = Offset(centerX + bowWidth / 2, centerY - bowHeight / 2),
        end = Offset(centerX + bowWidth / 2, centerY + bowHeight / 2),
        strokeWidth = 1.5f
    )
    
    // Arrow (extends to bow string on the right)
    drawLine(
        color = Color(0xFFC0C0C0),
        start = Offset(centerX - bowWidth / 3, centerY),
        end = Offset(centerX + bowWidth / 2, centerY),
        strokeWidth = 2f
    )
    // Arrowhead (pointing left)
    val arrowPath = Path().apply {
        moveTo(centerX - bowWidth / 3, centerY)
        lineTo(centerX - bowWidth / 4, centerY - size * 0.1f)
        lineTo(centerX - bowWidth / 4, centerY + size * 0.1f)
        close()
    }
    drawPath(arrowPath, Color(0xFFC0C0C0))
}
