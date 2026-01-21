package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope

/**
 * Draw evil wizard symbol (pointed hat with mystical energy)
 */
fun DrawScope.drawEvilWizardSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null) {
    val outlineWidth = 3f
    
    // Draw outline first (behind main drawing)
    if (outlineColor != null) {
        // Wizard hat outline (triangle)
        val hatOutlinePath = Path().apply {
            moveTo(centerX, centerY - size * 0.4f - outlineWidth)
            lineTo(centerX - size * 0.3f - outlineWidth, centerY + outlineWidth)
            lineTo(centerX + size * 0.3f + outlineWidth, centerY + outlineWidth)
            close()
        }
        drawPath(hatOutlinePath, outlineColor)
        
        // Hat brim outline
        drawRect(
            color = outlineColor,
            topLeft = Offset(centerX - size * 0.35f - outlineWidth, centerY - outlineWidth),
            size = Size(size * 0.7f + outlineWidth * 2, size * 0.08f + outlineWidth * 2)
        )
        
        // Face outline
        drawCircle(
            color = outlineColor,
            radius = size * 0.2f + outlineWidth,
            center = Offset(centerX, centerY + size * 0.15f)
        )
        
        // Staff outline
        drawLine(
            color = outlineColor,
            start = Offset(centerX + size * 0.25f, centerY + size * 0.1f),
            end = Offset(centerX + size * 0.35f, centerY + size * 0.45f),
            strokeWidth = 3f + outlineWidth * 2
        )
        // Orb outline on staff
        drawCircle(color = outlineColor, radius = size * 0.08f + outlineWidth, center = Offset(centerX + size * 0.35f, centerY + size * 0.05f))
    }
    
    // Wizard hat (triangle)
    val hatPath = Path().apply {
        moveTo(centerX, centerY - size * 0.4f)
        lineTo(centerX - size * 0.3f, centerY)
        lineTo(centerX + size * 0.3f, centerY)
        close()
    }
    drawPath(hatPath, Color(0xFF4B0082)) // Indigo
    
    // Hat brim
    drawRect(
        color = Color(0xFF4B0082),
        topLeft = Offset(centerX - size * 0.35f, centerY),
        size = Size(size * 0.7f, size * 0.08f)
    )
    
    // Face
    drawCircle(
        color = Color(0xFF8B4789), // Purple-ish
        radius = size * 0.2f,
        center = Offset(centerX, centerY + size * 0.15f)
    )
    
    // Glowing eyes
    drawCircle(color = Color(0xFFFF00FF), radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY + size * 0.12f))
    drawCircle(color = Color(0xFFFF00FF), radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY + size * 0.12f))
    
    // Staff
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX + size * 0.25f, centerY + size * 0.1f),
        end = Offset(centerX + size * 0.35f, centerY + size * 0.45f),
        strokeWidth = 3f
    )
    // Orb on staff
    drawCircle(color = Color(0xFF9400D3), radius = size * 0.08f, center = Offset(centerX + size * 0.35f, centerY + size * 0.05f))
}
