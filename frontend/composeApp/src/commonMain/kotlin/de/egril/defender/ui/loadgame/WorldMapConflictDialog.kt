package de.egril.defender.ui.loadgame

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.model.LevelStatus
import de.egril.defender.ui.WorldMapConflict
import defender_of_egril.composeapp.generated.resources.*

@Composable
fun WorldMapConflictDialog(
    conflict: WorldMapConflict,
    onUseSavedVersion: () -> Unit,
    onUseCurrentVersion: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { 
            Text(stringResource(Res.string.world_map_conflict_title)) 
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.world_map_conflict_message),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Find differences
                val allLevelIds = (conflict.savedWorldMap.levelStatuses.keys + conflict.currentWorldMap.keys).toSet()
                val differences = allLevelIds.mapNotNull { levelId ->
                    val savedStatus = conflict.savedWorldMap.levelStatuses[levelId]
                    val currentStatus = conflict.currentWorldMap[levelId]
                    
                    if (savedStatus != currentStatus) {
                        Triple(levelId, savedStatus, currentStatus)
                    } else {
                        null
                    }
                }
                
                if (differences.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.world_map_conflict_differences),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.world_map_conflict_level),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(Res.string.world_map_conflict_saved),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(Res.string.world_map_conflict_current),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    differences.forEach { (levelId, savedStatus, currentStatus) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = levelId,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatLevelStatus(savedStatus),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatLevelStatus(currentStatus),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column {
                Button(
                    onClick = onUseSavedVersion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.world_map_conflict_use_saved))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onUseCurrentVersion,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(Res.string.world_map_conflict_use_current))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun formatLevelStatus(status: LevelStatus?): String {
    return when (status) {
        LevelStatus.LOCKED -> stringResource(Res.string.level_status_locked)
        LevelStatus.UNLOCKED -> stringResource(Res.string.level_status_unlocked)
        LevelStatus.WON -> stringResource(Res.string.level_status_won)
        null -> stringResource(Res.string.level_status_unknown)
    }
}
