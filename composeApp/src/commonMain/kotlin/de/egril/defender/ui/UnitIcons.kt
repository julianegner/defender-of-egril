package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.egril.defender.model.*
import de.egril.defender.ui.gameplay.GamePlayColors
import de.egril.defender.ui.icon.LightningIcon
import de.egril.defender.ui.icon.TimerIcon
import de.egril.defender.ui.icon.defender.*
import de.egril.defender.ui.icon.enemy.EnemyIcon
import de.egril.defender.ui.icon.enemy.EnemyTypeIcon

/**
 * Composable that draws a tower type icon without defender-specific info.
 * 
 * Use this for displaying tower types in selection buttons and menus where you don't
 * have a specific Defender instance. This is more efficient than creating dummy defenders.
 * 
 * Excludes defender-specific information such as:
 * - Level indicator
 * - Actions remaining (lightning bolts)
 * - Build time remaining
 * 
 * For displaying actual placed towers on the game board, use [TowerIcon] instead.
 */
@Composable
fun TowerTypeIcon(
    defenderType: DefenderType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw tower graphics with default white lines (suitable for game UI with tile backgrounds)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)

            drawTower(defenderType, centerX, centerY, iconSize)
        }
    }
}

fun DrawScope.drawTower(
    defenderType: DefenderType,
    centerX: Float,
    centerY: Float,
    iconSize: Float,
    lineColor: Color = Color.White
) {
    // Draw tower base (trapezoid shape) - except for dragon's lair and dwarven mine
    if (defenderType != DefenderType.DRAGONS_LAIR && defenderType != DefenderType.DWARVEN_MINE) {
        drawTowerBase(centerX, centerY, iconSize * 0.8f, lineColor)
    }

    // Draw tower type symbol inside
    when (defenderType) {
        DefenderType.SPIKE_TOWER -> drawSpikeSymbol(centerX, centerY, iconSize * 0.4f, lineColor)
        DefenderType.SPEAR_TOWER -> drawSpearSymbol(centerX, centerY + iconSize * 0.15f, iconSize * 0.5f, lineColor)  // Move down 30% for better positioning
        DefenderType.BOW_TOWER -> drawBowSymbol(centerX, centerY, iconSize * 0.45f)
        DefenderType.WIZARD_TOWER -> drawWizardSymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.ALCHEMY_TOWER -> drawAlchemySymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.BALLISTA_TOWER -> drawBallistaSymbol(centerX, centerY, iconSize * 0.5f)
        DefenderType.DWARVEN_MINE -> drawMineSymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.DRAGONS_LAIR -> drawDragonLairSymbol(centerX, centerY, iconSize * 0.6f)
    }
}

/**
 * Composable that draws a tower icon with a symbol for the tower type
 */
@Composable
fun TowerIcon(
    defender: Defender,
    modifier: Modifier = Modifier,
    gameState: GameState? = null
) {
    // Check if tower is on a tower base
    val towerBase = gameState?.barricades?.find { it.id == defender.towerBaseBarricadeId.value }
    val isOnTowerBase = towerBase != null
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw tower graphics first (will be behind text)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            // Adjust center Y position if on tower base
            // Move tower down slightly so top is still visible, but base platform is visible
            val adjustedCenterY = if (isOnTowerBase) {
                centerY - iconSize * 0.05f  // Move tower up only 5% (reduced from 15%)
            } else {
                centerY
            }
            
            // Draw raft base OR tower base (not both)
            if (defender.raftId.value != null) {
                // If on a raft, draw only the raft base
                drawRaftBase(centerX, adjustedCenterY, iconSize * 0.9f)
            } else if (defender.type != DefenderType.DRAGONS_LAIR && defender.type != DefenderType.DWARVEN_MINE) {
                // If not on a raft, draw tower base (trapezoid shape) - except for dragon's lair and dwarven mine
                drawTowerBase(centerX, adjustedCenterY, iconSize * 0.8f)
            }

            // Draw tower type symbol inside
            drawDefenderSymbol(
                defender.type, centerX, adjustedCenterY, iconSize,
                dragonAlive = if (defender.type == DefenderType.DRAGONS_LAIR) {
                    // Check if the specific dragon from this lair is still alive
                    defender.dragonId.value?.let { dragonId ->
                        gameState?.attackers?.any {
                            it.id == dragonId && !it.isDefeated.value
                        } ?: false
                    } ?: true
                } else {
                    true
                }
            )
            // Draw tower base wood platform if on tower base
            if (isOnTowerBase) {
                drawTowerBasePlatform(centerX, centerY, iconSize * 0.9f)
            }
        }
        
        // Actions indicator at center left (lightning bolts for remaining actions)
        // Add semi-transparent blue background for better visibility on path tiles
        if (defender.isReady && defender.actionsRemaining.value > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)  // 6dp from left edge
            ) {
                // Semi-transparent blue background for better visibility
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = if (defender.isOnTowerBase) {
                                GamePlayColors.Info.copy(alpha = 0.6f)  // More visible on path tiles
                            } else {
                                Color.Transparent  // No background on build tiles
                            },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (defender.actionsRemaining.value == 1) {
                        LightningIcon(
                            size = 16.dp
                        )
                    } else {
                        Text(
                            text = defender.actionsRemaining.value.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 16.sp,
                            color = Color.Yellow,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
        
        // Tower base HP indicator at bottom center (if on tower base)
        // Position higher up so it's visible above the brown wood platform
        if (towerBase != null) {
            Text(
                text = "${towerBase.healthPoints.value} HP",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)  // Moved up from 2dp to 18dp for visibility
            )
        } else {
            // Level indicator at bottom center - 10dp from bottom edge (only if not on tower base)
            Text(
                text = "L${defender.level.value}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)  // 10dp from bottom as requested
            )
        }
        
        // Build time indicator at bottom center (only if not ready)
        if (!defender.isReady) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerIcon(
                    size = 10.dp
                )
                Text(
                    text = defender.buildTimeRemaining.value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = Color(0xFFFFA500),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Draws the type-specific symbol for a defender type inside a Canvas.
 * Extracted from [TowerIcon] so it can be reused (e.g. on rafts).
 *
 * @param dragonAlive Only relevant for [DefenderType.DRAGONS_LAIR]; defaults to true.
 */
fun DrawScope.drawDefenderSymbol(
    defenderType: DefenderType,
    centerX: Float,
    centerY: Float,
    iconSize: Float,
    dragonAlive: Boolean = true
) {
    when (defenderType) {
        DefenderType.SPIKE_TOWER -> drawSpikeSymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.SPEAR_TOWER -> drawSpearSymbol(centerX, centerY + iconSize * 0.15f, iconSize * 0.5f)  // Move down for better positioning
        DefenderType.BOW_TOWER -> drawBowSymbol(centerX, centerY, iconSize * 0.45f)
        DefenderType.WIZARD_TOWER -> drawWizardSymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.ALCHEMY_TOWER -> drawAlchemySymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.BALLISTA_TOWER -> drawBallistaSymbol(centerX, centerY, iconSize * 0.5f)
        DefenderType.DWARVEN_MINE -> drawMineSymbol(centerX, centerY, iconSize * 0.4f)
        DefenderType.DRAGONS_LAIR -> drawDragonLairSymbol(centerX, centerY, iconSize * 0.6f, dragonAlive)
    }
}

/**
 * Composable that draws a tower on a raft: the raft base shape with the type-specific
 * defender symbol on top.  Used to represent barge-mounted towers.
 */
@Composable
fun DrawRaft(
    defenderType: DefenderType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val iconSize = minOf(size.width, size.height)
        drawRaftBase(centerX, centerY, iconSize * 0.9f)
        drawDefenderSymbol(defenderType, centerX, centerY, iconSize)
    }
}

/**
 * Draw the base tower structure (trapezoid)
 */
internal fun DrawScope.drawTowerBase(centerX: Float, centerY: Float, size: Float, lineColor: Color = Color.White) {
    // Use provided lineColor for tower bases to ensure visibility in both dark and light modes
    val baseColor = lineColor
    
    val path = Path().apply {
        val topWidth = size * 0.4f
        val bottomWidth = size * 0.6f
        val height = size * 0.6f
        val top = centerY - height / 2
        val bottom = centerY + height / 2
        
        // Trapezoid (wider at bottom)
        moveTo(centerX - bottomWidth / 2, bottom)
        lineTo(centerX + bottomWidth / 2, bottom)
        lineTo(centerX + topWidth / 2, top)
        lineTo(centerX - topWidth / 2, top)
        close()
    }
    
    // Fill tower base with appropriate color
    drawPath(path, baseColor.copy(alpha = 0.3f))
    // Draw tower outline
    drawPath(path, baseColor, style = Stroke(width = 2f))
    
    // Add battlements on top
    val battlement = size * 0.08f
    val topWidth = size * 0.4f
    val top = centerY - size * 0.6f / 2
    for (i in 0..2) {
        val x = centerX - topWidth / 2 + (topWidth / 3) * i
        drawRect(
            color = baseColor,
            topLeft = Offset(x, top - battlement),
            size = Size(battlement, battlement),
            style = Stroke(width = 1.5f)
        )
    }
}

/**
 * Draw a raft base in the shape \__/ beneath towers on rafts
 */
/**
 * Draw a wooden platform at the bottom for towers on tower bases (barricades with 100+ HP)
 */
private fun DrawScope.drawTowerBasePlatform(centerX: Float, centerY: Float, size: Float) {
    // Brown color for wooden platform
    val woodColor = Color(0xFF8B4513)  // Saddle brown color for wood
    
    // Draw thick brown line at the bottom representing wood platform
    // Move up by 1/6 of tile height as requested
    val platformWidth = size * 0.7f
    val platformHeight = size * 0.12f  // Thicker than normal
    val platformBottom = centerY + size * 0.28f  // Position moved up from 0.45 (moved up by ~1/6)
    
    // Draw filled rectangle for wood platform
    drawRect(
        color = woodColor,
        topLeft = Offset(centerX - platformWidth / 2, platformBottom - platformHeight),
        size = Size(platformWidth, platformHeight)
    )
    
    // Draw darker outline for definition
    drawRect(
        color = Color(0xFF654321),  // Dark brown for outline
        topLeft = Offset(centerX - platformWidth / 2, platformBottom - platformHeight),
        size = Size(platformWidth, platformHeight),
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawRaftBase(centerX: Float, centerY: Float, size: Float) {
    // Brown color for wooden raft
    val raftColor = Color(0xFF8B7355)  // Brown/tan color for wood
    
    val path = Path().apply {
        val width = size * 0.7f
        val height = size * 0.3f
        val bottom = centerY + size * 0.25f  // Bottom of raft
        val top = bottom - height  // Top of raft (higher up)
        
        // Draw the raft shape: \___/ (opening upward)
        // Start at top left
        moveTo(centerX - width / 2, top)
        // Left side slant down to bottom left
        lineTo(centerX - width * 0.35f, bottom)
        // Flat bottom
        lineTo(centerX + width * 0.35f, bottom)
        // Right side slant up to top right
        lineTo(centerX + width / 2, top)
        // Close the path back to start
        close()
    }
    
    // Fill the raft with brown color
    drawPath(path, raftColor)
    // Draw outline for definition
    drawPath(path, Color(0xFF5D4E37), style = Stroke(width = 2f))  // Darker brown for outline
}




