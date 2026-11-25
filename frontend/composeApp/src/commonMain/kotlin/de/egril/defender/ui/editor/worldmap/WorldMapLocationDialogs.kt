@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPoint
import de.egril.defender.ui.IconGridScrollbar
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * Dialog for adding a new location
 */
@Composable
internal fun AddLocationDialog(
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    existingLocations: List<WorldMapLocationData>,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapLocationData) -> Unit,
    preselectedLevelIds: List<String> = emptyList(),
) {
    var name by remember { mutableStateOf("") }
    var selectedLevelIds by remember { mutableStateOf(preselectedLevelIds.toSet()) }
    var selectedIconResourceName by remember { mutableStateOf<String?>(null) }

    // Get levels not yet assigned to any location (preselected levels stay visible)
    val assignedLevelIds = existingLocations.flatMap { it.levelIds }.toSet()
    val availableLevels = allLevels.filter { it.id !in assignedLevelIds || it.id in preselectedLevelIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_location_dialog_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.location_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(Res.string.select_levels), style = MaterialTheme.typography.bodyMedium)

                LazyColumn(
                    modifier = Modifier.height(150.dp),
                ) {
                    items(availableLevels) { level ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLevelIds =
                                            if (level.id in selectedLevelIds) {
                                                selectedLevelIds - level.id
                                            } else {
                                                selectedLevelIds + level.id
                                            }
                                    }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = level.id in selectedLevelIds,
                                onCheckedChange = {
                                    selectedLevelIds =
                                        if (it) {
                                            selectedLevelIds + level.id
                                        } else {
                                            selectedLevelIds - level.id
                                        }
                                },
                            )
                            Text(level.title.ifEmpty { level.id })
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Icon Selection Section
                Text(stringResource(Res.string.location_icon_optional), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Current icon preview
                if (selectedIconResourceName != null) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val iconPainter = LocationIconUtils.loadIconPainter(selectedIconResourceName)
                            if (iconPainter != null) {
                                Image(
                                    painter = iconPainter,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                                Text(
                                    text = LocationIconUtils.getIconDisplayName(selectedIconResourceName!!),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        TextButton(
                            onClick = { selectedIconResourceName = null },
                        ) {
                            Text(stringResource(Res.string.remove))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.no_icon_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Icon selection grid/slider
                Text(stringResource(Res.string.available_icons), style = MaterialTheme.typography.bodySmall)
                val iconGridState = rememberLazyGridState()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        state = iconGridState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(LocationIconUtils.AVAILABLE_LOCATION_ICONS) { iconName ->
                            val iconPainter = LocationIconUtils.loadIconPainter(iconName)
                            if (iconPainter != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedIconResourceName = iconName },
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color =
                                            if (selectedIconResourceName == iconName) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        shadowElevation = if (selectedIconResourceName == iconName) 4.dp else 1.dp,
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(8.dp),
                                        ) {
                                            Image(
                                                painter = iconPainter,
                                                contentDescription = LocationIconUtils.getIconDisplayName(iconName),
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                    Text(
                                        text = LocationIconUtils.getIconDisplayName(iconName),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    IconGridScrollbar(iconGridState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedLevelIds.isNotEmpty()) {
                        val locationId = name.lowercase().replace(" ", "_")
                        onConfirm(
                            WorldMapLocationData(
                                id = locationId,
                                name = name,
                                position = WorldMapPoint(500, 500), // Center by default
                                levelIds = selectedLevelIds.toList(),
                                iconResourceName = selectedIconResourceName,
                            ),
                        )
                    }
                },
                enabled = name.isNotBlank() && selectedLevelIds.isNotEmpty(),
            ) {
                Text(stringResource(Res.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

/**
 * Dialog for editing an existing location
 */
@Composable
internal fun EditLocationDialog(
    location: WorldMapLocationData,
    allLevels: List<de.egril.defender.editor.EditorLevel>,
    existingLocations: List<WorldMapLocationData>,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapLocationData) -> Unit,
) {
    var name by remember { mutableStateOf(location.name) }
    var selectedLevelIds by remember { mutableStateOf(location.levelIds.toSet()) }
    var selectedIconResourceName by remember { mutableStateOf(location.iconResourceName) }

    // Get levels not yet assigned to other locations (excluding current location)
    val assignedLevelIds =
        existingLocations
            .filter { it.id != location.id }
            .flatMap { it.levelIds }
            .toSet()
    val availableLevels = allLevels.filter { it.id !in assignedLevelIds || it.id in location.levelIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_location)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.location_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(stringResource(Res.string.select_levels), style = MaterialTheme.typography.bodyMedium)

                LazyColumn(
                    modifier = Modifier.height(150.dp),
                ) {
                    items(availableLevels) { level ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedLevelIds =
                                            if (level.id in selectedLevelIds) {
                                                selectedLevelIds - level.id
                                            } else {
                                                selectedLevelIds + level.id
                                            }
                                    }.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = level.id in selectedLevelIds,
                                onCheckedChange = {
                                    selectedLevelIds =
                                        if (it) {
                                            selectedLevelIds + level.id
                                        } else {
                                            selectedLevelIds - level.id
                                        }
                                },
                            )
                            Text(level.title.ifEmpty { level.id })
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Icon Selection Section
                Text(stringResource(Res.string.location_icon_optional), style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Current icon preview
                if (selectedIconResourceName != null) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val iconPainter = LocationIconUtils.loadIconPainter(selectedIconResourceName)
                            if (iconPainter != null) {
                                Image(
                                    painter = iconPainter,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                                Text(
                                    text = LocationIconUtils.getIconDisplayName(selectedIconResourceName!!),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            } else {
                                Text(
                                    text = stringResource(Res.string.invalid_icon),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Red,
                                )
                            }
                        }
                        TextButton(
                            onClick = { selectedIconResourceName = null },
                        ) {
                            Text(stringResource(Res.string.remove))
                        }
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.no_icon_selected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Icon selection grid/slider
                Text(stringResource(Res.string.available_icons), style = MaterialTheme.typography.bodySmall)
                val iconGridState = rememberLazyGridState()
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(vertical = 8.dp),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 72.dp),
                        state = iconGridState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(LocationIconUtils.AVAILABLE_LOCATION_ICONS) { iconName ->
                            val iconPainter = LocationIconUtils.loadIconPainter(iconName)
                            if (iconPainter != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.clickable { selectedIconResourceName = iconName },
                                ) {
                                    Surface(
                                        modifier = Modifier.size(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        color =
                                            if (selectedIconResourceName == iconName) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        shadowElevation = if (selectedIconResourceName == iconName) 4.dp else 1.dp,
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(8.dp),
                                        ) {
                                            Image(
                                                painter = iconPainter,
                                                contentDescription = LocationIconUtils.getIconDisplayName(iconName),
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                    Text(
                                        text = LocationIconUtils.getIconDisplayName(iconName),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                    IconGridScrollbar(iconGridState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedLevelIds.isNotEmpty()) {
                        onConfirm(
                            location.copy(
                                name = name,
                                levelIds = selectedLevelIds.toList(),
                                iconResourceName = selectedIconResourceName,
                            ),
                        )
                    }
                },
                enabled = name.isNotBlank() && selectedLevelIds.isNotEmpty(),
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
