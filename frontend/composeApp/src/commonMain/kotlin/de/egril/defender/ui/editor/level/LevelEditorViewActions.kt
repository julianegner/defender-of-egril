package de.egril.defender.ui.editor.level

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperether.resources.stringResource
import de.egril.defender.editor.*
import de.egril.defender.model.DEFAULT_AUTO_TOWER_BASE_EXTRA_COST
import de.egril.defender.ui.common.SelectableText
import defender_of_egril.composeapp.generated.resources.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun LevelEditorViewActions(
    state: LevelEditorViewState,
    level: EditorLevel,
    draftLevel: EditorLevel,
    spawnTimeline: EditorSpawnTimeline,
    spawnPlanValid: Boolean,
    coroutineScope: CoroutineScope,
    onSave: (EditorLevel) -> Unit,
    onCancel: () -> Unit,
) {
    with(state) {
        // Save/Cancel buttons
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!spawnPlanValid) {
                SelectableText(stringResource(Res.string.spawn_fix_errors), color = MaterialTheme.colorScheme.error)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val updatedLevel =
                            level
                                .copy(
                                    title = title,
                                    subtitle = subtitle,
                                    author = author,
                                    communityDescription = communityDescription,
                                    mapId = selectedMapId,
                                    startCoins = startCoins.toIntOrNull() ?: 100,
                                    startHealthPoints = startHP.toIntOrNull() ?: 10,
                                    // Sandbox levels have no scripted enemy waves; the player spawns test enemies while playing.
                                    enemySpawns = emptyList(),
                                    spawnGroups = null,
                                    availableTowers = availableTowersState,
                                    waypoints = waypointsState.toList(),
                                    singleHitTargetOrder = singleHitTargetOrder,
                                    testingOnly = testingOnly,
                                    allowAutoAttack = allowAutoAttack,
                                    connectedToPreviousLevel = connectedToPreviousLevel,
                                    autoTowerBase = autoTowerBase,
                                    autoTowerBaseExtraCost =
                                        autoTowerBaseExtraCost.toIntOrNull()
                                            ?: DEFAULT_AUTO_TOWER_BASE_EXTRA_COST,
                                    isSandbox = isSandbox,
                                    waaghEnabled = waaghEnabled,
                                    supports = supportsState,
                                    events = eventsState,
                                    initialData = initialDataState,
                                ).let { if (isSandbox) it else it.withSpawnTimeline(spawnTimeline) }

                        // Show warning dialog for official levels before saving
                        if (level.isOfficial && de.egril.defender.OfficialEditMode.enabled) {
                            pendingLevelToSave = updatedLevel
                            showOfficialLevelSavedWarning = true
                        } else {
                            // Save immediately if not an official level
                            onSave(updatedLevel)
                        }
                    },
                    enabled = spawnPlanValid && (!level.isOfficial || de.egril.defender.OfficialEditMode.enabled),
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(Res.string.save_level))
                }

                Button(
                    onClick = { showSaveAsDialog = true },
                    enabled = spawnPlanValid,
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(Res.string.save_as_new))
                }

                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(56.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text(stringResource(Res.string.cancel))
                }

                // Community upload button - only shown for non-official levels when user is authenticated
                val iamState by de.egril.defender.iam.IamService.state
                if (!level.isOfficial && iamState.isAuthenticated) {
                    val storedCommunityJson =
                        remember(level.id) {
                            de.egril.defender.editor.EditorStorage
                                .getStoredCommunityLevelJson(level.id)
                        }
                    val currentLevelJson =
                        remember(draftLevel) {
                            de.egril.defender.editor.EditorJsonSerializer
                                .serializeLevel(draftLevel)
                        }
                    val storedCommunityLevel =
                        remember(level.id) {
                            de.egril.defender.editor.EditorStorage
                                .getCommunityLevel(level.id)
                        }
                    val isMyUpload = storedCommunityLevel?.communityAuthorUsername == iamState.username
                    val isChanged = storedCommunityJson != null && storedCommunityJson != currentLevelJson

                    // Check if the map is a user map that needs auto-uploading
                    val levelMap =
                        remember(draftLevel.mapId) {
                            de.egril.defender.editor.EditorStorage
                                .getMap(draftLevel.mapId)
                        }
                    val mapAlsoUploaded =
                        levelMap != null &&
                            !levelMap.isOfficial &&
                            !levelMap.isCommunity &&
                            de.egril.defender.editor.EditorStorage
                                .getCommunityMap(draftLevel.mapId) == null

                    fun doUpload(token: String) {
                        isUploadingToCommunity = true
                        communityUploadStatus = null
                        coroutineScope.launch {
                            val success =
                                de.egril.defender.save.BackendCommunityService
                                    .uploadCommunityFile("LEVEL", level.id, currentLevelJson, token)
                            if (success) {
                                de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                                    draftLevel.copy(
                                        isCommunity = true,
                                        communityAuthorUsername = iamState.username ?: "",
                                    ),
                                )
                                // Auto-upload the map if it hasn't been uploaded yet
                                if (mapAlsoUploaded) {
                                    val map =
                                        de.egril.defender.editor.EditorStorage
                                            .getMap(draftLevel.mapId)
                                    if (map != null) {
                                        val mapJson =
                                            de.egril.defender.editor.EditorJsonSerializer
                                                .serializeMap(map)
                                        val mapSuccess =
                                            de.egril.defender.save.BackendCommunityService
                                                .uploadCommunityFile("MAP", draftLevel.mapId, mapJson, token)
                                        if (mapSuccess) {
                                            de.egril.defender.editor.EditorStorage.saveCommunityMap(
                                                map,
                                                iamState.username ?: "",
                                            )
                                        }
                                    }
                                }
                                communityUploadStatus = "success"
                            } else {
                                communityUploadStatus = "error"
                            }
                            isUploadingToCommunity = false
                        }
                    }

                    if (storedCommunityJson == null) {
                        // Level not yet in community - show upload button
                        Button(
                            onClick = { showCommunityUploadConfirm = true },
                            enabled = spawnPlanValid && !isUploadingToCommunity,
                            modifier = Modifier.weight(1f).height(56.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(
                                if (isUploadingToCommunity) {
                                    stringResource(Res.string.community_uploading)
                                } else {
                                    stringResource(Res.string.upload_as_community_level)
                                },
                            )
                        }
                    } else if (isMyUpload && isChanged) {
                        // Level exists in community and belongs to this user and has been changed - show update button
                        Button(
                            onClick = {
                                val token =
                                    de.egril.defender.iam.IamService
                                        .getToken() ?: return@Button
                                doUpload(token)
                            },
                            enabled = spawnPlanValid && !isUploadingToCommunity,
                            modifier = Modifier.weight(1f).height(56.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                        ) {
                            Text(
                                if (isUploadingToCommunity) {
                                    stringResource(Res.string.community_uploading)
                                } else {
                                    stringResource(Res.string.update_community_level)
                                },
                            )
                        }
                    }
                    // Confirmation dialog before first community upload
                    if (showCommunityUploadConfirm) {
                        val username = iamState.username ?: ""
                        val confirmMessage =
                            if (mapAlsoUploaded) {
                                stringResource(Res.string.upload_community_level_confirm_message, username) +
                                    "\n\n" + stringResource(Res.string.upload_community_also_uploads_map)
                            } else {
                                stringResource(Res.string.upload_community_level_confirm_message, username)
                            }
                        de.egril.defender.ui.editor.ConfirmationDialog(
                            title = stringResource(Res.string.upload_community_confirm_title),
                            message = confirmMessage,
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
                                doUpload(token)
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
                            androidx.compose.ui.graphics
                                .Color(0xFF2E7D32)
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
