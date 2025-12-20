package de.egril.defender.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.model.AttackerType
import de.egril.defender.model.DefenderType
import de.egril.defender.ui.icon.enemy.*
import de.egril.defender.ui.icon.defender.*
import de.egril.defender.ui.settings.SettingsButton
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.serialization.EncodeDefault
import org.jetbrains.compose.resources.painterResource

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
                    /*
                    Image(
                        painter = painterResource(Res.drawable.example_map_cutout),
                        contentDescription = "Game Map",
                        modifier = Modifier.size(300.dp)
                    )
                     */

                    Row {
                        Box(
                            modifier = Modifier
                                .height(80.dp)
                                .width(80.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerX = size.width / 2
                                val centerY = (size.height / 2) + 20f
                                val iconSize = minOf(size.width, size.height)

                                drawGoblinSymbol(centerX.plus(20), centerY.minus(20), iconSize * 0.7f)
                                drawOrkSymbol(centerX, centerY.minus(10), iconSize * 0.7f)
                                drawEvilWizardSymbol(centerX.minus(20), centerY, iconSize * 0.7f)


                                drawTower(DefenderType.BOW_TOWER, centerX.plus(80), centerY.minus(20), iconSize)
                                
                                // Draw black background with same trapezoid shape as wizard tower to prevent bow tower from showing through
                                val wizardCenterX = centerX.plus(100)
                                val wizardCenterY = centerY
                                val wizardBaseSize = iconSize * 0.8f
                                val topWidth = wizardBaseSize * 0.4f
                                val bottomWidth = wizardBaseSize * 0.6f
                                val towerHeight = wizardBaseSize * 0.6f
                                val top = wizardCenterY - towerHeight / 2
                                val bottom = wizardCenterY + towerHeight / 2
                                
                                val blackTrapezoid = Path().apply {
                                    moveTo(wizardCenterX - bottomWidth / 2, bottom)
                                    lineTo(wizardCenterX + bottomWidth / 2, bottom)
                                    lineTo(wizardCenterX + topWidth / 2, top)
                                    lineTo(wizardCenterX - topWidth / 2, top)
                                    close()
                                }
                                drawPath(blackTrapezoid, Color.Black)
                                
                                // Draw battlements in black
                                val battlement = wizardBaseSize * 0.08f
                                for (i in 0..2) {
                                    val x = wizardCenterX - topWidth / 2 + (topWidth / 3) * i
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(x, top - battlement),
                                        size = androidx.compose.ui.geometry.Size(battlement, battlement)
                                    )
                                }
                                
                                drawTower(DefenderType.WIZARD_TOWER, centerX.plus(100), centerY, iconSize)
                            }
                        }
                        Spacer(Modifier.width(80.dp))
                        ApplicationBanner()
                    }
                    Row {
                        Text(stringResource(Res.string.game_sticker_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 15.sp,
                        )
                    }
                    Row {
                        Text("defender.egril.de",
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 18.sp,
                        )
                    }
                }
            }
        }
    }
}
