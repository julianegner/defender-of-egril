package de.egril.defender.ui.gameplay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import de.egril.defender.model.AttackType
import de.egril.defender.model.GameState
import de.egril.defender.model.Position
import de.egril.defender.ui.hexagon.HexagonShape
import de.egril.defender.ui.settings.AppSettings
import kotlin.math.sqrt

@Composable
internal fun BoxScope.TileFrameOverlays(
    position: Position,
    gameState: GameState,
    isSpawnPoint: Boolean,
    isTarget: Boolean,
    isRiverTile: Boolean,
    targetCircleInfo: TargetCircleInfo?,
    useDashedBorder: Boolean,
    borderColor: Color,
    borderWidth: Dp,
    hexSize: Dp,
    showDiagonalStripes: Boolean,
) {
    // Draw target circles AFTER other content so they appear on top
    // Inner circles on central target tile, outer ring segments on neighbor tiles
    targetCircleInfo?.let { info ->
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .zIndex(11f),
        ) {
            when (info) {
                is TargetCircleInfo.CentralTarget -> {
                    // Draw 3 inner circles on the central target tile
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    val center = Offset(centerX, centerY)

                    // Filled inner circle
                    drawCircle(
                        color = info.color,
                        radius = TargetCircleConstants.INNER_CIRCLE_1_RADIUS,
                        center = center,
                    )

                    // Two stroke circles
                    drawCircle(
                        color = info.color,
                        radius = TargetCircleConstants.INNER_CIRCLE_2_RADIUS,
                        center = center,
                        style =
                            Stroke(
                                width = TargetCircleConstants.INNER_CIRCLE_STROKE_WIDTH,
                            ),
                    )

                    drawCircle(
                        color = info.color,
                        radius = TargetCircleConstants.INNER_CIRCLE_3_RADIUS,
                        center = center,
                        style =
                            Stroke(
                                width = TargetCircleConstants.INNER_CIRCLE_STROKE_WIDTH,
                            ),
                    )
                }

                is TargetCircleInfo.NeighborTarget -> {
                    // Draw outer ring segments on neighbor tiles (only for AREA and LASTING)
                    if (info.attackType == AttackType.AREA || info.attackType == AttackType.LASTING) {
                        // Use different radii based on distance from center
                        // Distance 2 (extended area for level 20+) uses larger radii
                        val radius1 =
                            if (info.distanceFromCenter >= 2) {
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_1_RADIUS
                            } else {
                                TargetCircleConstants.OUTER_CIRCLE_1_RADIUS
                            }
                        val radius2 =
                            if (info.distanceFromCenter >= 2) {
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_2_RADIUS
                            } else {
                                TargetCircleConstants.OUTER_CIRCLE_2_RADIUS
                            }
                        val radius3 =
                            if (info.distanceFromCenter >= 2) {
                                TargetCircleConstants.EXTENDED_OUTER_CIRCLE_3_RADIUS
                            } else {
                                TargetCircleConstants.OUTER_CIRCLE_3_RADIUS
                            }

                        // Draw 3 concentric arc segments
                        CircularSegmentDrawer.drawArcSegment(
                            drawScope = this,
                            color = info.color,
                            radius = radius1,
                            strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                            centerPos = info.centerPosition,
                            neighborPos = info.thisPosition,
                            hexSize = hexSize.value,
                        )

                        CircularSegmentDrawer.drawArcSegment(
                            drawScope = this,
                            color = info.color,
                            radius = radius2,
                            strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                            centerPos = info.centerPosition,
                            neighborPos = info.thisPosition,
                            hexSize = hexSize.value,
                        )

                        CircularSegmentDrawer.drawArcSegment(
                            drawScope = this,
                            color = info.color,
                            radius = radius3,
                            strokeWidth = TargetCircleConstants.OUTER_CIRCLE_STROKE_WIDTH,
                            centerPos = info.centerPosition,
                            neighborPos = info.thisPosition,
                            hexSize = hexSize.value,
                        )
                    }
                }
            }
        }
    }

    // Draw dashed border for tower placement preview
    if (useDashedBorder) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .zIndex(12f),
        ) {
            val sqrt3 = sqrt(3.0).toFloat()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f

            // Create hexagon path
            val path =
                Path().apply {
                    // Top point
                    moveTo(centerX, centerY - radius)
                    // Top-right
                    lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
                    // Bottom-right
                    lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
                    // Bottom point
                    lineTo(centerX, centerY + radius)
                    // Bottom-left
                    lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
                    // Top-left
                    lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
                    // Close the path
                    close()
                }

            // Draw dashed border
            drawPath(
                path = path,
                color = borderColor,
                style =
                    Stroke(
                        width = borderWidth.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(
                                intervals = floatArrayOf(10f, 5f), // 10px dash, 5px gap
                                phase = 0f,
                            ),
                    ),
            )
        }
    }

    // Draw diagonal stripes for buildable tiles, tower bases, and placement tiles (trap/barricade/magical trap)
    if (showDiagonalStripes) {
        Canvas(
            modifier =
                Modifier
                    .matchParentSize()
                    .zIndex(11f), // Below the dashed border
        ) {
            val sqrt3 = sqrt(3.0).toFloat()
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f

            // Create hexagon clip path
            val hexPath =
                Path().apply {
                    moveTo(centerX, centerY - radius)
                    lineTo(centerX + radius * sqrt3 / 2f, centerY - radius / 2f)
                    lineTo(centerX + radius * sqrt3 / 2f, centerY + radius / 2f)
                    lineTo(centerX, centerY + radius)
                    lineTo(centerX - radius * sqrt3 / 2f, centerY + radius / 2f)
                    lineTo(centerX - radius * sqrt3 / 2f, centerY - radius / 2f)
                    close()
                }

            // Draw diagonal stripes with clipping
            drawContext.canvas.save()
            drawContext.canvas.clipPath(hexPath)

            // Draw diagonal stripes
            val stripeWidth = 8f
            val stripeSpacing = 16f
            val totalSpacing = stripeWidth + stripeSpacing
            val diagonalLength = size.width + size.height

            // Start from top-right, go to bottom-left (90 degree rotation)
            var offset = -diagonalLength
            while (offset < diagonalLength) {
                drawLine(
                    color = borderColor.copy(alpha = 0.8f), // 80% opacity
                    start = Offset(size.width - offset, 0f),
                    end = Offset(size.width - offset - size.height, size.height),
                    strokeWidth = stripeWidth,
                )
                offset += totalSpacing
            }

            drawContext.canvas.restore()
        }
    }

    // Debug overlay: tile borders by type
    if (AppSettings.showTileBorders.value) {
        val debugBorderColor =
            when {
                isSpawnPoint -> Color(0xFFFF4400)
                isTarget -> Color(0xFF00DD00)
                isRiverTile -> Color(0xFF0066FF)
                gameState.level.isOnPath(position) -> Color(0xFFFFAA00)
                gameState.level.isBuildArea(position) -> Color(0xFF44BB44)
                else -> Color(0xFF888888)
            }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .border(2.dp, debugBorderColor, HexagonShape()),
        )
    }

    // Debug overlay: tile position text
    if (AppSettings.showTilePositions.value) {
        Box(
            modifier =
                Modifier
                    .background(Color.White.copy(alpha = 0.8f))
                    .padding(1.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${position.x},${position.y}",
                fontSize = 8.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
