package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.OfficialEditMode
import de.egril.defender.editor.EditorMap
import defender_of_egril.composeapp.generated.resources.*

/**
 * Inline resize controls for the side panel. Text fields accept only integers (negative values
 * shrink the map on that side).
 */
@Composable
internal fun ResizeMapSection(
    map: EditorMap,
    mapWidth: Int,
    mapHeight: Int,
    resizeLeft: String,
    onResizeLeftChange: (String) -> Unit,
    resizeRight: String,
    onResizeRightChange: (String) -> Unit,
    resizeTop: String,
    onResizeTopChange: (String) -> Unit,
    resizeBottom: String,
    onResizeBottomChange: (String) -> Unit,
    onApplyResize: () -> Unit,
    canApplyResize: Boolean,
    resultingMapWidth: Int,
    resultingMapHeight: Int,
    showUnsafeResizeWarning: Boolean,
    mapUsageLevelNames: List<String>,
) {
    fun isIntegerInput(value: String): Boolean = value.isEmpty() || value == "-" || value.matches(Regex("-?[0-9]+"))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${stringResource(Res.string.map_size)}: $mapWidth x $mapHeight",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(Res.string.resize_map_hint),
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = resizeLeft,
                onValueChange = { if (isIntegerInput(it)) onResizeLeftChange(it) },
                label = { Text(stringResource(Res.string.columns_left)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = resizeRight,
                onValueChange = { if (isIntegerInput(it)) onResizeRightChange(it) },
                label = { Text(stringResource(Res.string.columns_right)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = resizeTop,
                onValueChange = { if (isIntegerInput(it)) onResizeTopChange(it) },
                label = { Text(stringResource(Res.string.rows_top)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            OutlinedTextField(
                value = resizeBottom,
                onValueChange = { if (isIntegerInput(it)) onResizeBottomChange(it) },
                label = { Text(stringResource(Res.string.rows_bottom)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
        }
        Text(
            text = stringResource(Res.string.resulting_map_size, resultingMapWidth, resultingMapHeight),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (canApplyResize) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
        if (showUnsafeResizeWarning) {
            Text(
                text =
                    "${stringResource(Res.string.map_resize_in_use_warning)} " +
                        "${stringResource(Res.string.used_in_levels)}: ${mapUsageLevelNames.joinToString(", ")}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Button(
            onClick = onApplyResize,
            enabled = canApplyResize && (!map.isOfficial || OfficialEditMode.enabled),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.apply))
        }
    }
}
