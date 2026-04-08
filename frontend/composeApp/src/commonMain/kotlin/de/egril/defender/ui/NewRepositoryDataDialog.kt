package de.egril.defender.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import de.egril.defender.editor.RepositoryManager
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog shown when new map/level data is detected in the repository
 */
@Composable
fun NewRepositoryDataDialog(
    newData: RepositoryManager.NewRepositoryData,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(Res.string.new_repository_data_title))
        },
        text = {
            SelectionContainer {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.new_repository_data_message),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (newData.newMaps.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.new_maps_found, newData.newMaps.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    newData.newMaps.take(5).forEach { mapId ->
                        Text(
                            text = "  • $mapId",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (newData.newMaps.size > 5) {
                        Text(
                            text = "  • ${stringResource(Res.string.and_more, newData.newMaps.size - 5)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (newData.newLevels.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.new_levels_found, newData.newLevels.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    newData.newLevels.take(5).forEach { levelId ->
                        Text(
                            text = "  • $levelId",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (newData.newLevels.size > 5) {
                        Text(
                            text = "  • ${stringResource(Res.string.and_more, newData.newLevels.size - 5)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (newData.hasNewSequence) {
                    Text(
                        text = stringResource(Res.string.sequence_will_be_replaced),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(Res.string.sequence_backup_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text(stringResource(Res.string.add_data))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}
