package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.findUnreachableLevels
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.close
import defender_of_egril.composeapp.generated.resources.unreachable_create_location
import defender_of_egril.composeapp.generated.resources.unreachable_levels
import defender_of_egril.composeapp.generated.resources.unreachable_levels_none
import defender_of_egril.composeapp.generated.resources.unreachable_levels_official_only
import defender_of_egril.composeapp.generated.resources.unreachable_levels_show_test

/**
 * Lists the levels that cannot be reached through the world map, with filters for official
 * and test levels. Selected levels can be turned into a new location via [onCreateLocation].
 */
@Composable
fun UnreachableLevelsDialog(
    levels: List<EditorLevel>,
    sequence: List<String>,
    worldMap: WorldMapData,
    onDismiss: () -> Unit,
    onCreateLocation: (List<String>) -> Unit,
) {
    var officialOnly by remember { mutableStateOf(true) }
    var showTestLevels by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val unreachable = findUnreachableLevels(
        levels = levels,
        sequence = sequence,
        worldMap = worldMap,
        officialOnly = officialOnly,
        includeTestLevels = showTestLevels,
    )
    val selectedVisibleIds = unreachable.map { it.id }.filter { it in selectedIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.unreachable_levels)) },
        text = {
            Column {
                ToggleRow(
                    label = stringResource(Res.string.unreachable_levels_official_only),
                    checked = officialOnly,
                    onCheckedChange = { officialOnly = it },
                )
                ToggleRow(
                    label = stringResource(Res.string.unreachable_levels_show_test),
                    checked = showTestLevels,
                    onCheckedChange = { showTestLevels = it },
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                if (unreachable.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.unreachable_levels_none),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                        items(unreachable) { level ->
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIds =
                                                if (level.id in selectedIds) selectedIds - level.id else selectedIds + level.id
                                        }.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = level.id in selectedIds,
                                    onCheckedChange = null,
                                )
                                Column {
                                    Text(
                                        text = level.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    Text(
                                        text = level.id,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(listState),
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateLocation(selectedVisibleIds) },
                enabled = selectedVisibleIds.isNotEmpty(),
            ) {
                Text(stringResource(Res.string.unreachable_create_location))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
