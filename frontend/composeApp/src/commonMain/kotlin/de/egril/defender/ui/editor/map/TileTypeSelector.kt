package de.egril.defender.ui.editor.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.TileType
import de.egril.defender.model.RiverFlow
import de.egril.defender.model.SpawnPointType
import de.egril.defender.model.TargetType
import de.egril.defender.ui.TooltipWrapper
import de.egril.defender.ui.editor.RiverFlowIndicator
import de.egril.defender.ui.editor.getTileColor
import de.egril.defender.ui.icon.PencilIcon
import de.egril.defender.ui.icon.TriangleDownIcon
import androidx.compose.foundation.layout.height
import defender_of_egril.composeapp.generated.resources.Res
import defender_of_egril.composeapp.generated.resources.cancel
import defender_of_egril.composeapp.generated.resources.edit
import defender_of_egril.composeapp.generated.resources.ok
import defender_of_egril.composeapp.generated.resources.spawn_point_type
import defender_of_egril.composeapp.generated.resources.spawn_point_type_land
import defender_of_egril.composeapp.generated.resources.spawn_point_type_water
import defender_of_egril.composeapp.generated.resources.target_name_label
import defender_of_egril.composeapp.generated.resources.target_type_label
import defender_of_egril.composeapp.generated.resources.target_type_single_hit
import defender_of_egril.composeapp.generated.resources.target_type_standard

/**
 * Tile type dropdown with edit access for target and spawn point properties.
 * Shown at the top of the map editor side panel.
 */
@Composable
fun TileTypeSelector(
    selectedTileType: TileType,
    onTileTypeChange: (TileType) -> Unit,
    selectedRiverFlow: RiverFlow,
    selectedRiverSpeed: Int,
    selectedTargetName: String,
    onTargetNameChange: (String) -> Unit,
    selectedTargetType: TargetType,
    onTargetTypeChange: (TargetType) -> Unit,
    selectedSpawnPointType: SpawnPointType,
    onSpawnPointTypeChange: (SpawnPointType) -> Unit,
) {
    var showTargetPropertiesDialog by remember { mutableStateOf(false) }
    var showSpawnPointPropertiesDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).widthIn(min = 120.dp)) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = getTileColor(selectedTileType).copy(alpha = 0.3f),
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(16.dp)
                                    .background(getTileColor(selectedTileType), shape = MaterialTheme.shapes.small),
                        )
                        Text(
                            text = selectedTileType.name,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (selectedTileType == TileType.RIVER) {
                            RiverFlowIndicator(
                                flowDirection = selectedRiverFlow,
                                flowSpeed = selectedRiverSpeed,
                                size = 14.dp,
                            )
                        }
                        if (selectedTileType == TileType.TARGET) {
                            val typeLabel =
                                when (selectedTargetType) {
                                    TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                            Text(
                                text = typeLabel,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    TriangleDownIcon(size = 10.dp)
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                TileType.entries.forEach { tileType ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(16.dp)
                                            .background(getTileColor(tileType), shape = MaterialTheme.shapes.small),
                                )
                                Text(tileType.name)
                            }
                        },
                        onClick = {
                            onTileTypeChange(tileType)
                            expanded = false
                            if (tileType == TileType.TARGET) {
                                showTargetPropertiesDialog = true
                            } else if (tileType == TileType.SPAWN_POINT) {
                                showSpawnPointPropertiesDialog = true
                            }
                        },
                    )
                }
            }
        }

        if (selectedTileType == TileType.TARGET || selectedTileType == TileType.SPAWN_POINT) {
            val editLabel = stringResource(Res.string.edit)
            TooltipWrapper(text = editLabel) {
                IconButton(
                    onClick = {
                        if (selectedTileType == TileType.TARGET) {
                            showTargetPropertiesDialog = true
                        } else {
                            showSpawnPointPropertiesDialog = true
                        }
                    },
                    modifier = Modifier.size(32.dp).semantics { contentDescription = editLabel },
                ) {
                    PencilIcon(size = 16.dp)
                }
            }
        }
    }

    if (showTargetPropertiesDialog) {
        var localName by remember(showTargetPropertiesDialog) { mutableStateOf(selectedTargetName) }
        AlertDialog(
            onDismissRequest = { showTargetPropertiesDialog = false },
            title = { Text(stringResource(Res.string.target_name_label)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        label = { Text(stringResource(Res.string.target_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(stringResource(Res.string.target_type_label), style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TargetType.entries.forEach { type ->
                            val label =
                                when (type) {
                                    TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                            Button(
                                onClick = { onTargetTypeChange(type) },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (selectedTargetType == type) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.secondary
                                            },
                                    ),
                                modifier = Modifier.height(36.dp),
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onTargetNameChange(localName)
                    showTargetPropertiesDialog = false
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { showTargetPropertiesDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (showSpawnPointPropertiesDialog) {
        AlertDialog(
            onDismissRequest = { showSpawnPointPropertiesDialog = false },
            title = { Text(stringResource(Res.string.spawn_point_type)) },
            text = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SpawnPointType.entries.forEach { type ->
                        val label =
                            when (type) {
                                SpawnPointType.LAND -> stringResource(Res.string.spawn_point_type_land)
                                SpawnPointType.WATER -> stringResource(Res.string.spawn_point_type_water)
                            }
                        Button(
                            onClick = { onSpawnPointTypeChange(type) },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor =
                                        if (selectedSpawnPointType == type) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        },
                                ),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Text(label, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSpawnPointPropertiesDialog = false }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { showSpawnPointPropertiesDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
