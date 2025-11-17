package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.egril.defender.editor.EditorStorage
import de.egril.defender.ui.icon.DownArrowIcon
import de.egril.defender.ui.icon.UpArrowIcon
import de.egril.defender.ui.editor.ConfirmationDialog
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.*

/**
 * Main content for the Level Sequence tab
 */
@Composable
fun LevelSequenceContent() {
    val sequence = remember { mutableStateOf(EditorStorage.getLevelSequence()) }
    val allLevels = remember { mutableStateOf(EditorStorage.getAllLevels()) }
    var levelToRemove by remember { mutableStateOf<Pair<String, String>?>(null) }
    
    // Get levels in sequence that are ready to play
    val levelsInSequence = sequence.value.sequence.mapNotNull { levelId ->
        val level = EditorStorage.getLevel(levelId)
        if (level != null && EditorStorage.isLevelReadyToPlay(level)) {
            levelId to level
        } else null
    }
    
    // Get all levels not in sequence that are ready to play
    val availableLevels = allLevels.value.filter { level ->
        EditorStorage.isLevelReadyToPlay(level) && !sequence.value.sequence.contains(level.id)
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(Res.string.level_sequence),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = stringResource(Res.string.arrange_level_order),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Section: Levels in Sequence
        Text(
            text = stringResource(Res.string.levels_in_sequence),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (levelsInSequence.isEmpty()) {
            Text(
                text = "No levels in sequence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(levelsInSequence.size) { index ->
                    val (levelId, level) = levelsInSequence[index]
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "${index + 1}. ${level.title}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        EditorStorage.moveLevelUp(levelId)
                                        sequence.value = EditorStorage.getLevelSequence()
                                    },
                                    enabled = index > 0
                                ) {
                                    UpArrowIcon(size = 16.dp, tint = Color.White)
                                }
                                
                                Button(
                                    onClick = {
                                        EditorStorage.moveLevelDown(levelId)
                                        sequence.value = EditorStorage.getLevelSequence()
                                    },
                                    enabled = index < levelsInSequence.size - 1
                                ) {
                                    DownArrowIcon(size = 16.dp, tint = Color.White)
                                }
                                
                                Button(
                                    onClick = {
                                        levelToRemove = levelId to level.title
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(stringResource(Res.string.remove_from_sequence))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Section: Available Levels
        Text(
            text = stringResource(Res.string.available_levels),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (availableLevels.isEmpty()) {
            Text(
                text = "No available levels",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(0.5f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableLevels.size) { index ->
                    val level = availableLevels[index]
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = level.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = level.subtitle.ifBlank { level.id },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Button(
                                onClick = {
                                    EditorStorage.addLevelToSequence(level.id)
                                    sequence.value = EditorStorage.getLevelSequence()
                                    allLevels.value = EditorStorage.getAllLevels()
                                }
                            ) {
                                Text(stringResource(Res.string.add_to_sequence))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Confirmation dialog for removing level from sequence
    levelToRemove?.let { (levelId, levelTitle) ->
        ConfirmationDialog(
            title = stringResource(Res.string.remove_level_confirmation_title),
            message = stringResource(Res.string.remove_level_confirmation_message).replace("%s", levelTitle),
            onDismiss = { levelToRemove = null },
            onConfirm = {
                EditorStorage.removeLevelFromSequence(levelId)
                sequence.value = EditorStorage.getLevelSequence()
                allLevels.value = EditorStorage.getAllLevels()
                levelToRemove = null
            }
        )
    }
}
