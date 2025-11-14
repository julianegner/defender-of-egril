package de.egril.defender.ui.icon.defender

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw dwarven mine symbol (two towers with axe, gold bar, and gem)
 */
fun DrawScope.drawMineSymbol(centerX: Float, centerY: Float, size: Float) {
    // Left tower
    val leftTowerPath = Path().apply {
        moveTo(centerX - size * 0.45f, centerY + size * 0.3f)  // Bottom left
        lineTo(centerX - size * 0.35f, centerY - size * 0.3f)  // Top left
        lineTo(centerX - size * 0.2f, centerY - size * 0.3f)   // Top right
        lineTo(centerX - size * 0.15f, centerY + size * 0.3f)  // Bottom right
        close()
    }
    drawPath(leftTowerPath, Color(0xFF8B7355))  // Brown
    
    // Right tower
    val rightTowerPath = Path().apply {
        moveTo(centerX + size * 0.15f, centerY + size * 0.3f)  // Bottom left
        lineTo(centerX + size * 0.2f, centerY - size * 0.3f)   // Top left
        lineTo(centerX + size * 0.35f, centerY - size * 0.3f)  // Top right
        lineTo(centerX + size * 0.45f, centerY + size * 0.3f)  // Bottom right
        close()
    }
    drawPath(rightTowerPath, Color(0xFF8B7355))  // Brown
    
    // Axe in the middle
    // Axe handle
    drawLine(
        color = Color(0xFF654321),  // Dark brown handle
        start = Offset(centerX - size * 0.05f, centerY - size * 0.1f),
        end = Offset(centerX + size * 0.05f, centerY + size * 0.2f),
        strokeWidth = 3f
    )
    // Axe blade
    val axePath = Path().apply {
        moveTo(centerX - size * 0.15f, centerY - size * 0.15f)
        lineTo(centerX, centerY - size * 0.05f)
        lineTo(centerX - size * 0.1f, centerY)
        close()
    }
    drawPath(axePath, Color.Gray)
    
    // Gold bar at bottom left
    drawRect(
        color = Color(0xFFFFD700),  // Gold
        topLeft = Offset(centerX - size * 0.35f, centerY + size * 0.35f),
        size = Size(size * 0.15f, size * 0.08f)
    )
    
    // Gem at bottom right (diamond shape)
    val gemPath = Path().apply {
        moveTo(centerX + size * 0.25f, centerY + size * 0.35f)  // Top
        lineTo(centerX + size * 0.3f, centerY + size * 0.39f)   // Right
        lineTo(centerX + size * 0.25f, centerY + size * 0.43f)  // Bottom
        lineTo(centerX + size * 0.2f, centerY + size * 0.39f)   // Left
        close()
    }
    drawPath(gemPath, Color(0xFF00CED1))  // Cyan/turquoise gem
}
