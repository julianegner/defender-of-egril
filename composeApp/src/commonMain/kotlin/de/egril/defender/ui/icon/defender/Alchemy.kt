package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draw alchemy symbol (potion flask)
 */
fun DrawScope.drawAlchemySymbol(centerX: Float, centerY: Float, size: Float) {
    // Flask body (trapezoid)
    val bodyPath = Path().apply {
        val topWidth = size * 0.3f
        val bottomWidth = size * 0.5f
        val bodyHeight = size * 0.5f
        val top = centerY - size * 0.1f
        val bottom = centerY + bodyHeight
        
        moveTo(centerX - bottomWidth / 2, bottom)
        lineTo(centerX + bottomWidth / 2, bottom)
        lineTo(centerX + topWidth / 2, top)
        lineTo(centerX - topWidth / 2, top)
        close()
    }
    drawPath(bodyPath, Color(0xFF00FF00).copy(alpha = 0.5f)) // Green transparent
    drawPath(bodyPath, Color(0xFF00AA00), style = Stroke(width = 2f))
    
    // Flask neck
    val neckWidth = size * 0.2f
    val neckHeight = size * 0.3f
    drawRect(
        color = Color(0xFFAAAAAA).copy(alpha = 0.3f),
        topLeft = Offset(centerX - neckWidth / 2, centerY - size * 0.1f - neckHeight),
        size = Size(neckWidth, neckHeight),
        style = Stroke(width = 1.5f)
    )
    
    // Bubbles
    drawCircle(
        color = Color(0xFF90EE90),
        radius = size * 0.08f,
        center = Offset(centerX - size * 0.15f, centerY + size * 0.2f)
    )
    drawCircle(
        color = Color(0xFF90EE90),
        radius = size * 0.06f,
        center = Offset(centerX + size * 0.1f, centerY + size * 0.15f)
    )
}
