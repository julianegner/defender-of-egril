package de.egril.defender.ui.editor.level

import androidx.compose.foundation.border
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.EditorMap
import de.egril.defender.ui.editor.map.MapSelectionCard
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.allow_auto_attack
import defender_of_egril.composeapp.generated.resources.auto_attack_info_message
import defender_of_egril.composeapp.generated.resources.auto_attack_info_title
import defender_of_egril.composeapp.generated.resources.author_optional
import defender_of_egril.composeapp.generated.resources.level_title
import defender_of_egril.composeapp.generated.resources.map_label
import defender_of_egril.composeapp.generated.resources.ok
import defender_of_egril.composeapp.generated.resources.start_coins
import defender_of_egril.composeapp.generated.resources.start_hp
import defender_of_egril.composeapp.generated.resources.subtitle_optional
import defender_of_egril.composeapp.generated.resources.test_level
import defender_of_egril.composeapp.generated.resources.user_map_not_allowed_title
import defender_of_egril.composeapp.generated.resources.user_map_not_allowed_message

/**
 * Tab 1: Level Info (title, subtitle, map, coins, HP)
 * 
 * Layout: Title and Subtitle input fields on the left, level toggles (Test Level, Allow Auto-Attack, etc.) 
 * in a bordered container on the right. This container will grow vertically as more toggles are added.
 */
@Composable
fun LevelInfoTab(
    title: String,
    onTitleChange: (String) -> Unit,
    subtitle: String,
    onSubtitleChange: (String) -> Unit,
    author: String,
    onAuthorChange: (String) -> Unit,
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
    onAllowAutoAttackChange: (Boolean) -> Unit,
    isOfficial: Boolean = false
) {
    var showAutoAttackInfo by remember { mutableStateOf(false) }
    var showUserMapNotAllowedDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title, Subtitle, and Level Toggles Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left side: Title and Subtitle fields in a column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text(stringResource(Res.string.level_title)) },
                        enabled = !isOfficial || de.egril.defender.OfficialEditMode.enabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = subtitle,
                        onValueChange = onSubtitleChange,
                        label = { Text(stringResource(Res.string.subtitle_optional)) },
                        enabled = !isOfficial || de.egril.defender.OfficialEditMode.enabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = author,
                        onValueChange = onAuthorChange,
                        label = { Text(stringResource(Res.string.author_optional)) },
                        enabled = !isOfficial || de.egril.defender.OfficialEditMode.enabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Right side: Level Toggles Container
                // This container holds all level-related toggles and will grow as more toggles are added
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Test Level toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.allow_auto_attack),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = allowAutoAttack,
                            onCheckedChange = { newValue ->
                                // Show info dialog when enabling for the first time
                                if (newValue && !allowAutoAttack) {
                                    showAutoAttackInfo = true
                                }
                                onAllowAutoAttackChange(newValue)
                            }
                        )
                    }
                }
            }
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
                        // For official levels, user maps should be disabled
                        val isMapEnabled = !isOfficial || map.isOfficial
                        
                        MapSelectionCard(
                            map = map,
                            isSelected = selectedMapId == map.id,
                            onClick = { onMapChange(map.id) },
                            isEnabled = isMapEnabled,
                            onDisabledClick = {
                                // Show dialog when clicking on disabled user map
                                showUserMapNotAllowedDialog = true
                            }
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
    
    // Info dialog for auto-attack feature
    if (showAutoAttackInfo) {
        AlertDialog(
            onDismissRequest = { showAutoAttackInfo = false },
            title = { Text(stringResource(Res.string.auto_attack_info_title)) },
            text = { Text(stringResource(Res.string.auto_attack_info_message)) },
            confirmButton = {
                Button(onClick = { showAutoAttackInfo = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
    
    // Dialog for user map not allowed in official levels
    if (showUserMapNotAllowedDialog) {
        AlertDialog(
            onDismissRequest = { showUserMapNotAllowedDialog = false },
            title = { Text(stringResource(Res.string.user_map_not_allowed_title)) },
            text = { Text(stringResource(Res.string.user_map_not_allowed_message)) },
            confirmButton = {
                Button(onClick = { showUserMapNotAllowedDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}
