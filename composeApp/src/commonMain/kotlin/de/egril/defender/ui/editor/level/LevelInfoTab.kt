package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.ui.editor.map.MapSelectionCard
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.level_title
import defender_of_egril.composeapp.generated.resources.map_label
import defender_of_egril.composeapp.generated.resources.start_coins
import defender_of_egril.composeapp.generated.resources.start_hp
import defender_of_egril.composeapp.generated.resources.subtitle_optional

/**
 * Tab 1: Level Info (title, subtitle, map, coins, HP)
 */
@Composable
fun LevelInfoTab(
    title: String,
    onTitleChange: (String) -> Unit,
    subtitle: String,
    onSubtitleChange: (String) -> Unit,
    selectedMapId: String,
    onMapChange: (String) -> Unit,
    maps: List<EditorMap>,
    startCoins: String,
    onStartCoinsChange: (String) -> Unit,
    startHP: String,
    onStartHPChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title and subtitle
        item {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.level_title)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = subtitle,
                onValueChange = onSubtitleChange,
                label = { Text(stringResource(Res.string.subtitle_optional)) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Map selection with mini-maps
        item {
            Column {
                Text(
                    text = "${stringResource(Res.string.map_label)}:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(maps) { map ->
                        MapSelectionCard(
                            map = map,
                            isSelected = selectedMapId == map.id,
                            onClick = { onMapChange(map.id) }
                        )
                    }
                }
            }
        }

        // Start coins and HP
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startCoins,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onStartCoinsChange(it) },
                    label = { Text(stringResource(Res.string.start_coins)) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = startHP,
                    onValueChange = { if (it.all { c -> c.isDigit() }) onStartHPChange(it) },
                    label = { Text(stringResource(Res.string.start_hp)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
