package de.egril.defender.ui

import de.egril.defender.ui.settings.applyFromSettingsMap
import de.egril.defender.ui.settings.toSettingsMap
import kotlinx.coroutines.launch

// Download/Upload functionality

fun GameViewModel.downloadSaveGame(
    saveId: String,
    includeGameState: Boolean = false,
) {
    viewModelScope.launch {
        val jsonContent =
            if (includeGameState) {
                de.egril.defender.save.SaveFileStorage
                    .getSaveGameWithWorldMapJson(saveId)
            } else {
                de.egril.defender.save.SaveFileStorage
                    .getSaveGameJson(saveId)
            }

        if (jsonContent != null) {
            val fileExportImport =
                de.egril.defender.save
                    .getFileExportImport()
            val filename =
                if (includeGameState) {
                    "${saveId}_with_progress.json"
                } else {
                    "$saveId.json"
                }
            fileExportImport.exportFile(filename, jsonContent)
        }
    }
}

fun GameViewModel.downloadAllSaveGames(includeGameState: Boolean = false) {
    viewModelScope.launch {
        val allSaves =
            if (includeGameState) {
                // Get all saves with world map included
                de.egril.defender.save.SaveFileStorage
                    .getAllSaveGamesJson()
                    .mapValues { (filename, _) ->
                        val saveId = filename.removeSuffix(".json")
                        de.egril.defender.save.SaveFileStorage
                            .getSaveGameWithWorldMapJson(saveId) ?: ""
                    }.filter { it.value.isNotEmpty() }
            } else {
                de.egril.defender.save.SaveFileStorage
                    .getAllSaveGamesJson()
            }

        if (allSaves.isNotEmpty()) {
            val timestamp =
                de.egril.defender.utils
                    .formatTimestampISO(
                        de.egril.defender.utils
                            .currentTimeMillis(),
                    )
            val zipFilename =
                if (includeGameState) {
                    "defender-of-egril-saves-with-progress-$timestamp.zip"
                } else {
                    "defender-of-egril-saves-$timestamp.zip"
                }
            val fileExportImport =
                de.egril.defender.save
                    .getFileExportImport()
            fileExportImport.exportZip(zipFilename, allSaves)
        }
    }
}

fun GameViewModel.downloadGameState() {
    viewModelScope.launch {
        val jsonContent =
            de.egril.defender.save.SaveFileStorage
                .exportWorldMapProgress()
        val fileExportImport =
            de.egril.defender.save
                .getFileExportImport()
        val timestamp =
            de.egril.defender.utils
                .formatTimestampISO(
                    de.egril.defender.utils
                        .currentTimeMillis(),
                )
        fileExportImport.exportFile("game-progress-$timestamp.json", jsonContent)
    }
}

/**
 * Upload save files and handle override conflicts
 * Returns a state flow with import results: (success count, conflicts)
 */
suspend fun GameViewModel.uploadSaveGames(): Pair<Int, List<String>> {
    val fileExportImport =
        de.egril.defender.save
            .getFileExportImport()
    val importedFiles = fileExportImport.importFiles() ?: return Pair(0, emptyList())

    var successCount = 0
    val conflicts = mutableListOf<String>()

    importedFiles.forEach { file ->
        if (de.egril.defender.save.SaveFileStorage
                .saveGameExists(file.filename)
        ) {
            conflicts.add(file.filename)
        } else {
            if (de.egril.defender.save.SaveFileStorage
                    .importSaveGame(file.filename, file.content, overwrite = false)
            ) {
                successCount++
            }
        }
    }

    refreshSavedGames()
    return Pair(successCount, conflicts)
}

/**
 * Import a specific file with override option
 */
suspend fun GameViewModel.importSaveGameWithOverride(
    filename: String,
    content: String,
    overwrite: Boolean,
): Boolean {
    val success =
        de.egril.defender.save.SaveFileStorage
            .importSaveGame(filename, content, overwrite)
    if (success) {
        refreshSavedGames()
    }
    return success
}

/** Public wrapper so App.kt can trigger a refresh when the IAM state changes (e.g. user logs in). */
fun GameViewModel.onAuthStateChanged() {
    // When the user logs in, link their Keycloak username to the current player profile
    // if no remote username is stored yet for this profile
    val iamState = de.egril.defender.iam.IamService.state.value
    if (iamState.isAuthenticated) {
        val username = iamState.username
        val player = _currentPlayer.value
        if (username != null && player != null && player.remoteUsername == null) {
            // Safety check: if this remote account is already linked to a *different*
            // local player profile, a stale Keycloak session is being reused for the
            // wrong local player.  Terminate that session immediately and abort linking.
            val existingOwner =
                de.egril.defender.save.PlayerProfileStorage
                    .findByRemoteUsername(username)
            if (existingOwner != null && existingOwner.id != player.id) {
                de.egril.defender.iam.IamService
                    .logoutBackchannel()
                return
            }
            de.egril.defender.save.PlayerProfileStorage
                .linkRemoteUser(player.id, username)
            _currentPlayer.value = player.copy(remoteUsername = username)
            // On first SSO login, use the account's first name as the local player name
            val firstName = iamState.firstName
            if (!firstName.isNullOrBlank() && firstName.length <= 50) {
                renameCurrentPlayer(firstName)
            }
        }
        // Download and merge remote user data (abilities, level progress) on login
        downloadAndMergeUserData()
        // Download and apply remote settings independently (dedicated player_settings table)
        downloadAndApplySettings()
    }
    viewModelScope.launch { refreshSavedGames() }
}

/**
 * Persists the "always log in" preference for the current player profile.
 * This is a per-player setting: each local player can independently opt-in to
 * automatic Keycloak login whenever they are the active player.
 */
fun GameViewModel.setAlwaysLogin(value: Boolean) {
    val player = _currentPlayer.value ?: return
    de.egril.defender.save.PlayerProfileStorage
        .saveAlwaysLogin(player.id, value)
    _currentPlayer.value = player.copy(alwaysLogin = value)
}

internal fun GameViewModel.refreshSavedGames() {
    val localGames =
        de.egril.defender.save.SaveFileStorage
            .getAllSavedGames()
    _savedGames.value = localGames
    // Asynchronously enrich with remote save information
    viewModelScope.launch {
        val token =
            de.egril.defender.iam.IamService
                .getToken() ?: return@launch
        _isLoadingRemoteSaves.value = true
        try {
            val remoteFiles =
                de.egril.defender.save.BackendSaveService
                    .fetchSavefiles(token)
                    ?: return@launch
            // Cache all remote file data so loadGame() can import remote-only saves on demand
            remoteFilesCache.clear()
            for (remote in remoteFiles) {
                remoteFilesCache[remote.saveId] = remote.data
            }
            val localIds = localGames.map { it.id }.toSet()
            // Build a map of remote saves by saveId for timestamp comparison
            val remoteById = remoteFiles.associateBy { it.saveId }
            // Build merged list: union of local and remote saves
            val merged = mutableListOf<de.egril.defender.save.SaveGameMetadata>()
            // For each local save, check if a newer version exists remotely
            for (local in localGames) {
                val remote = remoteById[local.id]
                if (remote != null) {
                    // Both local and remote exist for this save ID – prefer the newer one
                    val remoteGame =
                        try {
                            de.egril.defender.save.SaveJsonSerializer
                                .deserializeSavedGame(remote.data)
                        } catch (e: Exception) {
                            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                                println("Failed to parse remote savefile ${remote.saveId}: ${e.message}")
                            }
                            null
                        }
                    if (remoteGame != null && remoteGame.timestamp > local.timestamp) {
                        // Remote version is newer – overwrite local save and use remote metadata
                        de.egril.defender.save.SaveFileStorage.importSaveGame(
                            filename = "${remote.saveId}.json",
                            jsonContent = remote.data,
                            overwrite = true,
                        )
                        val metadata =
                            de.egril.defender.save.SaveFileStorage
                                .buildMetadataFromSavedGame(remoteGame)
                        merged.add(metadata.copy(isLocal = true, isRemote = true))
                    } else {
                        merged.add(local.copy(isLocal = true, isRemote = true))
                    }
                } else {
                    merged.add(local.copy(isLocal = true, isRemote = false))
                }
            }
            // Add remote-only saves (not present locally) using metadata from remote JSON
            for (remote in remoteFiles) {
                if (remote.saveId !in localIds) {
                    val savedGame =
                        try {
                            de.egril.defender.save.SaveJsonSerializer
                                .deserializeSavedGame(remote.data)
                        } catch (e: Exception) {
                            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                                println("Failed to parse remote savefile ${remote.saveId}: ${e.message}")
                            }
                            null
                        }
                    if (savedGame != null) {
                        val metadata =
                            de.egril.defender.save.SaveFileStorage
                                .buildMetadataFromSavedGame(savedGame)
                        merged.add(metadata.copy(isLocal = false, isRemote = true))
                    }
                }
            }
            // Sort by timestamp descending (newest first)
            merged.sortByDescending { it.timestamp }
            _savedGames.value = merged
        } catch (e: Exception) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to fetch remote savefiles: ${e.message}")
            }
        } finally {
            _isLoadingRemoteSaves.value = false
        }
    }
}

internal fun GameViewModel.saveWorldMapStatus() {
    de.egril.defender.save.SaveFileStorage
        .saveWorldMapStatus(_worldLevels.value)
}

/**
 * Uploads the current player's general user data (abilities, level progress, local username)
 * to the backend if the user is currently logged in.
 * Runs in the background so it never blocks gameplay.
 */
internal fun GameViewModel.uploadUserDataToBackend() {
    val token =
        de.egril.defender.iam.IamService
            .getToken()
    if (token == null) {
        if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Skipping userdata upload: not authenticated")
        }
        return
    }
    val player = _currentPlayer.value ?: return
    val levelProgress =
        _worldLevels.value
            .filter { it.level.editorLevelId != null }
            .associate { it.level.editorLevelId!! to it.status.name }

    viewModelScope.launch {
        try {
            val jsonData =
                de.egril.defender.save.serializeUserDataJson(
                    localUsername = player.name,
                    abilities = player.abilities,
                    levelProgress = levelProgress,
                )
            val success =
                de.egril.defender.save.BackendUserDataService
                    .uploadUserData(jsonData, token)
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Userdata upload ${if (success) "succeeded" else "failed"} for player ${player.name}")
            }
        } catch (e: Exception) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to upload userdata to backend: ${e.message}")
            }
        }
    }
}

/**
 * Uploads the current player's settings to the backend independently of userdata.
 * Settings are stored in a dedicated `player_settings` table on the server.
 */
internal fun GameViewModel.uploadSettingsToBackend() {
    val token =
        de.egril.defender.iam.IamService
            .getToken() ?: return
    viewModelScope.launch {
        try {
            val settingsJson =
                de.egril.defender.save.serializeSettingsJson(
                    de.egril.defender.ui.settings.AppSettings
                        .toSettingsMap(),
                )
            val success =
                de.egril.defender.save.BackendSettingsService
                    .uploadSettings(settingsJson, token)
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Settings upload ${if (success) "succeeded" else "failed"}")
            }
        } catch (e: Exception) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to upload settings to backend: ${e.message}")
            }
        }
    }
}

/**
 * Downloads the user's general data from the backend and merges it with the current local
 * state. Remote data wins only when it contains higher XP than the local profile or when
 * a level has a higher status remotely (UNLOCKED or WON) than locally.
 */
private fun GameViewModel.downloadAndMergeUserData() {
    val token =
        de.egril.defender.iam.IamService
            .getToken() ?: return
    viewModelScope.launch {
        try {
            val remote =
                de.egril.defender.save.BackendUserDataService
                    .fetchUserData(token) ?: return@launch
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Downloaded userdata for ${remote.localUsername}")
            }

            var playerUpdated = false

            // Merge abilities: prefer remote when it has higher XP, or when XP is equal but
            // the ability distribution differs (spending points doesn't change XP, so the
            // remote is more recent in that case – last writer wins).
            val player = _currentPlayer.value ?: return@launch
            val remoteAbilities = remote.abilities
            if (remoteAbilities != null &&
                (
                    remoteAbilities.totalXP > player.abilities.totalXP ||
                        (remoteAbilities.totalXP == player.abilities.totalXP && remoteAbilities != player.abilities)
                )
            ) {
                val updatedPlayer = player.copy(abilities = remoteAbilities)
                _currentPlayer.value = updatedPlayer
                de.egril.defender.save.PlayerProfileStorage
                    .updateProfile(updatedPlayer)
                playerUpdated = true
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Merged remote abilities: totalXP=${remoteAbilities.totalXP}")
                }
            }

            // Merge level progress: apply any levels that are WON/UNLOCKED remotely
            // but not yet at that status locally
            val remoteLevelProgress = remote.levelProgress
            if (remoteLevelProgress != null && remoteLevelProgress.isNotEmpty()) {
                val updatedLevels = _worldLevels.value.toMutableList()
                var levelsChanged = false
                for (i in updatedLevels.indices) {
                    val wl = updatedLevels[i]
                    val editorId = wl.level.editorLevelId ?: continue
                    val remoteStatusStr = remoteLevelProgress[editorId] ?: continue
                    val remoteStatus =
                        try {
                            de.egril.defender.model.LevelStatus
                                .valueOf(remoteStatusStr)
                        } catch (_: Exception) {
                            continue
                        }
                    // Only upgrade the status (LOCKED → UNLOCKED → WON), never downgrade
                    if (remoteStatus.ordinal > wl.status.ordinal) {
                        updatedLevels[i] = wl.copy(status = remoteStatus)
                        levelsChanged = true
                    }
                }
                if (levelsChanged) {
                    _worldLevels.value = updatedLevels
                    de.egril.defender.save.SaveFileStorage
                        .saveWorldMapStatus(updatedLevels)
                    if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                        println("Merged remote level progress into local world map")
                    }
                }
            }
        } catch (e: Exception) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to download/merge userdata: ${e.message}")
            }
        }
    }
}

/**
 * Downloads and applies the player's settings from the backend.
 * Only applied when the current player has [useRemoteSettings] set to true.
 * Settings are fetched from the dedicated `player_settings` table, independent of userdata.
 */
private fun GameViewModel.downloadAndApplySettings() {
    val token =
        de.egril.defender.iam.IamService
            .getToken() ?: return
    val player = _currentPlayer.value ?: return
    if (!player.useRemoteSettings) {
        if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Skipping remote settings download: useRemoteSettings=false for player ${player.name}")
        }
        return
    }
    viewModelScope.launch {
        try {
            val remoteSettings =
                de.egril.defender.save.BackendSettingsService
                    .fetchSettings(token)
            if (remoteSettings != null && remoteSettings.isNotEmpty()) {
                de.egril.defender.ui.settings.AppSettings
                    .applyFromSettingsMap(remoteSettings)
                if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("Applied remote settings for player ${player.name}")
                }
            }
        } catch (e: Exception) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to download/apply settings: ${e.message}")
            }
        }
    }
}

/**
 * Persists the "use remote settings" preference for the current player profile.
 */
fun GameViewModel.setUseRemoteSettings(value: Boolean) {
    val player = _currentPlayer.value ?: return
    de.egril.defender.save.PlayerProfileStorage
        .saveUseRemoteSettings(player.id, value)
    _currentPlayer.value = player.copy(useRemoteSettings = value)
}
