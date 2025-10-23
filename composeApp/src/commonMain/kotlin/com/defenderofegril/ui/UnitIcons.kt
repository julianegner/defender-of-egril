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
            
            // Draw tower base (trapezoid shape)
            drawTowerBase(centerX, centerY, iconSize * 0.8f)
            
            // Draw tower type symbol inside
            when (defender.type) {
                DefenderType.SPIKE_TOWER -> drawSpikeSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.SPEAR_TOWER -> drawSpearSymbol(centerX, centerY, iconSize * 0.5f)
                DefenderType.BOW_TOWER -> drawBowSymbol(centerX, centerY, iconSize * 0.45f)
                DefenderType.WIZARD_TOWER -> drawWizardSymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.ALCHEMY_TOWER -> drawAlchemySymbol(centerX, centerY, iconSize * 0.4f)
                DefenderType.BALLISTA_TOWER -> drawBallistaSymbol(centerX, centerY, iconSize * 0.5f)
            }
        }
        
        // Actions indicator on the top right (lightning bolts) - drawn on top, overlaying icon
        if (defender.isReady && defender.actionsRemaining > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp),  // Extra padding to avoid hexagon edge
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(defender.actionsRemaining) {
                    Text(
                        text = "⚡",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 16.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Level indicator at top center - overlaying icon, no background
        Text(
            text = "L${defender.level}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)  // Extra padding from top to avoid hexagon edge
        )
        
        // Build time indicator at bottom center (only if not ready)
        if (!defender.isReady) {
            Text(
                text = "⏱${defender.buildTimeRemaining}",
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
    
    // Bow arc
    val path = Path().apply {
        moveTo(centerX + bowWidth / 2, centerY - bowHeight / 2)
        cubicTo(
            centerX + bowWidth * 0.3f, centerY - bowHeight / 4,
            centerX + bowWidth * 0.3f, centerY + bowHeight / 4,
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
    
    // Arrow
    drawLine(
        color = Color(0xFFC0C0C0),
        start = Offset(centerX - bowWidth / 3, centerY),
        end = Offset(centerX + bowWidth / 3, centerY),
        strokeWidth = 2f
    )
    // Arrowhead
    val arrowPath = Path().apply {
        moveTo(centerX + bowWidth / 3, centerY)
        lineTo(centerX + bowWidth / 4, centerY - size * 0.1f)
        lineTo(centerX + bowWidth / 4, centerY + size * 0.1f)
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
            }
        }
        
        // Health number at top center - overlaying icon, no background
        Text(
            text = "${attacker.currentHealth}",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)  // Extra padding from top to avoid hexagon edge
        )
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
