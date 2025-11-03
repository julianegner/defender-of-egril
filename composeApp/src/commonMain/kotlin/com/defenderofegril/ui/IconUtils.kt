package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws a lightning bolt icon using Canvas for cross-platform compatibility
 */
@Composable
fun LightningIcon(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    color: Color = Color.Yellow
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        
        val path = Path().apply {
            // Lightning bolt shape
            moveTo(width * 0.5f, 0f)
            lineTo(width * 0.3f, height * 0.5f)
            lineTo(width * 0.5f, height * 0.5f)
            lineTo(width * 0.3f, height)
            lineTo(width * 0.7f, height * 0.4f)
            lineTo(width * 0.5f, height * 0.4f)
            close()
        }
        
        drawPath(path, color)
        drawPath(path, Color.White.copy(alpha = 0.3f), style = Stroke(width = 1f))
    }
}

/**
 * Draws a timer/clock icon using Canvas for cross-platform compatibility
 */
@Composable
fun TimerIcon(
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
    color: Color = Color(0xFFFFA500)
) {
    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        val radius = minOf(centerX, centerY) * 0.8f
        
        // Clock circle
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, centerY),
            style = Stroke(width = 1.5f)
        )
        
        // Hour hand
        drawLine(
            color = color,
            start = Offset(centerX, centerY),
            end = Offset(centerX, centerY - radius * 0.5f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        
        // Minute hand
        drawLine(
            color = color,
            start = Offset(centerX, centerY),
            end = Offset(centerX + radius * 0.6f, centerY),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        
        // Center dot
        drawCircle(
            color = color,
            radius = 1.5f,
            center = Offset(centerX, centerY)
        )
    }
}

/**
 * Draws a sword/attack icon using Canvas for cross-platform compatibility
 */
@Composable
fun SwordIcon(
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    color: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val width = this.size.width
        val height = this.size.height
        
        // Blade
        val bladePath = Path().apply {
            moveTo(width * 0.3f, height * 0.8f)
            lineTo(width * 0.7f, height * 0.2f)
            lineTo(width * 0.8f, height * 0.3f)
            lineTo(width * 0.4f, height * 0.9f)
            close()
        }
        drawPath(bladePath, Color(0xFFC0C0C0))  // Silver blade
        
        // Crossguard
        drawLine(
            color = Color(0xFFFFD700),  // Gold
            start = Offset(width * 0.15f, height * 0.7f),
            end = Offset(width * 0.55f, height * 0.95f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
        
        // Handle
        drawLine(
            color = Color(0xFF8B4513),  // Brown
            start = Offset(width * 0.2f, height * 0.75f),
            end = Offset(width * 0.35f, height * 0.9f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
        
        // Pommel
        drawCircle(
            color = Color(0xFFFFD700),  // Gold
            radius = width * 0.06f,
            center = Offset(width * 0.15f, height * 0.95f)
        )
    }
}
