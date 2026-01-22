package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draw evil wizard symbol (pointed hat with mystical energy)
 */
fun DrawScope.drawEvilWizardSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null) {
    val outlineWidth = 2f
    val pathOutlineWidth = 3f  // Thicker for paths to match visual appearance
    
    // Draw outline first (behind main drawing) using stroke style for uniform thickness
    if (outlineColor != null) {
        // Wizard hat outline (triangle) - use stroke for uniform outline with thicker stroke
        val hatOutlinePath = Path().apply {
            moveTo(centerX, centerY - size * 0.4f)
            lineTo(centerX - size * 0.3f, centerY)
            lineTo(centerX + size * 0.3f, centerY)
            close()
        }
        drawPath(hatOutlinePath, outlineColor, style = Stroke(width = pathOutlineWidth))
        
        // Hat brim outline - use stroke for uniform outline with thicker stroke
        val brimPath = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(
                left = centerX - size * 0.35f,
                top = centerY,
                right = centerX + size * 0.35f,
                bottom = centerY + size * 0.08f
            ))
        }
        drawPath(brimPath, outlineColor, style = Stroke(width = pathOutlineWidth))
        
        // Face outline - use stroke for uniform outline
        drawCircle(
            color = outlineColor,
            radius = size * 0.2f + outlineWidth / 2,
            center = Offset(centerX, centerY + size * 0.15f),
            style = Stroke(width = outlineWidth)
        )
        
        // Staff outline - use thicker stroke to match path widths
        drawLine(
            color = outlineColor,
            start = Offset(centerX + size * 0.25f, centerY + size * 0.1f),
            end = Offset(centerX + size * 0.35f, centerY + size * 0.45f),
            strokeWidth = 3f + pathOutlineWidth
        )
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
    // Orb on staff (draw filled first)
    drawCircle(color = Color(0xFF9400D3), radius = size * 0.08f, center = Offset(centerX + size * 0.35f, centerY + size * 0.05f))
    
    // Draw orb outline last so it appears on top
    if (outlineColor != null) {
        drawCircle(
            color = outlineColor,
            radius = size * 0.08f + outlineWidth / 2,
            center = Offset(centerX + size * 0.35f, centerY + size * 0.05f),
            style = Stroke(width = outlineWidth)
        )
    }
}
