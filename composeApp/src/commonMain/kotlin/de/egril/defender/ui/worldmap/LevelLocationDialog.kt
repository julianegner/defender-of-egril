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
import de.egril.defender.editor.EditorStorage
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*

/**
 * Dialog that shows all levels at a specific map location.
 * Each level is shown as a card with a button to start the level.
 * The button is only active if the level is playable (not locked).
 */
@Composable
fun LevelLocationDialog(
    location: WorldMapLocation,
    levelsAtLocation: List<WorldLevel>,
    onPlayLevel: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDarkMode = AppSettings.isDarkMode.value
    val currentDifficulty = AppSettings.difficulty.value
    
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
                
                Text(
                    text = stringResource(Res.string.levels_at_location, levelsAtLocation.size.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Level cards list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(levelsAtLocation) { worldLevel ->
                        LevelLocationCard(
                            worldLevel = worldLevel,
                            isDarkMode = isDarkMode,
                            currentDifficulty = currentDifficulty,
                            onPlayLevel = onPlayLevel
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

/**
 * A card showing a single level within the location dialog.
 * Shows level name, status, prerequisites, and a play button.
 */
@Composable
private fun LevelLocationCard(
    worldLevel: WorldLevel,
    isDarkMode: Boolean,
    currentDifficulty: de.egril.defender.ui.settings.DifficultyLevel,
    onPlayLevel: (Int) -> Unit
) {
    val isPlayable = worldLevel.status != LevelStatus.LOCKED
    
    val cardBackgroundColor = when (worldLevel.status) {
        LevelStatus.LOCKED -> if (isDarkMode) Color(0xFF3C3C3C) else Color(0xFFE0E0E0)
        LevelStatus.UNLOCKED -> if (isDarkMode) Color(0xFF0D47A1) else Color(0xFF2196F3)
        LevelStatus.WON -> if (isDarkMode) Color(0xFF1B5E20) else Color(0xFF4CAF50)
    }
    
    val textColor = if (isDarkMode || worldLevel.status != LevelStatus.LOCKED) Color.White else Color.Black
    
    // Get prerequisite info
    val editorLevel = worldLevel.level.editorLevelId?.let { EditorStorage.getLevel(it) }
    val prerequisites = editorLevel?.prerequisites ?: emptySet()
    val requiredCount = editorLevel?.getEffectiveRequiredCount() ?: 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Level name and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = worldLevel.level.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                    
                    if (worldLevel.level.subtitle.isNotBlank()) {
                        Text(
                            text = worldLevel.level.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = textColor.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Status badge
                val statusText = when (worldLevel.status) {
                    LevelStatus.LOCKED -> stringResource(Res.string.locked)
                    LevelStatus.UNLOCKED -> stringResource(Res.string.available)
                    LevelStatus.WON -> stringResource(Res.string.completed)
                }
                
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Prerequisites info (show when locked)
            if (worldLevel.status == LevelStatus.LOCKED && prerequisites.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                
                val prereqText = if (requiredCount < prerequisites.size) {
                    stringResource(Res.string.requires_any_of, requiredCount.toString())
                } else {
                    stringResource(Res.string.requires_all)
                }
                
                Text(
                    text = prereqText,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
                
                // List prerequisite level names
                for (prereqId in prerequisites) {
                    val prereqLevel = EditorStorage.getLevel(prereqId)
                    val prereqName = prereqLevel?.title ?: prereqId
                    val prereqStatus = EditorStorage.getAllLevels()
                        .find { it.id == prereqId }
                        ?.let { /* Would need world map state */ }
                    
                    Text(
                        text = "• $prereqName",
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Play button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { onPlayLevel(worldLevel.level.id) },
                    enabled = isPlayable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlayable) {
                            if (isDarkMode) Color(0xFF2E7D32) else Color(0xFF4CAF50)
                        } else {
                            Color.Gray
                        },
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.5f),
                        disabledContentColor = Color.White.copy(alpha = 0.5f)
                    )
                ) {
                    Text(stringResource(Res.string.play_level))
                }
            }
        }
    }
}
