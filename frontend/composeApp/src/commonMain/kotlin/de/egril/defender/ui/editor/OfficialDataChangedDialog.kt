package de.egril.defender.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import defender_of_egril.composeapp.generated.resources.*
import androidx.compose.foundation.text.selection.SelectionContainer

/**
 * Dialog to warn user about modifications to official game data.
 * Shown when the game is closing and official data has been modified.
 */
@Composable
fun OfficialDataChangedDialog(
    modifiedMaps: List<String>,
    modifiedLevels: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.official_data_changed_title))
        },
        text = {
            SelectionContainer {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(Res.string.official_data_changed_message))
                
                if (modifiedMaps.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.official_data_modified_maps, modifiedMaps.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                if (modifiedLevels.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.official_data_modified_levels, modifiedLevels.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.understood))
            }
        }
    )
}
