@file:OptIn(org.jetbrains.compose.resources.InternalResourceApi::class)

package de.egril.defender.ui.editor.worldmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.ConnectionType
import de.egril.defender.editor.WorldMapData
import de.egril.defender.editor.WorldMapLocationData
import de.egril.defender.editor.WorldMapPathData
import de.egril.defender.editor.WorldMapPoint
import de.egril.defender.ui.icon.RightArrowIcon
import defender_of_egril.composeapp.generated.resources.*
import defender_of_egril.composeapp.generated.resources.Res

/**
 * Dialog for editing path control points
 */
@Composable
internal fun EditPathDialog(
    path: WorldMapPathData,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapPathData) -> Unit,
) {
    var controlPoints by remember { mutableStateOf(path.controlPoints.toMutableList()) }
    var connectionType by remember { mutableStateOf(path.type) }
    var segmentTypes by remember {
        mutableStateOf(
            if (path.segmentTypes.isEmpty()) {
                // Initialize with default type for all segments
                List(path.getSegmentCount()) { connectionType }
            } else {
                path.segmentTypes.toMutableList()
            },
        )
    }
    var newX by remember { mutableStateOf("") }
    var newY by remember { mutableStateOf("") }

    // Update segment types when controlPoints change
    LaunchedEffect(controlPoints.size) {
        val newSegmentCount = controlPoints.size + 1
        if (segmentTypes.size != newSegmentCount) {
            segmentTypes =
                List(newSegmentCount) { index ->
                    segmentTypes.getOrElse(index) { connectionType }
                }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_connection)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "Connection: ${path.fromLocationId}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                RightArrowIcon(size = 14.dp)
                Text(
                    path.toLocationId,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Default connection type selector
                Text(stringResource(Res.string.default_connection_type), style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            connectionType = ConnectionType.ROAD
                            // Update all segments to new default
                            segmentTypes = List(segmentTypes.size) { ConnectionType.ROAD }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (connectionType == ConnectionType.ROAD) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.all_road))
                    }
                    Button(
                        onClick = {
                            connectionType = ConnectionType.SEA_ROUTE
                            // Update all segments to new default
                            segmentTypes = List(segmentTypes.size) { ConnectionType.SEA_ROUTE }
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (connectionType == ConnectionType.SEA_ROUTE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.all_sea))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Segment types editor
                Text(stringResource(Res.string.segment_types, segmentTypes.size), style = MaterialTheme.typography.bodySmall)
                Text(
                    "Edit individual segment types for mixed road/sea connections",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )

                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    segmentTypes.forEachIndexed { index, type ->
                        val fromPoint = if (index == 0) path.fromLocationId else "waypoint $index"
                        val toPoint = if (index == segmentTypes.size - 1) path.toLocationId else "waypoint ${index + 1}"

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    "Seg ${index + 1}: $fromPoint",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                RightArrowIcon(size = 12.dp)
                                Text(
                                    toPoint,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        segmentTypes =
                                            segmentTypes.toMutableList().apply {
                                                set(index, ConnectionType.ROAD)
                                            }
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (type == ConnectionType.ROAD) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                        ),
                                    modifier = Modifier.size(60.dp, 32.dp),
                                    contentPadding = PaddingValues(4.dp),
                                ) {
                                    Text(stringResource(Res.string.road), fontSize = 10.sp)
                                }
                                Button(
                                    onClick = {
                                        segmentTypes =
                                            segmentTypes.toMutableList().apply {
                                                set(index, ConnectionType.SEA_ROUTE)
                                            }
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor =
                                                if (type == ConnectionType.SEA_ROUTE) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                        ),
                                    modifier = Modifier.size(60.dp, 32.dp),
                                    contentPadding = PaddingValues(4.dp),
                                ) {
                                    Text(stringResource(Res.string.sea), fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(Res.string.waypoints_range), style = MaterialTheme.typography.bodySmall)

                LazyColumn(
                    modifier = Modifier.height(120.dp),
                ) {
                    items(controlPoints.size) { index ->
                        val cp = controlPoints[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${index + 1}: (${cp.x}, ${cp.y})")
                            TextButton(onClick = {
                                controlPoints = controlPoints.toMutableList().apply { removeAt(index) }
                            }) {
                                Text("X", color = Color.Red)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(Res.string.add_control_point), style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newX,
                        onValueChange = { newX = it.filter { c -> c.isDigit() } },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = newY,
                        onValueChange = { newY = it.filter { c -> c.isDigit() } },
                        label = { Text(stringResource(Res.string.y_coordinate)) },
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            val x = newX.toIntOrNull()?.coerceIn(0, 1000)
                            val y = newY.toIntOrNull()?.coerceIn(0, 1000)
                            if (x != null && y != null) {
                                controlPoints = controlPoints.toMutableList().apply { add(WorldMapPoint(x, y)) }
                                newX = ""
                                newY = ""
                            }
                        },
                    ) {
                        Text("+")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        path.copy(
                            controlPoints = controlPoints,
                            type = connectionType,
                            segmentTypes = segmentTypes,
                        ),
                    )
                },
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

/**
 * Dialog for adding a new connection between two locations
 */
@Composable
internal fun AddConnectionDialog(
    locations: List<WorldMapLocationData>,
    onDismiss: () -> Unit,
    onConfirm: (WorldMapPathData) -> Unit,
) {
    var fromLocationId by remember { mutableStateOf<String?>(null) }
    var toLocationId by remember { mutableStateOf<String?>(null) }
    var connectionType by remember { mutableStateOf(ConnectionType.ROAD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.add_connection)) },
        text = {
            Column {
                Text(stringResource(Res.string.select_two_locations), style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                // From location selector
                Text(stringResource(Res.string.from), style = MaterialTheme.typography.bodySmall)
                LazyColumn(
                    modifier = Modifier.height(120.dp).padding(bottom = 8.dp),
                ) {
                    items(locations) { location ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { fromLocationId = location.id }
                                    .padding(8.dp)
                                    .background(
                                        if (fromLocationId == location.id) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = fromLocationId == location.id,
                                onClick = { fromLocationId = location.id },
                            )
                            Text(location.name)
                        }
                    }
                }

                // To location selector
                Text(stringResource(Res.string.to), style = MaterialTheme.typography.bodySmall)
                LazyColumn(
                    modifier = Modifier.height(120.dp).padding(bottom = 8.dp),
                ) {
                    items(locations) { location ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { toLocationId = location.id }
                                    .padding(8.dp)
                                    .background(
                                        if (toLocationId == location.id) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = toLocationId == location.id,
                                onClick = { toLocationId = location.id },
                            )
                            Text(location.name)
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Connection type selector
                Text(stringResource(Res.string.connection_type), style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { connectionType = ConnectionType.ROAD },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (connectionType == ConnectionType.ROAD) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.road))
                    }
                    Button(
                        onClick = { connectionType = ConnectionType.SEA_ROUTE },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor =
                                    if (connectionType == ConnectionType.SEA_ROUTE) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                            ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(Res.string.sea))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fromLocationId != null && toLocationId != null && fromLocationId != toLocationId) {
                        onConfirm(
                            WorldMapPathData(
                                fromLocationId = fromLocationId!!,
                                toLocationId = toLocationId!!,
                                controlPoints = emptyList(),
                                type = connectionType,
                            ),
                        )
                    }
                },
                enabled = fromLocationId != null && toLocationId != null && fromLocationId != toLocationId,
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

internal fun WorldMapData.withUpdatedPath(path: WorldMapPathData): WorldMapData {
    val existingIndex =
        paths.indexOfFirst {
            it.fromLocationId == path.fromLocationId && it.toLocationId == path.toLocationId
        }
    val updatedPaths =
        if (existingIndex >= 0) {
            paths.mapIndexed { index, existing -> if (index == existingIndex) path else existing }
        } else {
            paths + path
        }
    return copy(paths = updatedPaths)
}

internal fun WorldMapData.withUpdatedLocation(location: WorldMapLocationData): WorldMapData {
    val existingIndex = locations.indexOfFirst { it.id == location.id }
    val updatedLocations =
        if (existingIndex >= 0) {
            locations.mapIndexed { index, existing -> if (index == existingIndex) location else existing }
        } else {
            locations + location
        }
    return copy(locations = updatedLocations)
}
