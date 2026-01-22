package de.egril.defender.ui.icon.enemy

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draw ork symbol (larger, muscular creature)
 */
fun DrawScope.drawOrkSymbol(centerX: Float, centerY: Float, size: Float, outlineColor: Color? = null) {
    val outlineWidth = 2f
    
    // Draw outline first (behind main drawing) using stroke style for uniform thickness
    if (outlineColor != null) {
        // Head outline - use stroke for uniform outline
        val headPath = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(
                left = centerX - size * 0.25f,
                top = centerY - size * 0.3f,
                right = centerX + size * 0.25f,
                bottom = centerY + size * 0.05f
            ))
        }
        drawPath(headPath, outlineColor, style = Stroke(width = outlineWidth))
        
        // Body outline - use stroke for uniform outline
        val bodyPath = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(
                left = centerX - size * 0.25f,
                top = centerY + size * 0.1f,
                right = centerX + size * 0.25f,
                bottom = centerY + size * 0.4f
            ))
        }
        drawPath(bodyPath, outlineColor, style = Stroke(width = outlineWidth))
        
        // Tusks outline - use stroke for uniform outline
        val tuskPath1 = Path().apply {
            moveTo(centerX - size * 0.15f, centerY)
            lineTo(centerX - size * 0.25f, centerY + size * 0.15f)
            lineTo(centerX - size * 0.1f, centerY + size * 0.05f)
            close()
        }
        val tuskPath2 = Path().apply {
            moveTo(centerX + size * 0.15f, centerY)
            lineTo(centerX + size * 0.25f, centerY + size * 0.15f)
            lineTo(centerX + size * 0.1f, centerY + size * 0.05f)
            close()
        }
        drawPath(tuskPath1, outlineColor, style = Stroke(width = outlineWidth))
        drawPath(tuskPath2, outlineColor, style = Stroke(width = outlineWidth))
    }
    
    // Head (larger square-ish)
    drawRect(
        color = Color(0xFF556B2F), // Dark olive green
        topLeft = Offset(centerX - size * 0.25f, centerY - size * 0.3f),
        size = Size(size * 0.5f, size * 0.35f)
    )
    
    // Tusks
    val tuskPath1 = Path().apply {
        moveTo(centerX - size * 0.15f, centerY)
        lineTo(centerX - size * 0.25f, centerY + size * 0.15f)
        lineTo(centerX - size * 0.1f, centerY + size * 0.05f)
        close()
    }
    val tuskPath2 = Path().apply {
        moveTo(centerX + size * 0.15f, centerY)
        lineTo(centerX + size * 0.25f, centerY + size * 0.15f)
        lineTo(centerX + size * 0.1f, centerY + size * 0.05f)
        close()
    }
    drawPath(tuskPath1, Color.White)
    drawPath(tuskPath2, Color.White)
    
    // Eyes
    drawCircle(color = Color.Yellow, radius = size * 0.06f, center = Offset(centerX - size * 0.12f, centerY - size * 0.18f))
    drawCircle(color = Color.Yellow, radius = size * 0.06f, center = Offset(centerX + size * 0.12f, centerY - size * 0.18f))
    
    // Body (large)
    drawRect(
        color = Color(0xFF696969), // Gray armor
        topLeft = Offset(centerX - size * 0.25f, centerY + size * 0.1f),
        size = Size(size * 0.5f, size * 0.3f)
    )
}
