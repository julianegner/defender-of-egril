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
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.icon.enemy.*
import de.egril.defender.ui.icon.defender.*
import de.egril.defender.ui.settings.SettingsButton
import defender_of_egril.composeapp.generated.resources.*


/* todo remove string resources:
    sticker_enemies
    sticker_goblin
    sticker_ork
    sticker_wizard
    sticker_bow
    sticker_towers
 */

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

            // Back button
            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(200.dp)
                    .height(50.dp)
                    .padding(end = 80.dp, top = 8.dp)
            ) {
                Text(stringResource(Res.string.back))
            }

            // Main content
            Row(
                modifier = Modifier
                    .padding(top = 100.dp)
                    .fillMaxSize(),
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

                    Row {
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .width(80.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerX = size.width / 2
                                val centerY = size.height / 2
                                val iconSize = minOf(size.width, size.height)

                                drawGoblinSymbol(centerX.plus(20), centerY.minus(20), iconSize * 0.7f)
                                drawOrkSymbol(centerX, centerY.minus(10), iconSize * 0.7f)
                                drawEvilWizardSymbol(centerX.minus(20), centerY, iconSize * 0.7f)


                                drawTower(DefenderType.BOW_TOWER, centerX.plus(80), centerY.minus(20), iconSize)
                                drawTower(DefenderType.WIZARD_TOWER, centerX.plus(100), centerY, iconSize)
                            }
                        }
                    }
                }
                
                // Right side: Title and logo
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ApplicationBanner()
                    
                    Spacer(modifier = Modifier.height(48.dp))
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
            stringResource(Res.string.sticker_game_map),
            style = MaterialTheme.typography.titleMedium
        )
        
        // Simple hexagonal map display (5x5 grid for demonstration)
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
            val angle = kotlin.math.PI / 3 * i
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
