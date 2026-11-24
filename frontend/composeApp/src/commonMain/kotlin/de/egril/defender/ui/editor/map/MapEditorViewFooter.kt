package de.egril.defender.ui.editor.map

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Bottom action row of [MapEditorView] (save, save as, template, cancel) and the community upload controls.
 */
@Composable
internal fun MapEditorViewState.MapEditorViewFooter(
    coroutineScope: CoroutineScope,
    onSave: (EditorMap, String?, ByteArray?) -> Unit,
    onCancel: () -> Unit,
) {
    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = {
                // For user maps: if the name changed, derive a new ID from the new name
                // so that the JSON and PNG filenames match the new name.
                val (newId, oldId) =
                    if (!map.isOfficial && mapName.trim() != map.name.trim()) {
                        val derived = nameToMapId(mapName)
                        if (derived.isNotEmpty() && derived != map.id) Pair(derived, map.id) else Pair(map.id, null)
                    } else {
                        Pair(map.id, null)
                    }
                val updatedMap =
                    map.copy(
                        id = newId,
                        name = mapName,
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
                    )
                // Validate and set readyToUse flag
                val validatedMap = updatedMap.copy(readyToUse = updatedMap.validateReadyToUse())
                onSave(validatedMap, oldId, backgroundImageBytes)
            },
            enabled = !map.isOfficial || de.egril.defender.OfficialEditMode.enabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(Res.string.save_map))
        }

        Button(
            onClick = { showSaveAsDialog = true },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(Res.string.save_as_new))
        }

        Button(
            onClick = {
                templateName = mapName.ifBlank { map.name }
                showSaveTemplateDialog = true
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(Res.string.save_as_template))
        }

        Button(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(Res.string.cancel))
        }
    }

    // Community upload button - only shown for non-official maps when user is authenticated
    val iamState by de.egril.defender.iam.IamService.state
    if (!map.isOfficial && iamState.isAuthenticated) {
        val currentMapJson =
            remember(map.id, mapWidth, mapHeight, tiles.hashCode(), riverTiles.hashCode(), targetInfoMap.hashCode(), spawnPointInfoMap.hashCode(), mapToolingInfo, allowNoBuildableTiles, allowNoDirectPath, tileZones) {
                val updatedMap =
                    map.copy(
                        width = mapWidth,
                        height = mapHeight,
                        tiles = tiles.toMap(),
                        riverTiles = riverTiles.toMap(),
                        targetInfoMap = targetInfoMap.toMap(),
                        spawnPointInfoMap = spawnPointInfoMap.toMap(),
                        mapToolingInfo = mapToolingInfo,
                        allowNoBuildableTiles = allowNoBuildableTiles,
                        allowNoDirectPath = allowNoDirectPath,
                        tileZones = tileZones,
                    )
                de.egril.defender.editor.EditorJsonSerializer
                    .serializeMap(updatedMap)
            }
        val storedCommunityJson =
            remember(map.id) {
                de.egril.defender.editor.EditorStorage
                    .getStoredCommunityMapJson(map.id)
            }
        val storedCommunityMap =
            remember(map.id) {
                de.egril.defender.editor.EditorStorage
                    .getCommunityMap(map.id)
            }
        val isMyUpload = storedCommunityMap?.communityAuthorUsername == iamState.username
        val isChanged = storedCommunityJson != null && storedCommunityJson != currentMapJson

        fun doMapUpload(token: String) {
            isUploadingToCommunity = true
            communityUploadStatus = null
            coroutineScope.launch {
                val success =
                    de.egril.defender.save.BackendCommunityService
                        .uploadCommunityFile("MAP", map.id, currentMapJson, token)
                if (success) {
                    val updatedMap =
                        map.copy(
                            width = mapWidth,
                            height = mapHeight,
                            tiles = tiles.toMap(),
                            riverTiles = riverTiles.toMap(),
                            targetInfoMap = targetInfoMap.toMap(),
                            spawnPointInfoMap = spawnPointInfoMap.toMap(),
                            mapToolingInfo = mapToolingInfo,
                            allowNoBuildableTiles = allowNoBuildableTiles,
                            allowNoDirectPath = allowNoDirectPath,
                            tileZones = tileZones,
                        )
                    de.egril.defender.editor.EditorStorage.saveCommunityMap(
                        updatedMap,
                        iamState.username ?: "",
                    )
                    communityUploadStatus = "success"
                } else {
                    communityUploadStatus = "error"
                }
                isUploadingToCommunity = false
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (storedCommunityJson == null) {
                Button(
                    onClick = { showCommunityUploadConfirm = true },
                    enabled = !isUploadingToCommunity,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (isUploadingToCommunity) {
                            stringResource(Res.string.community_uploading)
                        } else {
                            stringResource(Res.string.upload_as_community_map)
                        },
                    )
                }
            } else if (isMyUpload && isChanged) {
                Button(
                    onClick = {
                        val token =
                            de.egril.defender.iam.IamService
                                .getToken() ?: return@Button
                        doMapUpload(token)
                    },
                    enabled = !isUploadingToCommunity,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        if (isUploadingToCommunity) {
                            stringResource(Res.string.community_uploading)
                        } else {
                            stringResource(Res.string.update_community_map)
                        },
                    )
                }
            }
        }
        communityUploadStatus?.let { status ->
            Text(
                text =
                    if (status == "success") {
                        stringResource(Res.string.community_upload_success)
                    } else {
                        stringResource(Res.string.community_upload_failed)
                    },
                color =
                    if (status == "success") {
                        Color(0xFF2E7D32)
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        // Confirmation dialog before first community map upload
        if (showCommunityUploadConfirm) {
            de.egril.defender.ui.editor.ConfirmationDialog(
                title = stringResource(Res.string.upload_community_confirm_title),
                message = stringResource(Res.string.upload_community_map_confirm_message, iamState.username ?: ""),
                extraContent = {
                    de.egril.defender.ui.editor
                        .CommunityUploadLicenseNotice()
                },
                requireLicenseAgreement = true,
                onDismiss = { showCommunityUploadConfirm = false },
                onConfirm = {
                    showCommunityUploadConfirm = false
                    val token =
                        de.egril.defender.iam.IamService
                            .getToken() ?: return@ConfirmationDialog
                    doMapUpload(token)
                },
            )
        }
    }
}
