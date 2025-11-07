package com.defenderofegril.ui.editor.level

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.defenderofegril.editor.EditorStorage
import com.defenderofegril.ui.icon.DownArrowIcon
import com.defenderofegril.ui.icon.UpArrowIcon
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.arrange_level_order
import defender_of_egril.composeapp.generated.resources.hp_short
import defender_of_egril.composeapp.generated.resources.level_sequence

/**
 * Main content for the Level Sequence tab
 */
@Composable
fun LevelSequenceContent() {
    val sequence = remember { mutableStateOf(EditorStorage.getLevelSequence()) }
    
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
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sequence.value.sequence.size) { index ->
                val levelId = sequence.value.sequence[index]
                val level = EditorStorage.getLevel(levelId)
                
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
                                text = "${index + 1}. ${level?.title ?: levelId}",
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
                                enabled = index < sequence.value.sequence.size - 1
                            ) {
                                DownArrowIcon(size = 16.dp, tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
