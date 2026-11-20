package de.egril.defender.ui

import de.egril.defender.config.LogConfig
import de.egril.defender.game.GameEngine
import de.egril.defender.model.GameState
import de.egril.defender.ui.GameViewModel.Companion.BACKGROUND_SAVE_ID
import kotlinx.coroutines.launch

fun GameViewModel.navigateToLoadGame() {
    refreshSavedGames()
    _currentScreen.value = Screen.LoadGame
}

fun GameViewModel.saveCurrentGame(comment: String? = null): String? {
    val state = _gameState.value ?: return null
    val saveId =
        de.egril.defender.save.SaveFileStorage
            .saveGameState(state, comment)
    refreshSavedGames()
    // Update the last save snapshot
    lastSaveSnapshot = createGameStateSnapshot(state)
    // Upload to backend in background if the user is logged in
    uploadSavefileToBackend(saveId)
    return saveId
}

/**
 * Create an autosave at the beginning of a new turn.
 * Autosaves always use the fixed ID "autosave_game" so they overwrite previous autosaves.
 */
internal fun GameViewModel.autoSaveGame() {
    val state = _gameState.value ?: return
    // Use fixed ID "autosave_game" and add "Autosave" as comment
    de.egril.defender.save.SaveFileStorage
        .saveGameState(state, comment = "Autosave", saveId = "autosave_game")
    refreshSavedGames()
    // Upload autosave to backend in background if the user is logged in
    uploadSavefileToBackend("autosave_game")
    // Don't update lastSaveSnapshot for autosaves - we still want to track manual saves separately
}

/**
 * Uploads a locally-saved game to the backend if the user is currently logged in.
 * Runs in the background so it never blocks gameplay.
 */
private fun GameViewModel.uploadSavefileToBackend(saveId: String) {
    val token =
        de.egril.defender.iam.IamService
            .getToken()
    if (token == null) {
        if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Skipping backend upload for $saveId: not authenticated")
        }
        return
    }
    viewModelScope.launch {
        val json =
            de.egril.defender.save.SaveFileStorage
                .getSaveGameJson(saveId) ?: return@launch
        try {
            val success =
                de.egril.defender.save.BackendSaveService
                    .uploadSavefile(saveId, json, token)
            if (success) {
                // Re-merge so the card shows the "remote" chip
                refreshSavedGames()
            }
        } catch (e: Exception) {
            if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Failed to upload savefile $saveId to backend: ${e.message}")
            }
        }
    }
}

/**
 * Imports a savefile from [remoteFilesCache] into local storage and loads it.
 * Returns the deserialized [SavedGame] on success, null if the save ID is not in the
 * cache or the import fails.
 */
private fun GameViewModel.importAndLoadFromRemoteCache(saveId: String): de.egril.defender.save.SavedGame? {
    val remoteJson = remoteFilesCache[saveId] ?: return null
    val imported =
        de.egril.defender.save.SaveFileStorage.importSaveGame(
            filename = "$saveId.json",
            jsonContent = remoteJson,
            overwrite = true,
        )
    if (!imported) {
        if (de.egril.defender.config.LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
            println("Failed to import remote savefile $saveId into local storage")
        }
        return null
    }
    return de.egril.defender.save.SaveFileStorage
        .loadGameState(saveId)
}

/**
 * Check if an autosave exists
 */
fun GameViewModel.hasAutosave(): Boolean =
    de.egril.defender.save.SaveFileStorage
        .loadGameState("autosave_game") != null

/**
 * Load the autosave and start playing
 */
fun GameViewModel.continueFromAutosave() {
    loadGame("autosave_game")
}

/**
 * Save the current game state to the background save slot.
 * Called when the app goes to the background (onPause) while in gameplay.
 * Uses a fixed ID so the previous background save is always overwritten.
 */
fun GameViewModel.saveGameOnBackground() {
    if (_currentScreen.value !is Screen.GamePlay) return
    val state = _gameState.value ?: return
    de.egril.defender.save.SaveFileStorage.saveGameState(
        state,
        comment = "Background save",
        saveId = BACKGROUND_SAVE_ID,
    )
}

/**
 * Restore the game from the background save if one exists.
 * Called on ViewModel initialization to resume gameplay after the process was killed
 * while the app was in the background.
 */
internal fun GameViewModel.restoreBackgroundSaveIfExists() {
    if (de.egril.defender.save.SaveFileStorage
            .loadGameState(BACKGROUND_SAVE_ID) == null
    ) {
        return
    }
    loadGame(BACKGROUND_SAVE_ID)
    val restored = _currentScreen.value is Screen.GamePlay
    if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
        if (restored) {
            println("Background save restored successfully.")
        } else {
            println("Background save exists but could not be restored (level may no longer be available).")
        }
    }
    // Always delete the background save after attempting restore to prevent it
    // from being re-applied on subsequent cold starts (infinite retry loop).
    // A fresh background save will be created on the next onPause while in gameplay.
    deleteBackgroundSave()
}

/**
 * Delete the background save file, if it exists.
 * Called when the player explicitly leaves the game (back to map, level complete).
 */
internal fun GameViewModel.deleteBackgroundSave() {
    de.egril.defender.save.SaveFileStorage
        .deleteSavedGame(BACKGROUND_SAVE_ID)
}

/**
 * Create a snapshot of the current game state for change detection
 */
internal fun GameViewModel.createGameStateSnapshot(state: GameState): String {
    // Create a simple hash/snapshot of key game state properties
    return buildString {
        append("turn:${state.turnNumber.value}")
        append("|coins:${state.coins.value}")
        append("|health:${state.healthPoints.value}")
        append("|phase:${state.phase.value}")
        append("|defenders:${state.defenders.size}")
        state.defenders.sortedBy { it.id }.forEach { defender ->
            append(
                "|d${defender.id}:${defender.type},${defender.position.value.x},${defender.position.value.y},${defender.level.value},${defender.buildTimeRemaining.value}",
            )
        }
        append("|attackers:${state.attackers.size}")
        state.attackers.sortedBy { it.id }.forEach { attacker ->
            append(
                "|a${attacker.id}:${attacker.type},${attacker.position.value.x},${attacker.position.value.y},${attacker.currentHealth.value},${attacker.isDefeated.value}",
            )
        }
        append("|effects:${state.fieldEffects.size}")
        append("|traps:${state.traps.size}")
        if (state.level.isSandbox) {
            append("|sandboxTiles:${state.sandboxPaintedTiles.size}")
            state.sandboxPaintedTiles.entries
                .sortedWith(compareBy({ it.key.x }, { it.key.y }))
                .forEach { (position, tileType) ->
                    append("|st:${position.x},${position.y},$tileType")
                }
            append("|sandboxRivers:${state.sandboxPaintedRiverTiles.size}")
            state.sandboxPaintedRiverTiles.entries
                .sortedWith(compareBy({ it.key.x }, { it.key.y }))
                .forEach { (position, riverTile) ->
                    append("|sr:${position.x},${position.y},${riverTile.flowDirection},${riverTile.flowSpeed}")
                }
        }
    }
}

/**
 * Check if there are unsaved changes
 */
fun GameViewModel.hasUnsavedChanges(): Boolean {
    val currentState = _gameState.value ?: return false
    val currentSnapshot = createGameStateSnapshot(currentState)
    val referenceSnapshot = lastSaveSnapshot ?: initialGameStateSnapshot ?: return false
    return currentSnapshot != referenceSnapshot
}

fun GameViewModel.loadGame(saveId: String) {
    // Try to load from local storage first; if not present, import from remote cache
    val savedGame =
        de.egril.defender.save.SaveFileStorage
            .loadGameState(saveId)
            ?: importAndLoadFromRemoteCache(saveId)
            ?: return

    // Find the level by ID
    val level = _worldLevels.value.find { it.level.id == savedGame.levelId }?.level

    // Verify map ID matches if both are available
    if (level != null && savedGame.mapId != null && level.mapId != null) {
        if (savedGame.mapId != level.mapId) {
            // Map mismatch - the level at this numeric ID now uses a different map
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("WARNING: Save file has different map ID (saved: ${savedGame.mapId}, current: ${level.mapId})")
            }
            if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                println("Level sequence may have changed. Attempting to find level with matching map ID...")
            }

            // Try to find any level that uses the same map as the saved game
            val levelWithCorrectMap =
                _worldLevels.value
                    .map { it.level }
                    .find { it.mapId == savedGame.mapId }

            if (levelWithCorrectMap != null) {
                println("Found level with matching map ID: ${levelWithCorrectMap.name} (ID: ${levelWithCorrectMap.id})")
                val gameState =
                    de.egril.defender.save.SaveFileStorage
                        .convertSavedGameToGameState(savedGame, levelWithCorrectMap)
                _gameState.value = gameState
                gameEngine = GameEngine(gameState)
                _currentScreen.value = Screen.GamePlay(levelWithCorrectMap.id)
                // Set snapshots when loading a saved game
                initialGameStateSnapshot = createGameStateSnapshot(gameState)
                lastSaveSnapshot = initialGameStateSnapshot

                de.egril.defender.analytics.reportEvent(
                    de.egril.defender.analytics.GameEventType.LEVEL_LOADED,
                    levelWithCorrectMap.name,
                    gameState.turnNumber.value,
                    gameState.difficulty.name,
                )
                // Start time tracking for reminders
                startTimeTracking()
                return
            } else {
                if (LogConfig.ENABLE_SAVE_LOAD_LOGGING) {
                    println("ERROR: Could not find any level with map ID ${savedGame.mapId}. Save file may be incompatible.")
                }
                // TODO: Show error dialog to user
                return
            }
        }
    }

    // No map ID mismatch or one/both map IDs are null (backward compatibility)
    if (level != null) {
        val gameState =
            de.egril.defender.save.SaveFileStorage
                .convertSavedGameToGameState(savedGame, level)
        _gameState.value = gameState
        gameEngine = GameEngine(gameState)
        _currentScreen.value = Screen.GamePlay(savedGame.levelId)
        // Set snapshots when loading a saved game
        initialGameStateSnapshot = createGameStateSnapshot(gameState)
        lastSaveSnapshot = initialGameStateSnapshot

        de.egril.defender.analytics.reportEvent(
            de.egril.defender.analytics.GameEventType.LEVEL_LOADED,
            level.name,
            gameState.turnNumber.value,
            gameState.difficulty.name,
        )
        // Start time tracking for reminders
        startTimeTracking()
    }
}

/**
 * Import world map progress and check for conflicts
 * Returns true if conflict detected and shown to user
 */
fun GameViewModel.importWorldMapProgress(json: String): Boolean {
    val importedWorldMap =
        de.egril.defender.save.SaveFileStorage
            .importWorldMapProgress(json)

    if (importedWorldMap != null) {
        // Conflict detected - show dialog
        val currentWorldMap =
            de.egril.defender.save.SaveFileStorage
                .loadWorldMapStatus() ?: emptyMap()
        _worldMapConflict.value =
            WorldMapConflict(
                savedGame = null, // No associated save game
                savedWorldMap = importedWorldMap,
                currentWorldMap = currentWorldMap,
                level = null, // No level to load
            )
        return true
    }

    return false // No conflict, nothing to do
}

/**
 * Resolve world map conflict by choosing which version to keep
 */
fun GameViewModel.resolveWorldMapConflict(useSavedVersion: Boolean) {
    val conflict = _worldMapConflict.value ?: return

    if (useSavedVersion) {
        // Use the world map from the import/save
        val updatedWorldLevels =
            de.egril.defender.save.SaveFileStorage.applyWorldMapProgress(
                conflict.savedWorldMap,
                _worldLevels.value,
            )
        _worldLevels.value = updatedWorldLevels
    }
    // If not using saved version, keep current world map (do nothing)

    // Clear the conflict
    _worldMapConflict.value = null

    // If there's an associated saved game and level, load it
    if (conflict.savedGame != null && conflict.level != null) {
        val gameState =
            de.egril.defender.save.SaveFileStorage
                .convertSavedGameToGameState(conflict.savedGame, conflict.level)
        _gameState.value = gameState
        gameEngine = GameEngine(gameState)
        _currentScreen.value = Screen.GamePlay(conflict.level.id)
        // Set snapshots when loading a saved game
        initialGameStateSnapshot = createGameStateSnapshot(gameState)
        lastSaveSnapshot = initialGameStateSnapshot
    }
}

/**
 * Cancel world map conflict resolution
 */
fun GameViewModel.cancelWorldMapConflict() {
    _worldMapConflict.value = null
}

fun GameViewModel.deleteSavedGame(saveId: String) {
    de.egril.defender.save.SaveFileStorage
        .deleteSavedGame(saveId)
    refreshSavedGames()
}
