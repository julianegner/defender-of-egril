package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.ui.editor.map.MapSelectionCard
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.allow_auto_attack
import defender_of_egril.composeapp.generated.resources.level_title
import defender_of_egril.composeapp.generated.resources.map_label
import defender_of_egril.composeapp.generated.resources.start_coins
import defender_of_egril.composeapp.generated.resources.start_hp
import defender_of_egril.composeapp.generated.resources.subtitle_optional
import defender_of_egril.composeapp.generated.resources.test_level

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
    onStartHPChange: (String) -> Unit,
    testingOnly: Boolean,
    onTestingOnlyChange: (Boolean) -> Unit,
    allowAutoAttack: Boolean,
    onAllowAutoAttackChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title and Test Level toggle in same row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(Res.string.level_title)) },
                    modifier = Modifier.weight(1f)
                )
                
                // Test Level toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.test_level),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = testingOnly,
                        onCheckedChange = onTestingOnlyChange
                    )
                }
                
                // Allow Auto-Attack toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.allow_auto_attack),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = allowAutoAttack,
                        onCheckedChange = onAllowAutoAttackChange
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = subtitle,
                onValueChange = onSubtitleChange,
                label = { Text(stringResource(Res.string.subtitle_optional)) },
                modifier = Modifier.fillMaxWidth()
            )
        }


        // Map selection with mini-maps using a grid layout
        // Shows 8 columns and 2 visible rows, with scrolling to load more
        item {
            Column {
                Text(
                    text = "${stringResource(Res.string.map_label)}:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                // Height for 2 rows of MapSelectionCards (each card is approximately 160dp tall)
                // Including spacing between items
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp), // Height for ~2 rows with spacing
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(4.dp)
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
