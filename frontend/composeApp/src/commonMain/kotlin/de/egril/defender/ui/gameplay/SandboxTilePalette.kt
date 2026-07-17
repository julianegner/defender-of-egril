package de.egril.defender.ui.gameplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.TileType
import de.egril.defender.ui.editor.TileTypeButton
import de.egril.defender.ui.icon.PencilIcon
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.sandbox_done_editing
import defender_of_egril.composeapp.generated.resources.sandbox_edit_map

/**
 * Persistent map-tile selector for sandbox levels, reusing the map editor's [TileTypeButton].
 * It is always available while playing a sandbox level: selecting a tile type activates painting
 * (tapping a map tile repaints it), and "Done editing" (or reselecting the active type) turns
 * painting off so normal interactions resume.
 */
@Composable
fun SandboxTilePalette(
    selectedTileType: TileType?,
    onSelectTileType: (TileType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(8.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PencilIcon(size = 16.dp)
                Text(
                    text = stringResource(Res.string.sandbox_edit_map),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            TileType.entries.forEach { type ->
                TileTypeButton(
                    tileType = type,
                    selected = selectedTileType == type,
                    // Reselecting the active tile toggles painting off.
                    onClick = { onSelectTileType(if (selectedTileType == type) null else type) },
                )
            }

            if (selectedTileType != null) {
                OutlinedButton(onClick = { onSelectTileType(null) }) {
                    Text(stringResource(Res.string.sandbox_done_editing))
                }
            }
        }
    }
}
