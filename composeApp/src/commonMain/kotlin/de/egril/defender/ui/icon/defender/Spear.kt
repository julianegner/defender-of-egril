package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draw spear symbol (vertical spear)
 */
fun DrawScope.drawSpearSymbol(centerX: Float, centerY: Float, size: Float, lineColor: Color = Color.White) {
    val shaftWidth = size * 0.1f
    val shaftHeight = size * 0.7f
    val spearheadHeight = size * 0.3f
    val spearheadWidth = size * 0.3f
    
    // Shaft
    drawRect(
        color = Color(0xFFD2691E), // Brown
        topLeft = Offset(centerX - shaftWidth / 2, centerY - shaftHeight / 2),
        size = Size(shaftWidth, shaftHeight)
    )
    
    // Spearhead
    val path = Path().apply {
        moveTo(centerX, centerY - shaftHeight / 2 - spearheadHeight)
        lineTo(centerX - spearheadWidth / 2, centerY - shaftHeight / 2)
        lineTo(centerX + spearheadWidth / 2, centerY - shaftHeight / 2)
        close()
    }
    drawPath(path, Color(0xFFC0C0C0)) // Silver
    drawPath(path, lineColor, style = Stroke(width = 1.5f))
}
