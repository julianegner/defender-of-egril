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
import androidx.compose.ui.graphics.Color
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


/* todo remove string resources:
    sticker_enemies
    sticker_goblin
    sticker_ork
    sticker_wizard
    sticker_bow
    sticker_towers
    sticker_game_map
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
                                drawTower(DefenderType.WIZARD_TOWER, centerX.plus(100), centerY, iconSize)
                            }
                        }
                        Spacer(Modifier.width(80.dp))
                        ApplicationBanner()
                    }
                    Row {
                        Text("Open Source Turn Based Fantasy Tower Defense Game",
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
