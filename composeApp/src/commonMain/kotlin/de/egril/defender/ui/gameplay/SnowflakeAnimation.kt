package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated snowflakes overlay for frozen enemies
 */
@Composable
fun SnowflakeAnimation(
    modifier: Modifier = Modifier
) {
    // Animation state for snowflakes
    var animationTick by remember { mutableStateOf(0L) }
    
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(50) // Update every 50ms for smooth animation
            animationTick++
        }
    }
    
    // Create 4 snowflakes with different properties
    val snowflakes = remember {
        List(4) {
            SnowflakeData(
                initialY = Random.nextFloat() * 0.8f,  // Start at different heights
                speed = 0.003f + Random.nextFloat() * 0.002f,  // Slightly different speeds
                amplitude = 0.05f + Random.nextFloat() * 0.05f,  // Horizontal drift
                phase = Random.nextFloat() * 2f * PI.toFloat()  // Different phase for drift
            )
        }
    }
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        snowflakes.forEachIndexed { index, snowflake ->
            // Calculate position based on animation tick
            val progress = ((animationTick * snowflake.speed) % 1f).toFloat()
            val y = (snowflake.initialY + progress) * canvasHeight
            
            // Add horizontal drift
            val drift = sin(progress * 2f * PI.toFloat() + snowflake.phase) * snowflake.amplitude
            val x = (0.2f + index * 0.2f + drift) * canvasWidth
            
            // Only draw if within canvas bounds
            if (y < canvasHeight) {
                drawSnowflake(
                    center = Offset(x, y),
                    radius = canvasWidth * 0.06f,
                    color = Color.Cyan.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Data class for snowflake animation properties
 */
private data class SnowflakeData(
    val initialY: Float,
    val speed: Float,
    val amplitude: Float,
    val phase: Float
)

/**
 * Draw a simple snowflake at the given position
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnowflake(
    center: Offset,
    radius: Float,
    color: Color
) {
    val strokeWidth = radius * 0.15f
    
    // Draw 6 arms of the snowflake
    for (i in 0 until 6) {
        val angle = i * PI.toFloat() / 3f
        val cosAngle = cos(angle)
        val sinAngle = sin(angle)
        
        // Main arm
        drawLine(
            color = color,
            start = center,
            end = Offset(
                center.x + cosAngle * radius,
                center.y + sinAngle * radius
            ),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Small branches
        val branchLength = radius * 0.3f
        val branchPos = radius * 0.65f
        val branchX = center.x + cosAngle * branchPos
        val branchY = center.y + sinAngle * branchPos
        
        // Left branch
        val leftAngle = angle - PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(
                branchX + cos(leftAngle) * branchLength,
                branchY + sin(leftAngle) * branchLength
            ),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
        
        // Right branch
        val rightAngle = angle + PI.toFloat() / 6f
        drawLine(
            color = color,
            start = Offset(branchX, branchY),
            end = Offset(
                branchX + cos(rightAngle) * branchLength,
                branchY + sin(rightAngle) * branchLength
            ),
            strokeWidth = strokeWidth * 0.6f,
            cap = StrokeCap.Round
        )
    }
}
