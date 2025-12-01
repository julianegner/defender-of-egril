package de.egril.defender.ui.worldmap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import de.egril.defender.model.LevelStatus
import de.egril.defender.model.WorldLevel
import de.egril.defender.ui.settings.AppSettings
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog that shows all levels at a specific map location.
 * Each level is shown as a clickable card - clicking starts the level.
 * 
 * Uses the same LevelCard component as the LevelCardsView for consistency.
 */
@Composable
fun LevelLocationDialog(
    location: WorldMapLocation,
    levelsAtLocation: List<WorldLevel>,
    onPlayLevel: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkMode = AppSettings.isDarkMode.value
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Header
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                
                if (levelsAtLocation.size > 1) {
                    Text(
                        text = stringResource(Res.string.levels_at_location, levelsAtLocation.size.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) Color.LightGray else Color.DarkGray
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Show all levels using the same LevelCard component
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(levelsAtLocation) { worldLevel ->
                        val isPlayable = worldLevel.status != LevelStatus.LOCKED
                        LevelCard(
                            worldLevel = worldLevel,
                            onClick = { if (isPlayable) onPlayLevel(worldLevel.level.id) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(Res.string.close))
                    }
                }
            }
        }
    }
}
