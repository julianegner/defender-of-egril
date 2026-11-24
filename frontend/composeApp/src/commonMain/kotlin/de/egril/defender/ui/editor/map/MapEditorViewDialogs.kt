package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.Position
import de.egril.defender.model.TargetType
import de.egril.defender.ui.editor.SaveAsDialog
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.CoroutineScope

/**
 * Modal dialogs of [MapEditorView]: map image generation, river ring fill, save as, save as template
 * and the target name/type editor.
 */
@Composable
internal fun MapEditorViewState.MapEditorViewDialogs(
    currentMap: EditorMap,
    coroutineScope: CoroutineScope,
    onSave: (EditorMap, String?, ByteArray?) -> Unit,
) {
    LaunchedEffect(showMapImageGenerationDialog) {
        if (showMapImageGenerationDialog && shouldLoadMapPreviewFromDisk(mapPreviewPainter != null)) {
            mapPreviewPainter = loadMapPreviewFromDisk(currentMap)
        }
        if (showMapImageGenerationDialog) {
            mapImagePreviews = loadMapImagePreviews(mapImageFileNames(currentMap))
            mapImagePreviewIndex = 0
        }
    }

    if (showMapImageGenerationDialog) {
        AlertDialog(
            onDismissRequest = { if (!mapImageGenerationRunning) showMapImageGenerationDialog = false },
            title = { Text(stringResource(Res.string.map_image_generation_title, currentMap.name)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MapImageProgressRows(mapImageProgress)
                    if (mapImageGenerationSuccess == false) {
                        Text(stringResource(Res.string.map_image_generation_error, mapImageGenerationError ?: ""))
                    }
                    if (mapImagePreviews.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(Res.string.map_image_generation_preview_label))
                            MapImageCarousel(
                                previews = mapImagePreviews,
                                index = mapImagePreviewIndex.coerceIn(0, mapImagePreviews.lastIndex),
                                onIndexChange = { mapImagePreviewIndex = it },
                            )
                        }
                    }
                    if (currentMap.tileZones.any { it.movable }) {
                        MovableZoneImageHint()
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = { regenerateMapPreview(currentMap, coroutineScope) },
                        enabled = !mapImageGenerationRunning,
                    ) {
                        Text(stringResource(Res.string.regenerate_map_image))
                    }
                    TextButton(onClick = { showMapImageGenerationDialog = false }) {
                        Text(stringResource(Res.string.map_image_generation_close))
                    }
                }
            },
        )
    }

    // The modal dialog would swallow map clicks, so it is hidden while the start tile is being picked
    if (showFillRiverRingDialog && pickTarget != PositionPickTarget.RING_START) {
        AlertDialog(
            onDismissRequest = {
                if (pickTarget == PositionPickTarget.RING_START) pickTarget = null
                showFillRiverRingDialog = false
            },
            title = { Text(stringResource(Res.string.fill_river_ring_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ringStartX,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("-?[0-9]+"))) ringStartX = it },
                        label = { Text(stringResource(Res.string.ring_start_x)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = ringStartY,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("-?[0-9]+"))) ringStartY = it },
                        label = { Text(stringResource(Res.string.ring_start_y)) },
                        singleLine = true,
                    )
                    PositionPickButton(
                        active = false,
                        onClick = { pickTarget = PositionPickTarget.RING_START },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ringClockwise, onClick = { ringClockwise = true }, label = { Text(stringResource(Res.string.clockwise)) })
                        FilterChip(selected = !ringClockwise, onClick = { ringClockwise = false }, label = { Text(stringResource(Res.string.counterclockwise)) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = ringInnerToOuter, onClick = { ringInnerToOuter = true }, label = { Text(stringResource(Res.string.inner_to_outer)) })
                        FilterChip(selected = !ringInnerToOuter, onClick = { ringInnerToOuter = false }, label = { Text(stringResource(Res.string.outer_to_inner)) })
                    }
                    Text(
                        text = stringResource(Res.string.fill_river_ring_hint),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (fillRiverRingError) {
                        Text(
                            text = stringResource(Res.string.fill_river_ring_error_not_water),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val startX = ringStartX.toIntOrNull() ?: return@Button
                    val startY = ringStartY.toIntOrNull() ?: return@Button
                    val startKey = "$startX,$startY"
                    if (tiles[startKey] != TileType.RIVER) {
                        fillRiverRingError = true
                        return@Button
                    }
                    fillRiverRingError = false
                    val updated =
                        applyRiverRing(
                            tiles = tiles,
                            riverTiles = riverTiles,
                            mapWidth = mapWidth,
                            mapHeight = mapHeight,
                            start = Position(startX, startY),
                            clockwise = ringClockwise,
                            innerToOuter = ringInnerToOuter,
                            flowSpeed = selectedRiverSpeed,
                        )
                    if (updated.second != riverTiles) {
                        rememberForUndo()
                        tiles = updated.first
                        riverTiles = updated.second
                    }
                    if (pickTarget == PositionPickTarget.RING_START) pickTarget = null
                    showFillRiverRingDialog = false
                }) { Text(stringResource(Res.string.apply)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    fillRiverRingError = false
                    if (pickTarget == PositionPickTarget.RING_START) pickTarget = null
                    showFillRiverRingDialog = false
                }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            title = stringResource(Res.string.save_map_as_new),
            label = stringResource(Res.string.map_name),
            currentValue = mapName,
            onDismiss = { showSaveAsDialog = false },
            onSave = { newName ->
                // Save as new map with ID based on name
                val newId =
                    nameToMapId(newName).ifEmpty {
                        "map_copy_${kotlin.random.Random.nextInt(10000, 99999)}"
                    }
                val newMap =
                    map.copy(
                        id = newId,
                        name = newName,
                        author = mapAuthor,
                        mapToolingInfo = mapToolingInfo,
                        allowNoBuildableTiles = allowNoBuildableTiles,
                        allowNoDirectPath = allowNoDirectPath,
                        tileZones = tileZones,
                        width = mapWidth,
                        height = mapHeight,
                        tiles = tiles.toMap(),
                        riverTiles = riverTiles.toMap(),
                        targetInfoMap = targetInfoMap.toMap(),
                        spawnPointInfoMap = spawnPointInfoMap.toMap(),
                        isOfficial = false, // Save as new always creates a user map
                    )
                // Validate and set readyToUse flag
                val validatedMap = newMap.copy(readyToUse = newMap.validateReadyToUse())
                onSave(validatedMap, null, backgroundImageBytes) // null oldId: this is a brand-new map, not a rename
                showSaveAsDialog = false
            },
        )
    }

    if (showSaveTemplateDialog) {
        SaveAsDialog(
            title = stringResource(Res.string.save_map_template),
            label = stringResource(Res.string.template_name),
            currentValue = templateName,
            onDismiss = { showSaveTemplateDialog = false },
            onSave = { newName ->
                val templateId = createTemplateId(newName.ifBlank { mapName.ifBlank { map.name } })
                EditorStorage.saveMapTemplate(
                    MapTemplateDefinition(
                        id = templateId,
                        name = newName.ifBlank { mapName.ifBlank { map.name } },
                        templateMap =
                            currentMap.copy(
                                id = templateId,
                                name = newName.ifBlank { mapName.ifBlank { map.name } },
                                isOfficial = false,
                            ),
                    ),
                )
                showSaveTemplateDialog = false
            },
        )
    }

    // Edit target dialog - opens when clicking an existing TARGET tile while in TARGET mode
    editTargetKey?.let { editKey ->
        val existingInfo = targetInfoMap[editKey]
        var editName by remember(editKey) { mutableStateOf(existingInfo?.name ?: "") }
        var editType by remember(editKey) { mutableStateOf(existingInfo?.type ?: TargetType.STANDARD) }
        AlertDialog(
            onDismissRequest = { editTargetKey = null },
            title = { Text(stringResource(Res.string.target_name_label)) },
            text = {
                Column(
                    verticalArrangement =
                        androidx.compose.foundation.layout.Arrangement
                            .spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(stringResource(Res.string.target_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Text(stringResource(Res.string.target_type_label), style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(4.dp),
                    ) {
                        TargetType.entries.forEach { type ->
                            val label =
                                when (type) {
                                    TargetType.STANDARD -> stringResource(Res.string.target_type_standard)
                                    TargetType.SINGLE_HIT -> stringResource(Res.string.target_type_single_hit)
                                }
                            Button(
                                onClick = { editType = type },
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor =
                                            if (editType == type) {
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
                    rememberForUndo()
                    targetInfoMap =
                        targetInfoMap.toMutableMap().apply {
                            this[editKey] = EditorTargetInfo(name = editName, type = editType)
                        }
                    editTargetKey = null
                }) {
                    Text(stringResource(Res.string.ok))
                }
            },
            dismissButton = {
                Button(onClick = { editTargetKey = null }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}
