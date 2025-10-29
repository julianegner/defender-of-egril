package com.defenderofegril.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.defenderofegril.model.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

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
        // Draw tower graphics
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            // Draw tower base (trapezoid shape) - except for dragon's lair and dwarven mine
            if (defenderType != DefenderType.DRAGONS_LAIR && defenderType != DefenderType.DWARVEN_MINE) {
                drawTowerBase(centerX, centerY, iconSize * 0.8f)
            }
            
            // Draw tower type symbol inside
            when (defenderType) {
                DefenderType.SPIKE_TOWER -> drawSpikeSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.SPEAR_TOWER -> drawSpearSymbol(centerX, centerY, iconSize * 0.5f)
                DefenderType.BOW_TOWER -> drawBowSymbol(centerX, centerY, iconSize * 0.45f)
                DefenderType.WIZARD_TOWER -> drawWizardSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.ALCHEMY_TOWER -> drawAlchemySymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.BALLISTA_TOWER -> drawBallistaSymbol(centerX, centerY, iconSize * 0.5f)
                DefenderType.DWARVEN_MINE -> drawMineSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.DRAGONS_LAIR -> drawDragonLairSymbol(centerX, centerY, iconSize * 0.6f)
            }
        }
    }
}

/**
 * Composable that draws a tower icon with a symbol for the tower type
 */
@Composable
fun TowerIcon(
    defender: Defender,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw tower graphics first (will be behind text)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            // Draw tower base (trapezoid shape) - except for dragon's lair and dwarven mine
            if (defender.type != DefenderType.DRAGONS_LAIR && defender.type != DefenderType.DWARVEN_MINE) {
                drawTowerBase(centerX, centerY, iconSize * 0.8f)
            }
            
            // Draw tower type symbol inside
            when (defender.type) {
                DefenderType.SPIKE_TOWER -> drawSpikeSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.SPEAR_TOWER -> drawSpearSymbol(centerX, centerY, iconSize * 0.5f)
                DefenderType.BOW_TOWER -> drawBowSymbol(centerX, centerY, iconSize * 0.45f)
                DefenderType.WIZARD_TOWER -> drawWizardSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.ALCHEMY_TOWER -> drawAlchemySymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.BALLISTA_TOWER -> drawBallistaSymbol(centerX, centerY, iconSize * 0.5f)
                DefenderType.DWARVEN_MINE -> drawMineSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.DRAGONS_LAIR -> drawDragonLairSymbol(centerX, centerY, iconSize * 0.6f)
            }
        }
        
        // Actions indicator at center left (lightning bolts for remaining actions)
        if (defender.isReady && defender.actionsRemaining.value > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)  // 6dp from left edge
            ) {
                Text(
                    text = if (defender.actionsRemaining.value == 1) "⚡" else defender.actionsRemaining.value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 16.sp,
                    color = Color.Yellow,
                    fontWeight = if (defender.actionsRemaining.value == 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        
        // Level indicator at bottom center - 10dp from bottom edge
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
        
        // Build time indicator at bottom center (only if not ready)
        if (!defender.isReady) {
            Text(
                text = "⏱${defender.buildTimeRemaining.value}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color(0xFFFFA500),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            )
        }
    }
}

/**
 * Draw the base tower structure (trapezoid)
 */
private fun DrawScope.drawTowerBase(centerX: Float, centerY: Float, size: Float) {
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
    
    // Fill tower base
    drawPath(path, Color.White.copy(alpha = 0.3f))
    // Draw tower outline
    drawPath(path, Color.White, style = Stroke(width = 2f))
    
    // Add battlements on top
    val battlement = size * 0.08f
    val topWidth = size * 0.4f
    val top = centerY - size * 0.6f / 2
    for (i in 0..2) {
        val x = centerX - topWidth / 2 + (topWidth / 3) * i
        drawRect(
            color = Color.White,
            topLeft = Offset(x, top - battlement),
            size = Size(battlement, battlement),
            style = Stroke(width = 1.5f)
        )
    }
}

/**
 * Draw spike symbol (upward pointing spikes)
 */
private fun DrawScope.drawSpikeSymbol(centerX: Float, centerY: Float, size: Float) {
    val spikeCount = 3
    val spikeWidth = size / 4
    val spikeHeight = size * 0.8f
    
    for (i in 0 until spikeCount) {
        val x = centerX - size / 2 + (size / spikeCount) * i + spikeWidth / 2
        val path = Path().apply {
            moveTo(x - spikeWidth / 2, centerY + spikeHeight / 3)
            lineTo(x, centerY - spikeHeight / 3)
            lineTo(x + spikeWidth / 2, centerY + spikeHeight / 3)
            close()
        }
        drawPath(path, Color.Yellow)
        drawPath(path, Color.White, style = Stroke(width = 1.5f))
    }
}

/**
 * Draw spear symbol (vertical spear)
 */
private fun DrawScope.drawSpearSymbol(centerX: Float, centerY: Float, size: Float) {
    val shaftWidth = size * 0.1f
    val shaftHeight = size * 0.7f
    val spearheadHeight = size * 0.3f
    val spearheadWidth = size * 0.3f
    
    // Shaft
    drawRect(
        color = Color(0xFFD2691E), // Brown
        topLeft = Offset(centerX - shaftWidth / 2, centerY - shaftHeight / 2),
        size = Size(shaftWidth, shaftHeight)
    )
    
    // Spearhead
    val path = Path().apply {
        moveTo(centerX, centerY - shaftHeight / 2 - spearheadHeight)
        lineTo(centerX - spearheadWidth / 2, centerY - shaftHeight / 2)
        lineTo(centerX + spearheadWidth / 2, centerY - shaftHeight / 2)
        close()
    }
    drawPath(path, Color(0xFFC0C0C0)) // Silver
    drawPath(path, Color.White, style = Stroke(width = 1.5f))
}

/**
 * Draw bow symbol (curved bow with string)
 */
private fun DrawScope.drawBowSymbol(centerX: Float, centerY: Float, size: Float) {
    val bowHeight = size * 0.8f
    val bowWidth = size * 0.5f
    
    // Bow arc (spanned bow with pronounced curve)
    val path = Path().apply {
        moveTo(centerX + bowWidth / 2, centerY - bowHeight / 2)
        cubicTo(
            centerX - bowWidth * 0.1f, centerY - bowHeight / 4,
            centerX - bowWidth * 0.1f, centerY + bowHeight / 4,
            centerX + bowWidth / 2, centerY + bowHeight / 2
        )
    }
    drawPath(path, Color(0xFFD2691E), style = Stroke(width = 3f, cap = StrokeCap.Round))
    
    // Bow string
    drawLine(
        color = Color(0xFFFFFFDD),
        start = Offset(centerX + bowWidth / 2, centerY - bowHeight / 2),
        end = Offset(centerX + bowWidth / 2, centerY + bowHeight / 2),
        strokeWidth = 1.5f
    )
    
    // Arrow (extends to bow string on the right)
    drawLine(
        color = Color(0xFFC0C0C0),
        start = Offset(centerX - bowWidth / 3, centerY),
        end = Offset(centerX + bowWidth / 2, centerY),
        strokeWidth = 2f
    )
    // Arrowhead (pointing left)
    val arrowPath = Path().apply {
        moveTo(centerX - bowWidth / 3, centerY)
        lineTo(centerX - bowWidth / 4, centerY - size * 0.1f)
        lineTo(centerX - bowWidth / 4, centerY + size * 0.1f)
        close()
    }
    drawPath(arrowPath, Color(0xFFC0C0C0))
}

/**
 * Draw wizard symbol (star for magic)
 */
private fun DrawScope.drawWizardSymbol(centerX: Float, centerY: Float, size: Float) {
    val outerRadius = size * 0.5f
    val innerRadius = size * 0.2f
    val points = 5
    
    val path = Path()
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = PI / points * i - PI / 2
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    
    drawPath(path, Color(0xFFFFD700)) // Gold
    drawPath(path, Color(0xFFFF6B00), style = Stroke(width = 2f)) // Orange outline
    
    // Magic sparkle
    drawCircle(
        color = Color(0xFFFFFFFF),
        radius = size * 0.08f,
        center = Offset(centerX, centerY)
    )
}

/**
 * Draw alchemy symbol (potion flask)
 */
private fun DrawScope.drawAlchemySymbol(centerX: Float, centerY: Float, size: Float) {
    // Flask body (trapezoid)
    val bodyPath = Path().apply {
        val topWidth = size * 0.3f
        val bottomWidth = size * 0.5f
        val bodyHeight = size * 0.5f
        val top = centerY - size * 0.1f
        val bottom = centerY + bodyHeight
        
        moveTo(centerX - bottomWidth / 2, bottom)
        lineTo(centerX + bottomWidth / 2, bottom)
        lineTo(centerX + topWidth / 2, top)
        lineTo(centerX - topWidth / 2, top)
        close()
    }
    drawPath(bodyPath, Color(0xFF00FF00).copy(alpha = 0.5f)) // Green transparent
    drawPath(bodyPath, Color(0xFF00AA00), style = Stroke(width = 2f))
    
    // Flask neck
    val neckWidth = size * 0.2f
    val neckHeight = size * 0.3f
    drawRect(
        color = Color(0xFFAAAAAA).copy(alpha = 0.3f),
        topLeft = Offset(centerX - neckWidth / 2, centerY - size * 0.1f - neckHeight),
        size = Size(neckWidth, neckHeight),
        style = Stroke(width = 1.5f)
    )
    
    // Bubbles
    drawCircle(
        color = Color(0xFF90EE90),
        radius = size * 0.08f,
        center = Offset(centerX - size * 0.15f, centerY + size * 0.2f)
    )
    drawCircle(
        color = Color(0xFF90EE90),
        radius = size * 0.06f,
        center = Offset(centerX + size * 0.1f, centerY + size * 0.15f)
    )
}

/**
 * Draw ballista symbol (crossbow-like weapon)
 */
private fun DrawScope.drawBallistaSymbol(centerX: Float, centerY: Float, size: Float) {
    // Horizontal beam
    drawRect(
        color = Color(0xFF8B4513), // Brown
        topLeft = Offset(centerX - size * 0.5f, centerY - size * 0.08f),
        size = Size(size, size * 0.16f)
    )
    
    // Vertical support
    drawRect(
        color = Color(0xFF654321),
        topLeft = Offset(centerX - size * 0.05f, centerY - size * 0.15f),
        size = Size(size * 0.1f, size * 0.5f)
    )
    
    // Bowstring
    drawLine(
        color = Color(0xFFFFFFDD),
        start = Offset(centerX - size * 0.45f, centerY - size * 0.15f),
        end = Offset(centerX - size * 0.45f, centerY + size * 0.15f),
        strokeWidth = 2f
    )
    
    // Bolt
    drawLine(
        color = Color(0xFF696969),
        start = Offset(centerX - size * 0.3f, centerY),
        end = Offset(centerX + size * 0.4f, centerY),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    
    // Bolt head
    val boltPath = Path().apply {
        moveTo(centerX + size * 0.4f, centerY)
        lineTo(centerX + size * 0.3f, centerY - size * 0.08f)
        lineTo(centerX + size * 0.3f, centerY + size * 0.08f)
        close()
    }
    drawPath(boltPath, Color(0xFF696969))
}

/**
 * Composable that draws an enemy unit icon
 */
@Composable
fun EnemyIcon(
    attacker: Attacker,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw enemy graphics first (will be behind text)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            when (attacker.type) {
                AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, iconSize * 0.75f)
                AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.WITCH -> drawWitchSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, iconSize * 0.75f)
                AttackerType.EVIL_MAGE -> drawEvilMageSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, iconSize * 0.8f)
                AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, iconSize * 0.9f)
            }
        }
        
        // Health number at bottom center - 10dp from bottom edge
        Text(
            text = "${attacker.currentHealth.value}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)  // 10dp from bottom as requested
        )
    }
}

/**
 * Composable that draws an enemy type icon (for planned spawns)
 */
@Composable
fun EnemyTypeIcon(
    attackerType: AttackerType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Draw enemy graphics
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val iconSize = minOf(size.width, size.height)
            
            when (attackerType) {
                AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.OGRE -> drawOgreSymbol(centerX, centerY, iconSize * 0.75f)
                AttackerType.SKELETON -> drawSkeletonSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.WITCH -> drawWitchSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.BLUE_DEMON -> drawBlueDemonSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.RED_DEMON -> drawRedDemonSymbol(centerX, centerY, iconSize * 0.75f)
                AttackerType.EVIL_MAGE -> drawEvilMageSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.RED_WITCH -> drawRedWitchSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.GREEN_WITCH -> drawGreenWitchSymbol(centerX, centerY, iconSize * 0.7f)
                AttackerType.EWHAD -> drawEwhadSymbol(centerX, centerY, iconSize * 0.8f)
                AttackerType.DRAGON -> drawDragonSymbol(centerX, centerY, iconSize * 0.9f)
            }
        }
    }
}

/**
 * Draw goblin symbol (small creature with pointy ears)
 */
private fun DrawScope.drawGoblinSymbol(centerX: Float, centerY: Float, size: Float) {
    // Head (circle)
    drawCircle(
        color = Color(0xFF90EE90), // Light green
        radius = size * 0.3f,
        center = Offset(centerX, centerY - size * 0.1f)
    )
    
    // Pointy ears
    val earPath1 = Path().apply {
        moveTo(centerX - size * 0.3f, centerY - size * 0.1f)
        lineTo(centerX - size * 0.45f, centerY - size * 0.25f)
        lineTo(centerX - size * 0.25f, centerY - size * 0.2f)
        close()
    }
    val earPath2 = Path().apply {
        moveTo(centerX + size * 0.3f, centerY - size * 0.1f)
        lineTo(centerX + size * 0.45f, centerY - size * 0.25f)
        lineTo(centerX + size * 0.25f, centerY - size * 0.2f)
        close()
    }
    drawPath(earPath1, Color(0xFF90EE90))
    drawPath(earPath2, Color(0xFF90EE90))
    
    // Eyes
    drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY - size * 0.15f))
    drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY - size * 0.15f))
    
    // Body (small)
    drawRect(
        color = Color(0xFF8B4513), // Brown
        topLeft = Offset(centerX - size * 0.15f, centerY + size * 0.15f),
        size = Size(size * 0.3f, size * 0.25f)
    )
}

/**
 * Draw ork symbol (larger, muscular creature)
 */
private fun DrawScope.drawOrkSymbol(centerX: Float, centerY: Float, size: Float) {
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

/**
 * Draw ogre symbol (very large creature)
 */
private fun DrawScope.drawOgreSymbol(centerX: Float, centerY: Float, size: Float) {
    // Huge head
    drawCircle(
        color = Color(0xFFA0522D), // Sienna/brown
        radius = size * 0.35f,
        center = Offset(centerX, centerY - size * 0.05f)
    )
    
    // Big eyes
    drawCircle(color = Color.White, radius = size * 0.08f, center = Offset(centerX - size * 0.15f, centerY - size * 0.1f))
    drawCircle(color = Color.White, radius = size * 0.08f, center = Offset(centerX + size * 0.15f, centerY - size * 0.1f))
    drawCircle(color = Color.Black, radius = size * 0.04f, center = Offset(centerX - size * 0.15f, centerY - size * 0.1f))
    drawCircle(color = Color.Black, radius = size * 0.04f, center = Offset(centerX + size * 0.15f, centerY - size * 0.1f))
    
    // Mouth
    drawLine(
        color = Color.Black,
        start = Offset(centerX - size * 0.15f, centerY + size * 0.1f),
        end = Offset(centerX + size * 0.15f, centerY + size * 0.1f),
        strokeWidth = 3f
    )
    
    // Massive body
    drawRect(
        color = Color(0xFF8B7355), // Burlywood
        topLeft = Offset(centerX - size * 0.3f, centerY + size * 0.25f),
        size = Size(size * 0.6f, size * 0.2f)
    )
}

/**
 * Draw skeleton symbol (skull and bones)
 */
private fun DrawScope.drawSkeletonSymbol(centerX: Float, centerY: Float, size: Float) {
    // Skull
    drawCircle(
        color = Color.White,
        radius = size * 0.25f,
        center = Offset(centerX, centerY - size * 0.1f)
    )
    
    // Eye sockets (black)
    drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX - size * 0.12f, centerY - size * 0.15f))
    drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX + size * 0.12f, centerY - size * 0.15f))
    
    // Nose hole (triangle)
    val nosePath = Path().apply {
        moveTo(centerX, centerY - size * 0.05f)
        lineTo(centerX - size * 0.05f, centerY + size * 0.05f)
        lineTo(centerX + size * 0.05f, centerY + size * 0.05f)
        close()
    }
    drawPath(nosePath, Color.Black)
    
    // Crossbones
    drawLine(
        color = Color.White,
        start = Offset(centerX - size * 0.3f, centerY + size * 0.25f),
        end = Offset(centerX + size * 0.3f, centerY + size * 0.35f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White,
        start = Offset(centerX - size * 0.3f, centerY + size * 0.35f),
        end = Offset(centerX + size * 0.3f, centerY + size * 0.25f),
        strokeWidth = 4f,
        cap = StrokeCap.Round
    )
}

/**
 * Draw evil wizard symbol (pointed hat with mystical energy)
 */
private fun DrawScope.drawEvilWizardSymbol(centerX: Float, centerY: Float, size: Float) {
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

/**
 * Draw witch symbol (pointed hat with broom)
 */
private fun DrawScope.drawWitchSymbol(centerX: Float, centerY: Float, size: Float) {
    // Witch hat
    val hatPath = Path().apply {
        moveTo(centerX, centerY - size * 0.35f)
        lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
        lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
        close()
    }
    drawPath(hatPath, Color.Black)
    
    // Hat brim
    drawRect(
        color = Color.Black,
        topLeft = Offset(centerX - size * 0.3f, centerY - size * 0.05f),
        size = Size(size * 0.6f, size * 0.06f)
    )
    
    // Face
    drawCircle(
        color = Color(0xFF90EE90), // Greenish
        radius = size * 0.18f,
        center = Offset(centerX, centerY + size * 0.1f)
    )
    
    // Eyes
    drawCircle(color = Color(0xFFFF4500), radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY + size * 0.08f))
    drawCircle(color = Color(0xFFFF4500), radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY + size * 0.08f))
    
    // Broom
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX - size * 0.3f, centerY + size * 0.05f),
        end = Offset(centerX - size * 0.42f, centerY + size * 0.35f),
        strokeWidth = 2f
    )
    // Broom bristles
    for (i in 0..3) {
        drawLine(
            color = Color(0xFFDAA520),
            start = Offset(centerX - size * 0.42f, centerY + size * 0.3f),
            end = Offset(centerX - size * 0.48f + i * size * 0.03f, centerY + size * 0.42f),
            strokeWidth = 1.5f
        )
    }
}

/**
 * Draw blue demon symbol (fast demon with blue flames)
 */
private fun DrawScope.drawBlueDemonSymbol(centerX: Float, centerY: Float, size: Float) {
    // Demon head (angular)
    val headPath = Path().apply {
        moveTo(centerX, centerY - size * 0.3f)
        lineTo(centerX + size * 0.2f, centerY - size * 0.1f)
        lineTo(centerX + size * 0.15f, centerY + size * 0.1f)
        lineTo(centerX, centerY + size * 0.2f)
        lineTo(centerX - size * 0.15f, centerY + size * 0.1f)
        lineTo(centerX - size * 0.2f, centerY - size * 0.1f)
        close()
    }
    drawPath(headPath, Color(0xFF0080FF)) // Bright blue
    
    // Horns
    val hornPath1 = Path().apply {
        moveTo(centerX - size * 0.15f, centerY - size * 0.25f)
        lineTo(centerX - size * 0.25f, centerY - size * 0.4f)
        lineTo(centerX - size * 0.1f, centerY - size * 0.3f)
        close()
    }
    val hornPath2 = Path().apply {
        moveTo(centerX + size * 0.15f, centerY - size * 0.25f)
        lineTo(centerX + size * 0.25f, centerY - size * 0.4f)
        lineTo(centerX + size * 0.1f, centerY - size * 0.3f)
        close()
    }
    drawPath(hornPath1, Color(0xFF004080)) // Dark blue
    drawPath(hornPath2, Color(0xFF004080))
    
    // Glowing eyes
    drawCircle(color = Color.Cyan, radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY - size * 0.1f))
    drawCircle(color = Color.Cyan, radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY - size * 0.1f))
    
    // Blue flame aura (wings)
    val flamePath = Path().apply {
        moveTo(centerX - size * 0.15f, centerY + size * 0.1f)
        cubicTo(
            centerX - size * 0.3f, centerY,
            centerX - size * 0.35f, centerY + size * 0.2f,
            centerX - size * 0.2f, centerY + size * 0.3f
        )
    }
    drawPath(flamePath, Color(0xFF40A0FF), style = Stroke(width = 2f))
    val flamePath2 = Path().apply {
        moveTo(centerX + size * 0.15f, centerY + size * 0.1f)
        cubicTo(
            centerX + size * 0.3f, centerY,
            centerX + size * 0.35f, centerY + size * 0.2f,
            centerX + size * 0.2f, centerY + size * 0.3f
        )
    }
    drawPath(flamePath2, Color(0xFF40A0FF), style = Stroke(width = 2f))
}

/**
 * Draw red demon symbol (slow but tanky with red armor)
 */
private fun DrawScope.drawRedDemonSymbol(centerX: Float, centerY: Float, size: Float) {
    // Large armored body
    drawCircle(
        color = Color(0xFF8B0000), // Dark red
        radius = size * 0.35f,
        center = Offset(centerX, centerY)
    )
    
    // Armor plates
    drawRect(
        color = Color(0xFF4A0000),
        topLeft = Offset(centerX - size * 0.25f, centerY - size * 0.1f),
        size = Size(size * 0.5f, size * 0.2f)
    )
    
    // Large horns
    val hornPath1 = Path().apply {
        moveTo(centerX - size * 0.25f, centerY - size * 0.2f)
        lineTo(centerX - size * 0.4f, centerY - size * 0.45f)
        lineTo(centerX - size * 0.15f, centerY - size * 0.25f)
        close()
    }
    val hornPath2 = Path().apply {
        moveTo(centerX + size * 0.25f, centerY - size * 0.2f)
        lineTo(centerX + size * 0.4f, centerY - size * 0.45f)
        lineTo(centerX + size * 0.15f, centerY - size * 0.25f)
        close()
    }
    drawPath(hornPath1, Color(0xFF2A0000))
    drawPath(hornPath2, Color(0xFF2A0000))
    
    // Glowing eyes
    drawCircle(color = Color(0xFFFF4500), radius = size * 0.06f, center = Offset(centerX - size * 0.12f, centerY - size * 0.08f))
    drawCircle(color = Color(0xFFFF4500), radius = size * 0.06f, center = Offset(centerX + size * 0.12f, centerY - size * 0.08f))
}

/**
 * Draw evil mage symbol (similar to wizard but more menacing)
 */
private fun DrawScope.drawEvilMageSymbol(centerX: Float, centerY: Float, size: Float) {
    // Robe (dark)
    val robePath = Path().apply {
        moveTo(centerX, centerY - size * 0.35f)
        lineTo(centerX - size * 0.25f, centerY + size * 0.3f)
        lineTo(centerX + size * 0.25f, centerY + size * 0.3f)
        close()
    }
    drawPath(robePath, Color(0xFF2C0052)) // Very dark purple
    
    // Hood
    val hoodPath = Path().apply {
        moveTo(centerX, centerY - size * 0.4f)
        lineTo(centerX - size * 0.3f, centerY - size * 0.05f)
        lineTo(centerX + size * 0.3f, centerY - size * 0.05f)
        close()
    }
    drawPath(hoodPath, Color(0xFF1A0030))
    
    // Face in shadow
    drawCircle(
        color = Color(0xFF4A0080),
        radius = size * 0.15f,
        center = Offset(centerX, centerY)
    )
    
    // Glowing purple eyes
    drawCircle(color = Color(0xFFB000FF), radius = size * 0.06f, center = Offset(centerX - size * 0.08f, centerY - size * 0.02f))
    drawCircle(color = Color(0xFFB000FF), radius = size * 0.06f, center = Offset(centerX + size * 0.08f, centerY - size * 0.02f))
    
    // Staff with dark orb
    drawLine(
        color = Color(0xFF1A1A1A),
        start = Offset(centerX + size * 0.3f, centerY - size * 0.1f),
        end = Offset(centerX + size * 0.4f, centerY + size * 0.4f),
        strokeWidth = 3f
    )
    drawCircle(color = Color(0xFF8B00FF), radius = size * 0.1f, center = Offset(centerX + size * 0.4f, centerY - size * 0.15f))
}

/**
 * Draw red witch symbol (witch in red, focused on tower disruption)
 */
private fun DrawScope.drawRedWitchSymbol(centerX: Float, centerY: Float, size: Float) {
    // Red witch hat
    val hatPath = Path().apply {
        moveTo(centerX, centerY - size * 0.35f)
        lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
        lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
        close()
    }
    drawPath(hatPath, Color(0xFF8B0000)) // Dark red
    
    // Hat brim
    drawRect(
        color = Color(0xFF8B0000),
        topLeft = Offset(centerX - size * 0.3f, centerY - size * 0.05f),
        size = Size(size * 0.6f, size * 0.06f)
    )
    
    // Face
    drawCircle(
        color = Color(0xFFFFE4B5), // Light skin
        radius = size * 0.18f,
        center = Offset(centerX, centerY + size * 0.1f)
    )
    
    // Eyes
    drawCircle(color = Color(0xFFDC143C), radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY + size * 0.08f))
    drawCircle(color = Color(0xFFDC143C), radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY + size * 0.08f))
    
    // Wand (instead of broom)
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX - size * 0.25f, centerY + size * 0.15f),
        end = Offset(centerX - size * 0.4f, centerY + size * 0.35f),
        strokeWidth = 2f
    )
    // Red star on wand
    drawCircle(color = Color.Red, radius = size * 0.06f, center = Offset(centerX - size * 0.4f, centerY + size * 0.35f))
}

/**
 * Draw green witch symbol (healer witch)
 */
private fun DrawScope.drawGreenWitchSymbol(centerX: Float, centerY: Float, size: Float) {
    // Green witch hat
    val hatPath = Path().apply {
        moveTo(centerX, centerY - size * 0.35f)
        lineTo(centerX - size * 0.25f, centerY - size * 0.05f)
        lineTo(centerX + size * 0.25f, centerY - size * 0.05f)
        close()
    }
    drawPath(hatPath, Color(0xFF228B22)) // Forest green
    
    // Hat brim
    drawRect(
        color = Color(0xFF228B22),
        topLeft = Offset(centerX - size * 0.3f, centerY - size * 0.05f),
        size = Size(size * 0.6f, size * 0.06f)
    )
    
    // Face
    drawCircle(
        color = Color(0xFFE0FFE0), // Very light green
        radius = size * 0.18f,
        center = Offset(centerX, centerY + size * 0.1f)
    )
    
    // Eyes
    drawCircle(color = Color(0xFF32CD32), radius = size * 0.05f, center = Offset(centerX - size * 0.08f, centerY + size * 0.08f))
    drawCircle(color = Color(0xFF32CD32), radius = size * 0.05f, center = Offset(centerX + size * 0.08f, centerY + size * 0.08f))
    
    // Healing staff
    drawLine(
        color = Color(0xFF8B4513),
        start = Offset(centerX + size * 0.25f, centerY + size * 0.15f),
        end = Offset(centerX + size * 0.4f, centerY + size * 0.35f),
        strokeWidth = 2f
    )
    // Green healing orb
    drawCircle(color = Color(0xFF00FF00), radius = size * 0.07f, center = Offset(centerX + size * 0.4f, centerY + size * 0.35f))
}

/**
 * Draw Ewhad symbol (evil arch mage boss) - unique symbol ☠ Ψ
 */
private fun DrawScope.drawEwhadSymbol(centerX: Float, centerY: Float, size: Float) {
    // Large dark robe
    val robePath = Path().apply {
        moveTo(centerX, centerY - size * 0.45f)
        lineTo(centerX - size * 0.35f, centerY + size * 0.35f)
        lineTo(centerX + size * 0.35f, centerY + size * 0.35f)
        close()
    }
    drawPath(robePath, Color(0xFF0A0015)) // Almost black with purple tint
    
    // Elaborate hood with points
    val hoodPath = Path().apply {
        moveTo(centerX, centerY - size * 0.5f)
        lineTo(centerX - size * 0.35f, centerY - size * 0.1f)
        lineTo(centerX - size * 0.3f, centerY - size * 0.15f)
        lineTo(centerX, centerY - size * 0.45f)
        lineTo(centerX + size * 0.3f, centerY - size * 0.15f)
        lineTo(centerX + size * 0.35f, centerY - size * 0.1f)
        close()
    }
    drawPath(hoodPath, Color.Black)
    
    // Skull face (death aspect)
    drawCircle(
        color = Color(0xFFD3D3D3),
        radius = size * 0.2f,
        center = Offset(centerX, centerY - size * 0.05f)
    )
    
    // Skull eye sockets (glowing red)
    drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX - size * 0.1f, centerY - size * 0.1f))
    drawCircle(color = Color.Black, radius = size * 0.08f, center = Offset(centerX + size * 0.1f, centerY - size * 0.1f))
    drawCircle(color = Color(0xFFFF0000), radius = size * 0.04f, center = Offset(centerX - size * 0.1f, centerY - size * 0.1f))
    drawCircle(color = Color(0xFFFF0000), radius = size * 0.04f, center = Offset(centerX + size * 0.1f, centerY - size * 0.1f))
    
    // Crown/spikes on hood
    for (i in -1..1) {
        val path = Path().apply {
            val x = centerX + i * size * 0.15f
            moveTo(x, centerY - size * 0.45f)
            lineTo(x - size * 0.05f, centerY - size * 0.35f)
            lineTo(x + size * 0.05f, centerY - size * 0.35f)
            close()
        }
        drawPath(path, Color(0xFFFFD700)) // Gold
    }
    
    // Powerful staff (trident-like Ψ symbol)
    drawLine(
        color = Color(0xFF3A0060),
        start = Offset(centerX + size * 0.35f, centerY - size * 0.2f),
        end = Offset(centerX + size * 0.45f, centerY + size * 0.45f),
        strokeWidth = 4f
    )
    // Trident top (Ψ shape)
    drawLine(
        color = Color(0xFF8B00FF),
        start = Offset(centerX + size * 0.35f, centerY - size * 0.25f),
        end = Offset(centerX + size * 0.45f, centerY - size * 0.35f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF8B00FF),
        start = Offset(centerX + size * 0.45f, centerY - size * 0.25f),
        end = Offset(centerX + size * 0.45f, centerY - size * 0.35f),
        strokeWidth = 3f
    )
    drawLine(
        color = Color(0xFF8B00FF),
        start = Offset(centerX + size * 0.55f, centerY - size * 0.25f),
        end = Offset(centerX + size * 0.45f, centerY - size * 0.35f),
        strokeWidth = 3f
    )
    
    // Dark energy aura
    drawCircle(
        color = Color(0xFF4B0082).copy(alpha = 0.3f),
        radius = size * 0.5f,
        center = Offset(centerX, centerY)
    )
}

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

/**
 * Draw dragon's lair symbol (cave with smoke)
 */
fun DrawScope.drawDragonLairSymbol(centerX: Float, centerY: Float, size: Float) {
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
    
    // Dragon eyes in the darkness
    drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX - size * 0.1f, centerY - size * 0.1f))
    drawCircle(color = Color.Red, radius = size * 0.05f, center = Offset(centerX + size * 0.1f, centerY - size * 0.1f))
}

/**
 * Draw dragon symbol (large flying beast)
 */
fun DrawScope.drawDragonSymbol(centerX: Float, centerY: Float, size: Float) {
    // Dragon body
    drawOval(
        color = Color(0xFF8B0000),  // Dark red
        topLeft = Offset(centerX - size * 0.2f, centerY - size * 0.1f),
        size = Size(size * 0.4f, size * 0.2f)
    )
    
    // Dragon head
    drawCircle(
        color = Color(0xFF8B0000),
        radius = size * 0.15f,
        center = Offset(centerX + size * 0.25f, centerY - size * 0.15f)
    )
    
    // Wings
    val leftWing = Path().apply {
        moveTo(centerX - size * 0.1f, centerY)
        lineTo(centerX - size * 0.4f, centerY - size * 0.3f)
        lineTo(centerX - size * 0.2f, centerY - size * 0.1f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(centerX + size * 0.1f, centerY)
        lineTo(centerX + size * 0.4f, centerY - size * 0.3f)
        lineTo(centerX + size * 0.2f, centerY - size * 0.1f)
        close()
    }
    drawPath(leftWing, Color(0xFF654321))  // Dark brown wings
    drawPath(rightWing, Color(0xFF654321))
    
    // Eyes
    drawCircle(color = Color.Yellow, radius = size * 0.04f, center = Offset(centerX + size * 0.25f, centerY - size * 0.18f))
    
    // Tail
    drawLine(
        color = Color(0xFF8B0000),
        start = Offset(centerX - size * 0.2f, centerY),
        end = Offset(centerX - size * 0.35f, centerY + size * 0.2f),
        strokeWidth = 4f
    )
    
    // Fire breath
    for (i in 0..2) {
        drawCircle(
            color = Color(0xFFFF4500).copy(alpha = 0.6f),  // Orange-red
            radius = size * 0.08f,
            center = Offset(centerX + size * 0.4f + i * size * 0.1f, centerY - size * 0.15f + i * size * 0.05f)
        )
    }
}

