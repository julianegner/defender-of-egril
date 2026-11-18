package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Visualize a bridge over a river tile
 */
@Composable
fun BridgeVisualization(
    bridge: Bridge,
    incomePerTile: Int,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Draw bridge arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val arcWidth = size.width * 0.8f
            val arcHeight = size.height * 0.4f

            // Bridge color based on type
            val bridgeColor =
                when (bridge.type) {
                    BridgeType.WOODEN -> Color(0xFF8B4513) // Brown
                    BridgeType.STONE -> Color(0xFF808080) // Gray
                    BridgeType.MAGICAL -> Color(0xFFFF00FF) // Magenta/purple for magical
                }

            // Draw top half arc
            drawArc(
                color = bridgeColor,
                startAngle = 180f, // Start from bottom-left
                sweepAngle = 180f, // Draw top half
                useCenter = false,
                topLeft =
                    androidx.compose.ui.geometry.Offset(
                        centerX - arcWidth / 2,
                        centerY - arcHeight / 2,
                    ),
                size =
                    androidx.compose.ui.geometry
                        .Size(arcWidth, arcHeight),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 6f),
            )
            // Draw second parallel arc to create the bridge deck effect
            drawArc(
                color = bridgeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft =
                    androidx.compose.ui.geometry.Offset(
                        centerX - arcWidth / 2,
                        centerY - arcHeight / 2 + 10f,
                    ),
                size =
                    androidx.compose.ui.geometry
                        .Size(arcWidth, arcHeight),
                style =
                    androidx.compose.ui.graphics.drawscope
                        .Stroke(width = 6f),
            )

            // For magical bridges, add sparkle effect above the arc
            if (bridge.type == BridgeType.MAGICAL) {
                // Draw sparkles around the arc (top side)
                val sparklePositions =
                    listOf(
                        androidx.compose.ui.geometry
                            .Offset(centerX - arcWidth / 3, centerY - arcHeight / 2 + 5),
                        androidx.compose.ui.geometry
                            .Offset(centerX, centerY - arcHeight / 2),
                        androidx.compose.ui.geometry
                            .Offset(centerX + arcWidth / 3, centerY - arcHeight / 2 + 5),
                    )
                sparklePositions.forEach { pos ->
                    drawCircle(
                        color = Color.White,
                        radius = 2f,
                        center = pos,
                    )
                }
            }
        }

        // Display health/turn count and coin income below the arc
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 4.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (bridge.type) {
                    BridgeType.WOODEN, BridgeType.STONE -> {
                        if (!bridge.isIndestructible) {
                            Text(
                                text = "${bridge.currentHealth.value}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    BridgeType.MAGICAL -> {
                        // Show remaining turns
                        Text(
                            text = "${bridge.turnsRemaining.value}T",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 13.sp,
                            color = Color(0xFFFFFF00), // Yellow
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Text(
                    text = "+$incomePerTile",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 13.sp,
                    color = GamePlayColors.Yellow,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Returns true if [pos] lies on the straight-line path between [source] and [target]
 * (excluding the source and target tiles themselves).
 * Uses linear interpolation over grid coordinates to determine intermediate tiles.
 */
internal fun isOnArrowLinePath(
    source: Position,
    target: Position,
    pos: Position,
): Boolean {
    val dx = target.x - source.x
    val dy = target.y - source.y
    val steps = maxOf(abs(dx), abs(dy))
    if (steps <= 1) return false
    for (step in 1 until steps) {
        val t = step.toFloat() / steps
        val ix = (source.x + dx * t).roundToInt()
        val iy = (source.y + dy * t).roundToInt()
        if (ix == pos.x && iy == pos.y) return true
    }
    return false
}
