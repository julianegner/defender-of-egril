package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.icon.enemy.*
import de.egril.defender.ui.icon.defender.*
import de.egril.defender.ui.settings.SettingsButton

/**
 * Screen for displaying sticker merchandise preview.
 * Reachable with the "sticker" cheat code.
 * 
 * Layout:
 * - Left side: Game map section and enemy/tower groups
 * - Right side: Title and logo from ApplicationBanner
 */
@Composable
fun StickerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Settings button in top-right corner
            SettingsButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )
            
            // Main content
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Map and unit groups
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Game map section
                    MapSection()
                    
                    // Enemy units group
                    EnemyUnitsGroup()
                    
                    // Tower units group
                    TowerUnitsGroup()
                }
                
                // Right side: Title and logo
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ApplicationBanner()
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // Back button
                    Button(
                        onClick = onBack,
                        modifier = Modifier.width(200.dp).height(50.dp)
                    ) {
                        Text("Back")
                    }
                }
            }
        }
    }
}

/**
 * Displays a simplified game map section showing various tile types
 */
@Composable
private fun MapSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Game Map",
            style = MaterialTheme.typography.titleMedium
        )
        
        // Simple hexagonal map display (3x3 grid for demonstration)
        Box(
            modifier = Modifier
                .size(300.dp)
                .border(2.dp, MaterialTheme.colorScheme.onBackground)
                .background(Color(0xFF87CEEB)) // Sky blue background
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hexSize = 30f
                val rows = 5
                val cols = 5
                
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val x = col * hexSize * 1.8f + 40f
                        val y = row * hexSize * 1.6f + 40f + (if (col % 2 == 0) 0f else hexSize * 0.8f)
                        
                        // Determine tile color based on position
                        val tileColor = when {
                            col == 0 -> Color(0xFFFF6B6B) // Spawn point (red)
                            col == cols - 1 -> Color(0xFF4ECDC4) // Target (cyan)
                            row % 2 == 0 && col % 2 == 0 -> Color(0xFF95E1D3) // Build area (light green)
                            else -> Color(0xFFFFE66D) // Path (yellow)
                        }
                        
                        drawHexagon(x, y, hexSize, tileColor)
                    }
                }
            }
        }
    }
}

/**
 * Displays enemy units group (Goblin, Ork, Wizard)
 */
@Composable
private fun EnemyUnitsGroup() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Enemies",
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Goblin
            EnemyUnitCard(AttackerType.GOBLIN, "Goblin")
            
            // Ork
            EnemyUnitCard(AttackerType.ORK, "Ork")
            
            // Evil Wizard
            EnemyUnitCard(AttackerType.EVIL_WIZARD, "Wizard")
        }
    }
}

/**
 * Displays tower units group (Bow, Wizard)
 */
@Composable
private fun TowerUnitsGroup() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Towers",
            style = MaterialTheme.typography.titleMedium
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bow Tower
            TowerUnitCard(DefenderType.BOW_TOWER, "Bow")
            
            // Wizard Tower
            TowerUnitCard(DefenderType.WIZARD_TOWER, "Wizard")
        }
    }
}

/**
 * Card displaying a single enemy unit
 */
@Composable
private fun EnemyUnitCard(
    attackerType: AttackerType,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(2.dp, MaterialTheme.colorScheme.onBackground)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val iconSize = minOf(size.width, size.height)
                
                when (attackerType) {
                    AttackerType.GOBLIN -> drawGoblinSymbol(centerX, centerY, iconSize * 0.7f)
                    AttackerType.ORK -> drawOrkSymbol(centerX, centerY, iconSize * 0.7f)
                    AttackerType.EVIL_WIZARD -> drawEvilWizardSymbol(centerX, centerY, iconSize * 0.7f)
                    else -> {}
                }
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Card displaying a single tower unit
 */
@Composable
private fun TowerUnitCard(
    defenderType: DefenderType,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .border(2.dp, MaterialTheme.colorScheme.onBackground)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val iconSize = minOf(size.width, size.height)
                
                // Draw tower base
                drawTowerBase(centerX, centerY, iconSize * 0.8f)
                
                // Draw tower symbol
                when (defenderType) {
                    DefenderType.BOW_TOWER -> drawBowSymbol(centerX, centerY, iconSize * 0.45f)
                    DefenderType.WIZARD_TOWER -> drawWizardSymbol(centerX, centerY, iconSize * 0.4f)
                    else -> {}
                }
            }
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/**
 * Helper function to draw a hexagon on canvas
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHexagon(
    x: Float,
    y: Float,
    size: Float,
    color: Color
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        for (i in 0..5) {
            val angle = Math.PI / 3 * i
            val px = x + size * kotlin.math.cos(angle).toFloat()
            val py = y + size * kotlin.math.sin(angle).toFloat()
            
            if (i == 0) {
                moveTo(px, py)
            } else {
                lineTo(px, py)
            }
        }
        close()
    }
    
    drawPath(path, color)
    drawPath(path, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
}

/**
 * Helper function to draw tower base (from defender icons)
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTowerBase(
    centerX: Float,
    centerY: Float,
    size: Float
) {
    val path = androidx.compose.ui.graphics.Path().apply {
        // Trapezoid (wider at bottom, narrower at top)
        val topWidth = size * 0.5f
        val bottomWidth = size * 0.7f
        val height = size * 0.4f
        
        moveTo(centerX - bottomWidth / 2, centerY + height / 2)
        lineTo(centerX + bottomWidth / 2, centerY + height / 2)
        lineTo(centerX + topWidth / 2, centerY - height / 2)
        lineTo(centerX - topWidth / 2, centerY - height / 2)
        close()
    }
    
    drawPath(path, Color(0xFF8B7355)) // Brown
    drawPath(path, Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))
}
