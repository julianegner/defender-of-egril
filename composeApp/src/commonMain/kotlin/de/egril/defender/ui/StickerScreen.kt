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
 * - ApplicationBanner with game symbols, title and logo
 * - Additional text below banner
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
                // Left side: Banner and text
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    ApplicationBanner()
                    
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
