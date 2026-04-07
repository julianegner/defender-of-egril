package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw dragon's lair symbol (cave with smoke)
 */
fun DrawScope.drawDragonLairSymbol(centerX: Float, centerY: Float, size: Float, dragonAlive: Boolean = true) {
    // Cave opening
    val cavePath = Path().apply {
        moveTo(centerX - size * 0.3f, centerY + size * 0.3f)
        lineTo(centerX - size * 0.3f, centerY)
        quadraticTo(centerX, centerY - size * 0.4f, centerX + size * 0.3f, centerY)
        lineTo(centerX + size * 0.3f, centerY + size * 0.3f)
        close()
    }
    drawPath(cavePath, Color.Black)
    
    // Smoke/steam coming out
    for (i in 0..2) {
        drawCircle(
            color = Color.Gray.copy(alpha = 0.4f),
            radius = size * 0.1f,
            center = Offset(centerX + (i - 1) * size * 0.15f, centerY - size * 0.5f)
        )
    }
    
    // Dragon eyes in the darkness (only if dragon is alive)
    if (dragonAlive) {
        drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY - size * 0.1f))
        drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY - size * 0.1f))
    }
}
