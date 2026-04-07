package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Draw wizard symbol (star for magic)
 */
fun DrawScope.drawWizardSymbol(centerX: Float, centerY: Float, size: Float) {
    val outerRadius = size * 0.5f
    val innerRadius = size * 0.2f
    val points = 5
    
    val path = Path()
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = PI / points * i - PI / 2
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    
    drawPath(path, Color(0xFFFFD700)) // Gold
    drawPath(path, Color(0xFFFF6B00), style = Stroke(width = 2f)) // Orange outline
    
    // Magic sparkle
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = size * 0.08f,
        center = Offset(centerX, centerY)
    )
}
