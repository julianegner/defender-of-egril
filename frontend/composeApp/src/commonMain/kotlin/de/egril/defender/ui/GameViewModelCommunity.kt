package de.egril.defender.ui

import androidx.compose.runtime.mutableStateOf
import de.egril.defender.audio.GlobalSoundManager
import de.egril.defender.audio.SoundEvent
import de.egril.defender.config.LogConfig
import de.egril.defender.editor.EditorJsonSerializer
import de.egril.defender.editor.EditorLevel
import de.egril.defender.editor.InitialDefender
import de.egril.defender.game.GameEngine
import de.egril.defender.game.LevelData
import de.egril.defender.game.isProtectedByObsidianProtector
import de.egril.defender.game.isProtectedByRuneGlyphWard
import de.egril.defender.game.runeGlyphWardArmor
import de.egril.defender.model.*
import de.egril.defender.ui.animations.SKY_IS_FALLING_DURATION_MS
import de.egril.defender.ui.infopage.NewVersionInfo
import de.egril.defender.ui.infopage.checkForNewerVersions
import de.egril.defender.ui.settings.AppSettings
import de.egril.defender.utils.DeepLink
import de.egril.defender.utils.checkCurrentDeepLink
import de.egril.defender.utils.isPlatformWasm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

fun GameViewModel.downloadCommunityContent() {
    viewModelScope.launch {
        try {
            val communityLevelMeta =
                de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFileList("LEVEL")
            if (communityLevelMeta != null) {
                _remoteCommunityLevelsMeta.value = communityLevelMeta
            }
            val communityMapMeta =
                de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFileList("MAP")
            if (communityMapMeta != null) {
                _remoteCommunityMapsMeta.value = communityMapMeta
            }
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to fetch community content metadata: ${e.message}")
            }
        } finally {
            // Always reload locally-downloaded community levels so they appear in the world map,
            // even when the network is unavailable or the metadata fetch fails.
            de.egril.defender.editor.EditorStorage
                .clearCommunityCache()
            initializeWorldMap()
        }
    }
}

/**
 * Downloads a single community level (and its map if needed) on demand and then
 * reloads the world map via [initializeWorldMap] so the level becomes playable immediately.
 *
 * @param fileInfo Metadata of the community level to download
 * @param onComplete Called when the operation finishes; receives true on success, false on failure.
 *   Note: on success, [initializeWorldMap] has already been called and [worldLevels] will be
 *   updated before [onComplete] is invoked.
 */
fun GameViewModel.downloadCommunityLevelOnDemand(
    fileInfo: de.egril.defender.save.CommunityFileInfo,
    onComplete: (Boolean) -> Unit,
) {
    viewModelScope.launch {
        try {
            val fileData =
                de.egril.defender.save.BackendCommunityService
                    .fetchCommunityFile("LEVEL", fileInfo.fileId)
            if (fileData == null) {
                onComplete(false)
                return@launch
            }
            val level =
                de.egril.defender.editor.EditorJsonSerializer
                    .deserializeLevel(fileData.data)
            if (level == null) {
                onComplete(false)
                return@launch
            }
            de.egril.defender.editor.EditorStorage.saveCommunityLevel(
                level.copy(isCommunity = true, communityAuthorUsername = fileInfo.authorUsername),
            )
            // Also download the map used by this level if not already present locally.
            val mapNeededDownload =
                de.egril.defender.editor.EditorStorage
                    .getMap(level.mapId) == null
            if (mapNeededDownload) {
                downloadCommunityMap(level.mapId)
            }
            // Remove this level from the remote-only list now that it has been downloaded
            // locally, so that the level cards view immediately shows it as a local level
            // instead of keeping it in the "download" state.
            _remoteCommunityLevelsMeta.value =
                _remoteCommunityLevelsMeta.value
                    .filter { it.fileId != fileInfo.fileId }
            // If the community map was also freshly downloaded, remove it from the remote
            // maps list so the map editor list also reflects the local copy.
            if (mapNeededDownload) {
                _remoteCommunityMapsMeta.value =
                    _remoteCommunityMapsMeta.value
                        .filter { it.fileId != level.mapId }
            }
            initializeWorldMap()
            onComplete(true)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to download community level ${fileInfo.fileId}: ${e.message}")
            }
            // If the download failed but the level (and its map) are already stored locally,
            // still reload the world map so the level is surfaced as playable rather than
            // staying in the "remote" card state.
            val alreadyLocal =
                de.egril.defender.editor.EditorStorage
                    .getCommunityLevel(fileInfo.fileId) != null
            if (alreadyLocal) {
                _remoteCommunityLevelsMeta.value =
                    _remoteCommunityLevelsMeta.value
                        .filter { it.fileId != fileInfo.fileId }
                initializeWorldMap()
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }
}

/** Downloads a single community map (JSON + server-generated image) and stores it locally. */
private suspend fun downloadCommunityMap(mapId: String) {
    try {
        val mapData =
            de.egril.defender.save.BackendCommunityService
                .fetchCommunityFile("MAP", mapId)
        if (mapData != null) {
            val map =
                de.egril.defender.editor.EditorJsonSerializer
                    .deserializeMap(mapData.data)
            if (map != null) {
                // Pass the requested mapId so saveCommunityMap can also store the map under
                // that ID when the map's internal JSON id differs (edge case guard).
                if (map.id != mapId) {
                    if (LogConfig.isEnabled { LogConfig.ENABLE_COMMUNITY_DEBUG_LOGGING }) {
                        println(
                            "COMMUNITY-DEBUG: Map id mismatch for download: requested='$mapId', json id='${map.id}'. Saving under both IDs.",
                        )
                    }
                }
                de.egril.defender.editor.EditorStorage.saveCommunityMap(
                    map,
                    mapData.authorUsername,
                    requestedId = mapId,
                )
                // Try to fetch the server-generated image and overwrite the locally-generated one
                val imageBytes =
                    de.egril.defender.save.BackendCommunityService
                        .fetchCommunityMapImage(mapId)
                if (imageBytes != null) {
                    de.egril.defender.editor.getFileStorage().writeBinaryFile(
                        "gamedata/community/maps/$mapId.png",
                        imageBytes,
                    )
                }
                // Zone overlays are generated by the backend too; overwrite the local copies
                map.tileZones.indices.forEach { zoneIndex ->
                    val zoneBytes =
                        de.egril.defender.save.BackendCommunityService
                            .fetchCommunityMapZoneImage(mapId, zoneIndex)
                    if (zoneBytes != null) {
                        de.egril.defender.editor.getFileStorage().writeBinaryFile(
                            "gamedata/community/maps/${de.egril.defender.ui.MapImageProvider.tileZoneImageFileName(map.id, zoneIndex)}",
                            zoneBytes,
                        )
                    }
                }
            }
        }
    } catch (e: Exception) {
        if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Failed to download community map $mapId: ${e.message}")
        }
    }
}

/**
 * Downloads a single community map (JSON + server-generated image) on demand from the map editor.
 * Clears the community cache after saving so the editor list refreshes.
 *
 * @param fileInfo Metadata of the community map to download
 * @param onComplete Called when the operation finishes; receives true on success, false on failure
 */
fun GameViewModel.downloadCommunityMapOnDemand(
    fileInfo: de.egril.defender.save.CommunityFileInfo,
    onComplete: (Boolean) -> Unit,
) {
    viewModelScope.launch {
        try {
            downloadCommunityMap(fileInfo.fileId)
            de.egril.defender.editor.EditorStorage
                .clearCommunityCache()
            onComplete(true)
        } catch (e: Exception) {
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to download community map on demand ${fileInfo.fileId}: ${e.message}")
            }
            onComplete(false)
        }
    }
}

/**
 * Uploads a user level to the community backend.
 * If the level's map is a user map that has not yet been uploaded to the community,
 * the map is uploaded first so that other players can download it together with the level.
 * @param levelId The ID of the level to upload
 * @param token Bearer token for authentication
 * @return true on success, false on failure
 */
suspend fun uploadCommunityLevel(
    levelId: String,
    token: String,
): Boolean {
    val level =
        de.egril.defender.editor.EditorStorage
            .getLevel(levelId) ?: return false
    val username = de.egril.defender.iam.IamService.state.value.username ?: ""

    // Auto-upload the associated map if it is a user map not yet in the community
    val map =
        de.egril.defender.editor.EditorStorage
            .getMap(level.mapId)
    if (map != null && !map.isOfficial && !map.isCommunity) {
        val communityMap =
            de.egril.defender.editor.EditorStorage
                .getCommunityMap(level.mapId)
        if (communityMap == null) {
            // Map not yet uploaded – upload it now
            val mapJson =
                de.egril.defender.editor.EditorJsonSerializer
                    .serializeMap(map)
            val mapSuccess =
                de.egril.defender.save.BackendCommunityService
                    .uploadCommunityFile("MAP", level.mapId, mapJson, token)
            if (mapSuccess) {
                de.egril.defender.editor.EditorStorage
                    .saveCommunityMap(map, username)
            }
            // Proceed with the level upload even if the map upload failed; callers
            // can inspect the level-upload result and show an appropriate message.
        }
    }

    val json =
        de.egril.defender.editor.EditorJsonSerializer
            .serializeLevel(level)
    val success =
        de.egril.defender.save.BackendCommunityService
            .uploadCommunityFile("LEVEL", levelId, json, token, level.communityDescription)
    if (success) {
        // Store the uploaded version locally in the community directory so we can detect changes
        de.egril.defender.editor.EditorStorage.saveCommunityLevel(
            level.copy(
                isCommunity = true,
                communityAuthorUsername = username,
            ),
        )
    }
    return success
}

/**
 * Uploads a user map to the community backend.
 * @param mapId The ID of the map to upload
 * @param token Bearer token for authentication
 * @return true on success, false on failure
 */
suspend fun uploadCommunityMap(
    mapId: String,
    token: String,
): Boolean {
    val map =
        de.egril.defender.editor.EditorStorage
            .getMap(mapId) ?: return false
    val json =
        de.egril.defender.editor.EditorJsonSerializer
            .serializeMap(map)
    val success =
        de.egril.defender.save.BackendCommunityService
            .uploadCommunityFile("MAP", mapId, json, token)
    if (success) {
        val username = de.egril.defender.iam.IamService.state.value.username ?: ""
        de.egril.defender.editor.EditorStorage
            .saveCommunityMap(map, username)
    }
    return success
}
